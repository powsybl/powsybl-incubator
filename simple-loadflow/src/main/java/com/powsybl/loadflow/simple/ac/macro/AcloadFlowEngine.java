/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.macro;

import com.google.common.base.Stopwatch;
import com.powsybl.loadflow.simple.ac.equations.AcEquationSystem;
import com.powsybl.loadflow.simple.ac.nr.*;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.equations.VoltageInitializer;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.loadflow.simple.network.PerUnit;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.powsybl.loadflow.simple.util.Markers.PERFORMANCE_MARKER;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class AcloadFlowEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcloadFlowEngine.class);

    private final LfNetwork network;

    private final VoltageInitializer voltageInitializer;

    private final NewtonRaphsonStoppingCriteria stoppingCriteria;

    private final List<MacroAction> macroActions;

    private final MatrixFactory matrixFactory;

    private final AcLoadFlowObserver observer;

    public AcloadFlowEngine(LfNetwork network, VoltageInitializer voltageInitializer, NewtonRaphsonStoppingCriteria stoppingCriteria,
                            List<MacroAction> macroActions, MatrixFactory matrixFactory, AcLoadFlowObserver observer) {
        this.network = Objects.requireNonNull(network);
        this.voltageInitializer = Objects.requireNonNull(voltageInitializer);
        this.stoppingCriteria = Objects.requireNonNull(stoppingCriteria);
        this.macroActions = Objects.requireNonNull(macroActions);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.observer = Objects.requireNonNull(observer);
    }

    public AcLoadFlowResult run() {
        Stopwatch stopwatch = Stopwatch.createStarted();

        observer.beforeEquationSystemCreation();

        EquationContext equationContext = new EquationContext();
        EquationSystem equationSystem = AcEquationSystem.create(network, equationContext);

        observer.afterEquationSystemCreation();

        NewtonRaphsonResult lastNrResult;
        int macroIteration = 0;
        try (NewtonRaphson newtonRaphson = new NewtonRaphson(network, matrixFactory, observer, equationContext, equationSystem, stoppingCriteria)) {

            NewtonRaphsonParameters nrParameters = new NewtonRaphsonParameters().setVoltageInitializer(voltageInitializer);

            // initial Newton-Raphson
            lastNrResult = newtonRaphson.run(nrParameters);

            // for each macro action run macro iterations until stabilized
            // macro actions are nested: inner most loop first in the list
            for (MacroAction macroAction : macroActions) {
                // re-run macro action + Newton-Raphson until stabilization
                boolean cont;
                do {
                    observer.beforeMacroActionRun(macroIteration, macroAction.getName());

                    cont = macroAction.run(new MacroActionContext(macroIteration, network, lastNrResult));

                    observer.afterMacroActionRun(macroIteration, macroAction.getName(), cont);

                    if (cont) {
                        observer.beginMacroIteration(macroIteration, macroAction.getName());

                        // restart Newton-Raphson
                        lastNrResult = newtonRaphson.run(nrParameters);

                        observer.endMacroIteration(macroIteration, macroAction.getName());

                        macroIteration++;
                    }
                } while (cont);
            }

            // update network state variable
            if (lastNrResult.getStatus() == NewtonRaphsonStatus.CONVERGED) {
                observer.beforeNetworkUpdate();

                equationSystem.updateNetwork(lastNrResult.getX());

                observer.afterNetworkUpdate();
            }
        }

        stopwatch.stop();

        int iterations = lastNrResult.getIteration();
        int macroIterations = macroIteration + 1;

        LOGGER.debug(PERFORMANCE_MARKER, "Ac loadflow ran in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        LOGGER.info("Ac loadflow complete (status={}, iterations={}, macroIterations={}, slackBusActivePowerMismatch={})",
                lastNrResult.getStatus(),
                iterations,
                macroIterations,
                lastNrResult.getSlackBusActivePowerMismatch() * PerUnit.SB);

        return new AcLoadFlowResult(macroIterations, iterations, lastNrResult.getStatus());
    }
}
