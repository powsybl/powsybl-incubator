/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.ac.outerloop.OuterLoop;
import com.powsybl.loadflow.simple.ac.outerloop.OuterLoopContext;
import com.powsybl.loadflow.simple.ac.outerloop.OuterLoopStatus;
import com.powsybl.loadflow.simple.equations.Equation;
import com.powsybl.loadflow.simple.equations.EquationType;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.LfReactiveDiagram;
import com.powsybl.loadflow.simple.network.PerUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ReactiveLimitsOuterLoop implements OuterLoop {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveLimitsOuterLoop.class);

    @Override
    public String getName() {
        return "Reactive limits";
    }

    @Override
    public OuterLoopStatus check(OuterLoopContext context) {
        OuterLoopStatus status = OuterLoopStatus.STABLE;
        for (LfBus bus : context.getNetwork().getBuses()) {
            LfReactiveDiagram diagram = bus.getReactiveDiagram().orElse(null);
            if (diagram != null) {
                Equation vEq = context.getEquationSystem().getEquation(bus.getNum(), EquationType.BUS_V);
                Equation qEq = context.getEquationSystem().getEquation(bus.getNum(), EquationType.BUS_Q);
                if (bus.hasVoltageControl()) { // PV bus
                    double p = bus.getGenerationTargetP();
                    double q = bus.getQ() - bus.getLoadTargetQ();
                    double minQ = diagram.getMinQ(p);
                    double maxQ = diagram.getMaxQ(p);
                    if (q < minQ) {
                        // switch PV -> PQ
                        vEq.setActive(false);
                        qEq.setActive(true);
                        bus.setGenerationTargetQ(minQ);
                        bus.setVoltageControl(false);
                        LOGGER.debug("Switch bus {} PV -> PQ, q={} < minQ={}", bus.getId(), q * PerUnit.SB, minQ * PerUnit.SB);
                        status = OuterLoopStatus.UNSTABLE;
                    } else if (q > maxQ) {
                        // switch PV -> PQ
                        vEq.setActive(false);
                        qEq.setActive(true);
                        bus.setGenerationTargetQ(maxQ);
                        bus.setVoltageControl(false);
                        LOGGER.debug("Switch bus {} PV -> PQ, q={} MVar > maxQ={}", bus.getId(), q * PerUnit.SB, maxQ * PerUnit.SB);
                        status = OuterLoopStatus.UNSTABLE;
                    }
                } else { // PQ bus
                    // TODO
                }
            }
        }
        return status;
    }
}
