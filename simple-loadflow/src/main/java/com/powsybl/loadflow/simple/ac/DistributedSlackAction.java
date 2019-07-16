/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.commons.PowsyblException;
import com.powsybl.loadflow.simple.ac.macro.MacroAction;
import com.powsybl.loadflow.simple.ac.macro.MacroActionContext;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.loadflow.simple.network.PerUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DistributedSlackAction implements MacroAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedSlackAction.class);

    private static final double SLACK_EPSILON = 1d / PerUnit.SB;

    static class ParticipatingBus {

        final LfBus bus;

        double factor;

        ParticipatingBus(LfBus bus, double factor) {
            this.bus = bus;
            this.factor = factor;
        }
    }

    @Override
    public String getName() {
        return "Distributed slack";
    }

    private static List<ParticipatingBus> getParticipatingBuses(LfNetwork network) {
        return network.getBuses()
                .stream()
                .map(bus -> new ParticipatingBus(bus, bus.getParticipationFactor()))
                .filter(participatingBus -> participatingBus.factor != 0)
                .collect(Collectors.toList());
    }

    private void normalizeParticipationFactors(List<ParticipatingBus> participatingBuses) {
        double factorSum = participatingBuses.stream()
                .mapToDouble(participatingBus -> participatingBus.factor)
                .sum();
        if (factorSum == 0) {
            throw new PowsyblException("No more generator participating to slack distribution");
        }
        for (ParticipatingBus participatingBus : participatingBuses) {
            participatingBus.factor /= factorSum;
        }
    }

    private static String formatMw(double d) {
        return String.format("%.1f", d * PerUnit.SB);
    }

    @Override
    public boolean run(MacroActionContext context) {
        double slackBusActivePowerMismatch = context.getNewtonRaphsonResult().getSlackBusActivePowerMismatch();
        if (Math.abs(slackBusActivePowerMismatch) > SLACK_EPSILON) {
            LfNetwork network = context.getNetwork();

            List<ParticipatingBus> participatingBuses = getParticipatingBuses(network);

            int iteration = 0;
            double remainingMismatch = slackBusActivePowerMismatch;
            while (!participatingBuses.isEmpty()
                    && Math.abs(remainingMismatch) > SLACK_EPSILON) {

                remainingMismatch -= run(participatingBuses, iteration, remainingMismatch);

                iteration++;
            }

            if (Math.abs(remainingMismatch) > SLACK_EPSILON) {
                throw new PowsyblException("Failed to distribute slack bus active power mismatch, "
                        + formatMw(remainingMismatch) + " MW remains");
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Slack bus active power ({} MW) distributed in {} iterations",
                            formatMw(slackBusActivePowerMismatch), iteration);
                }
            }

            return true;
        }

        LOGGER.debug("Already balanced");

        return false;
    }

    private double run(List<ParticipatingBus> participatingBuses, int iteration, double remainingMismatch) {
        // normalize participation factors at each iteration start as some
        // buses might have reach a limit and have been discarded
        normalizeParticipationFactors(participatingBuses);

        double done = 0d;
        int modifiedBuses = 0;
        int busesAtMax = 0;
        int busesAtMin = 0;
        Iterator<ParticipatingBus> it = participatingBuses.iterator();
        while (it.hasNext()) {
            ParticipatingBus participatingBus = it.next();
            LfBus bus = participatingBus.bus;
            double factor = participatingBus.factor;

            double minP = bus.getMinP();
            double maxP = bus.getMaxP();
            double generationTargetP = bus.getGenerationTargetP();

            // we don't want to change the generation sign
            if (generationTargetP < 0) {
                maxP = Math.min(maxP, 0);
            } else {
                minP = Math.max(minP, 0);
            }

            double newGenerationTargetP = generationTargetP + remainingMismatch * factor;
            if (remainingMismatch > 0 && newGenerationTargetP > maxP) {
                newGenerationTargetP = maxP;
                busesAtMax++;
                it.remove();
            } else if (remainingMismatch < 0 && newGenerationTargetP < minP) {
                newGenerationTargetP = minP;
                busesAtMin++;
                it.remove();
            }

            if (newGenerationTargetP != generationTargetP) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Rescale '{}' active power target: {} -> {}",
                            bus.getId(), formatMw(generationTargetP), formatMw(newGenerationTargetP));
                }

                bus.setGenerationTargetP(newGenerationTargetP);
                done += newGenerationTargetP - generationTargetP;
                modifiedBuses++;
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} MW / {} MW distributed at iteration {} to {} buses ({} at max power, {} at min power)",
                    formatMw(done), formatMw(remainingMismatch), iteration, modifiedBuses,
                    busesAtMax, busesAtMin);
        }

        return done;
    }
}
