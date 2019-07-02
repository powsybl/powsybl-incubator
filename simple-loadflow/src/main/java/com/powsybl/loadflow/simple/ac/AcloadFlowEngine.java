/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.google.common.base.Stopwatch;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.simple.ac.equations.AcEquationSystem;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.loadflow.simple.network.PerUnit;
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

    private final Network network;

    private final LoadFlowParameters parameters;

    private final MatrixFactory matrixFactory;

    private final AcLoadFlowObserver observer;

    private final List<MacroIteration> macroIterations;

    public AcloadFlowEngine(Network network, LoadFlowParameters parameters, MatrixFactory matrixFactory, AcLoadFlowObserver observer,
                            List<MacroIteration> macroIterations) {
        this.network = Objects.requireNonNull(network);
        this.parameters = Objects.requireNonNull(parameters);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.observer = Objects.requireNonNull(observer);
        this.macroIterations = Objects.requireNonNull(macroIterations);
    }

    private boolean runMacroIterations(MacroIterationContext macroIterationContext) {
        boolean cont = false;
        for (MacroIteration macroIteration : macroIterations) {
            cont |= macroIteration.run(macroIterationContext);
        }
        return cont;
    }

    public AcLoadFlowResult run() {
        Stopwatch stopwatch = Stopwatch.createStarted();

        SimpleAcLoadFlowParameters parametersExt = parameters.getExtension(SimpleAcLoadFlowParameters.class);
        if (parametersExt == null) {
            parametersExt = new SimpleAcLoadFlowParameters();
        }

        List<NetworkContext> networkContexts = NetworkContext.of(network, parametersExt.getSlackBusSelectionMode());

        // only process main (largest) connected component
        NetworkContext networkContext = networkContexts.get(0);

        NewtonRaphsonParameters newtonRaphsonParameters = new NewtonRaphsonParameters()
                .setVoltageInitMode(parameters.getVoltageInitMode());

        observer.beforeEquationSystemCreation();

        EquationContext equationContext = new EquationContext();
        EquationSystem equationSystem = AcEquationSystem.create(networkContext, equationContext);

        observer.afterEquationSystemCreation();

        int macroIteration = 0;
        NewtonRaphsonResult newtonRaphsonResult = null;
        do {
            int newtowRaphsonIteration = newtonRaphsonResult != null ? newtonRaphsonResult.getIteration() : 0;
            newtonRaphsonResult = new NewtonRaphson(networkContext, matrixFactory, observer, equationContext,
                                                    equationSystem, newtonRaphsonParameters, newtowRaphsonIteration)
                    .run();
        } while (runMacroIterations(new MacroIterationContext(networkContext, newtonRaphsonResult, macroIteration)));

        NetworkContext.resetState(network);
        networkContext.updateState();

        stopwatch.stop();

        LOGGER.info("Ac loadflow ran in {} ms (status={}, iteration={}, slackBusActivePowerMismatch={})", stopwatch.elapsed(TimeUnit.MILLISECONDS),
                newtonRaphsonResult.getStatus(), newtonRaphsonResult.getIteration(), newtonRaphsonResult.getSlackBusActivePowerMismatch() * PerUnit.SB);

        return new AcLoadFlowResult(macroIteration, newtonRaphsonResult.getIteration(), newtonRaphsonResult.getStatus());
    }
}
