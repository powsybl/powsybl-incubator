/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.google.common.base.Stopwatch;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.simple.ac.equations.AcEquationSystem;
import com.powsybl.loadflow.simple.ac.nr.NewtonRaphson;
import com.powsybl.loadflow.simple.ac.nr.NewtonRaphsonParameters;
import com.powsybl.loadflow.simple.ac.nr.NewtonRaphsonResult;
import com.powsybl.loadflow.simple.ac.nr.VoltageInitializer;
import com.powsybl.loadflow.simple.ac.observer.AcLoadFlowObserver;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.loadflow.simple.network.PerUnit;
import com.powsybl.loadflow.simple.network.SlackBusSelector;
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

    private final Network network;

    private final SlackBusSelector slackBusSelector;

    private final VoltageInitializer voltageInitializer;

    private final List<MacroAction> macroActions;

    private final MatrixFactory matrixFactory;

    private final AcLoadFlowObserver observer;

    public AcloadFlowEngine(Network network, SlackBusSelector slackBusSelector, VoltageInitializer voltageInitializer,
                            List<MacroAction> macroActions, MatrixFactory matrixFactory, AcLoadFlowObserver observer) {
        this.network = Objects.requireNonNull(network);
        this.slackBusSelector = Objects.requireNonNull(slackBusSelector);
        this.voltageInitializer = Objects.requireNonNull(voltageInitializer);
        this.macroActions = Objects.requireNonNull(macroActions);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.observer = Objects.requireNonNull(observer);
    }

    private NewtonRaphsonResult runNewtowRaphson(NetworkContext networkContext, EquationContext equationContext,
                                                 EquationSystem equationSystem, NewtonRaphsonParameters newtonRaphsonParameters,
                                                 int newtowRaphsonIteration, int macroIteration, String macroActionName) {
        observer.beginMacroIteration(macroIteration, macroActionName);

        NewtonRaphsonResult newtonRaphsonResult = new NewtonRaphson(networkContext, matrixFactory, observer, equationContext,
                                                                    equationSystem, voltageInitializer, newtowRaphsonIteration)
                .run(newtonRaphsonParameters);

        observer.endMacroIteration(macroIteration, macroActionName);

        return newtonRaphsonResult;
    }

    private boolean runMacroAction(MacroIterationContext macroIterationContext, MacroAction macroAction) {
        observer.beforeMacroActionRun(macroIterationContext.getMacroIteration(), macroAction.getName());

        boolean cont = macroAction.run(macroIterationContext);

        observer.afterMacroActionRun(macroIterationContext.getMacroIteration(), macroAction.getName(), cont);

        return cont;
    }

    public AcLoadFlowResult run() {
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<NetworkContext> networkContexts = NetworkContext.of(network, slackBusSelector);

        // only process main (largest) connected component
        NetworkContext networkContext = networkContexts.get(0);

        NewtonRaphsonParameters newtonRaphsonParameters = new NewtonRaphsonParameters();

        observer.beforeEquationSystemCreation();

        EquationContext equationContext = new EquationContext();
        EquationSystem equationSystem = AcEquationSystem.create(networkContext, equationContext);

        observer.afterEquationSystemCreation();

        // initial macro iteration
        int macroIteration = 0;

        NewtonRaphsonResult newtonRaphsonResult = runNewtowRaphson(networkContext, equationContext, equationSystem,
                                                                   newtonRaphsonParameters, 0,
                                                                   macroIteration++, INITIAL_MACRO_ACTION_NAME);

        // for each macro action run macro iterations until stabilized
        // macro actions are nested: inner most loop first in the list
        for (MacroAction macroAction : macroActions) {
            MacroIterationContext macroIterationContext = new MacroIterationContext(macroIteration, networkContext, newtonRaphsonResult);
            while (runMacroAction(macroIterationContext, macroAction)) {
                observer.beginMacroIteration(macroIteration, macroAction.getName());

                newtonRaphsonResult = runNewtowRaphson(networkContext, equationContext, equationSystem,
                                                       newtonRaphsonParameters, newtonRaphsonResult.getIteration(),
                                                       macroIteration, macroAction.getName());

                observer.endMacroIteration(macroIteration, macroAction.getName());

                macroIteration++;
            }
        }

        NetworkContext.resetState(network);
        networkContext.updateState();

        stopwatch.stop();

        LOGGER.info("Ac loadflow ran in {} ms (status={}, iteration={}, slackBusActivePowerMismatch={})", stopwatch.elapsed(TimeUnit.MILLISECONDS),
                newtonRaphsonResult.getStatus(), newtonRaphsonResult.getIteration(), newtonRaphsonResult.getSlackBusActivePowerMismatch() * PerUnit.SB);

        return new AcLoadFlowResult(macroIteration, newtonRaphsonResult.getIteration(), newtonRaphsonResult.getStatus());
    }
}
