/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.commons.PowsyblException;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.NetworkContext;
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

    private static List<ParticipatingBus> getParticipatingBuses(NetworkContext networkContext) {
        return networkContext.getBuses()
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

    @Override
    public boolean run(MacroActionContext context) {
        double slackBusActivePowerMismatch = context.getNewtonRaphsonResult().getSlackBusActivePowerMismatch();
        if (Math.abs(slackBusActivePowerMismatch) > SLACK_EPSILON) {
            NetworkContext networkContext = context.getNetworkContext();

            List<ParticipatingBus> participatingBuses = getParticipatingBuses(networkContext);

            int iteration = 0;
            double remainingMismatch = slackBusActivePowerMismatch;
            while (!participatingBuses.isEmpty()
                    && Math.abs(remainingMismatch) > SLACK_EPSILON) {

                // normalize participation factors at each iteration start as some
                // buses might have reach a limit and have been discarded
                normalizeParticipationFactors(participatingBuses);

                double done = 0d;
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
                        it.remove();
                    } else if (remainingMismatch < 0 && newGenerationTargetP < minP) {
                        newGenerationTargetP = minP;
                        it.remove();
                    }

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Rescale '{}' active power target: {} -> {}",
                                bus.getId(), generationTargetP, newGenerationTargetP);
                    }
                    bus.setGenerationTargetP(newGenerationTargetP);
                    done += newGenerationTargetP - generationTargetP;
                }

                remainingMismatch -= done;

                iteration++;
            }

            if (Math.abs(remainingMismatch) > SLACK_EPSILON) {
                throw new PowsyblException("Failed to distribute slack bus active power mismatch, "
                        + remainingMismatch + " MW remains");
            } else {
                LOGGER.debug("Slack bus active power mismatch ({} MW) distributed in {} iterations",
                        slackBusActivePowerMismatch, iteration);
            }

            return true;
        }

        LOGGER.info("Already balanced");

        return false;
    }
}
