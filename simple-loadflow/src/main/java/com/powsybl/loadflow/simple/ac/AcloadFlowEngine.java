/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.google.common.base.Stopwatch;
import com.powsybl.loadflow.simple.ac.equations.AcEquationSystem;
import com.powsybl.loadflow.simple.ac.nr.*;
import com.powsybl.loadflow.simple.ac.observer.AcLoadFlowObserver;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.network.PerUnit;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class AcloadFlowEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcloadFlowEngine.class);

    private static final String INITIAL_MACRO_ACTION_NAME = "Init";

    private final LfNetwork network;

    private final VoltageInitializer voltageInitializer;

    private final List<MacroAction> macroActions;

    private final MatrixFactory matrixFactory;

    private final AcLoadFlowObserver observer;

    public AcloadFlowEngine(LfNetwork network, VoltageInitializer voltageInitializer, List<MacroAction> macroActions,
                            MatrixFactory matrixFactory, AcLoadFlowObserver observer) {
        this.network = Objects.requireNonNull(network);
        this.voltageInitializer = Objects.requireNonNull(voltageInitializer);
        this.macroActions = Objects.requireNonNull(macroActions);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.observer = Objects.requireNonNull(observer);
    }

    private NewtonRaphsonResult runNewtowRaphson(LfNetwork network, EquationContext equationContext,
                                                 EquationSystem equationSystem, NewtonRaphsonParameters newtonRaphsonParameters,
                                                 int newtowRaphsonIteration, int macroIteration, String macroActionName) {
        observer.beginMacroIteration(macroIteration, macroActionName);

        // for next macro iteration, restart from previous voltage
        VoltageInitializer macroIterationVoltageInitializer = macroIteration == 0 ? this.voltageInitializer
                                                                                  : new PreviousValueVoltageInitializer();

        NewtonRaphsonResult newtonRaphsonResult = new NewtonRaphson(network, matrixFactory, observer, equationContext,
                                                                    equationSystem, macroIterationVoltageInitializer,
                                                                    newtowRaphsonIteration)
                .run(newtonRaphsonParameters);

        observer.endMacroIteration(macroIteration, macroActionName);

        return newtonRaphsonResult;
    }

    public AcLoadFlowResult run() {
        Stopwatch stopwatch = Stopwatch.createStarted();

        NewtonRaphsonParameters newtonRaphsonParameters = new NewtonRaphsonParameters();

        observer.beforeEquationSystemCreation();

        EquationContext equationContext = new EquationContext();
        EquationSystem equationSystem = AcEquationSystem.create(network, equationContext);

        observer.afterEquationSystemCreation();

        // initial macro iteration
        int macroIteration = 0;

        NewtonRaphsonResult lastNewtonRaphsonResult = runNewtowRaphson(network, equationContext, equationSystem,
                                                                       newtonRaphsonParameters, 0,
                                                                       macroIteration++, INITIAL_MACRO_ACTION_NAME);

        // for each macro action run macro iterations until stabilized
        // macro actions are nested: inner most loop first in the list
        for (MacroAction macroAction : macroActions) {
            // re-run macro action + newtow raphson until stabilization
            boolean cont;
            do {
                observer.beforeMacroActionRun(macroIteration, macroAction.getName());

                cont = macroAction.run(new MacroActionContext(macroIteration, network, lastNewtonRaphsonResult));

                observer.afterMacroActionRun(macroIteration, macroAction.getName(), cont);

                if (cont) {
                    lastNewtonRaphsonResult = runNewtowRaphson(network, equationContext, equationSystem,
                                                           newtonRaphsonParameters, lastNewtonRaphsonResult.getIteration() + 1,
                                                           macroIteration, macroAction.getName());
                    macroIteration++;
                }
            } while (cont);
        }

        stopwatch.stop();

        LOGGER.info("Ac loadflow ran in {} ms (status={}, iteration={}, macroIteration={}, slackBusActivePowerMismatch={})",
                stopwatch.elapsed(TimeUnit.MILLISECONDS),
                lastNewtonRaphsonResult.getStatus(),
                lastNewtonRaphsonResult.getIteration(),
                macroIteration,
                lastNewtonRaphsonResult.getSlackBusActivePowerMismatch() * PerUnit.SB);

        return new AcLoadFlowResult(macroIteration, lastNewtonRaphsonResult.getIteration(), lastNewtonRaphsonResult.getStatus());
    }
}
