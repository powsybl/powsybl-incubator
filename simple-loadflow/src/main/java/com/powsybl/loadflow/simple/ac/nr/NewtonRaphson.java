/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.nr;

import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.math.matrix.LUDecomposition;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NewtonRaphson implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewtonRaphson.class);

    private final LfNetwork network;

    private final MatrixFactory matrixFactory;

    private final AcLoadFlowObserver observer;

    private final EquationSystem equationSystem;

    private final NewtonRaphsonStoppingCriteria stoppingCriteria;

    private int iteration = 0;

    private JacobianMatrix j;

    private double[] x;

    public NewtonRaphson(LfNetwork network, MatrixFactory matrixFactory, AcLoadFlowObserver observer,
                         EquationSystem equationSystem, NewtonRaphsonStoppingCriteria stoppingCriteria) {
        this.network = Objects.requireNonNull(network);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.observer = Objects.requireNonNull(observer);
        this.equationSystem = Objects.requireNonNull(equationSystem);
        this.stoppingCriteria = Objects.requireNonNull(stoppingCriteria);
    }

    private NewtonRaphsonStatus runIteration(double[] fx, double[] targets) {
        observer.beginIteration(iteration);

        try {
            // build jacobian
            observer.beforeJacobianBuild(iteration);

            if (j == null) {
                j = JacobianMatrix.create(equationSystem, matrixFactory);
            } else {
                j.update();
            }

            observer.afterJacobianBuild(j.getMatrix(), equationSystem, iteration);

            // solve f(x) = j * dx

            observer.beforeLuDecomposition(iteration);

            LUDecomposition lu = j.decomposeLU();

            observer.afterLuDecomposition(iteration);

            try {
                observer.beforeLuSolve(iteration);

                lu.solve(fx);

                observer.afterLuSolve(iteration);
            } catch (Exception e) {
                LOGGER.error(e.toString(), e);
                return NewtonRaphsonStatus.SOLVER_FAILED;
            }

            // update x
            Vectors.minus(x, fx);

            // evaluate equation terms with new x
            updateEquations();

            // recalculate f(x) with new x
            observer.beforeEquationVectorUpdate(iteration);

            equationSystem.updateEquationVector(fx);

            observer.afterEquationVectorUpdate(equationSystem, iteration);

            Vectors.minus(fx, targets);

            // test stopping criteria and log norm(fx)
            NewtonRaphsonStoppingCriteria.TestResult testResult = stoppingCriteria.test(fx);
            observer.norm(testResult.getNorm());

            if (testResult.isStop()) {
                return NewtonRaphsonStatus.CONVERGED;
            }

            return null;
        } finally {
            observer.endIteration(iteration);
            iteration++;
        }
    }

    private double computeSlackBusActivePowerMismatch(EquationSystem equationSystem) {
        // search equation corresponding to slack bus active power injection
        LfBus slackBus = network.getSlackBus();
        Equation slackBusActivePowerEquation = equationSystem.getEquation(slackBus.getNum(), EquationType.BUS_P);

        return slackBusActivePowerEquation.eval()
                - slackBus.getTargetP(); // slack bus can also have real injection connected
    }

    public NewtonRaphsonResult run(NewtonRaphsonParameters parameters) {
        Objects.requireNonNull(parameters);

        // initialize state vector
        if (x == null) {
            VoltageInitializer voltageInitializer = parameters.getVoltageInitializer();

            observer.beforeVoltageInitializerPreparation(voltageInitializer.getClass());

            voltageInitializer.prepare(network, matrixFactory);

            observer.afterVoltageInitializerPreparation();

            x = equationSystem.createStateVector(voltageInitializer);

            observer.stateVectorInitialized(x);

            updateEquations();
        }

        // initialize target vector
        double[] targets = equationSystem.createTargetVector();

        // initialize mismatch vector (difference between equation values and targets)
        observer.beforeEquationVectorUpdate(iteration);

        double[] fx = equationSystem.createEquationVector();

        observer.afterEquationVectorUpdate(equationSystem, iteration);

        Vectors.minus(fx, targets);

        // start iterations
        NewtonRaphsonStatus status = NewtonRaphsonStatus.NO_CALCULATION;
        while (iteration <= parameters.getMaxIteration()) {
            NewtonRaphsonStatus newStatus = runIteration(fx, targets);
            if (newStatus != null) {
                status = newStatus;
                break;
            }
        }

        if (iteration >= parameters.getMaxIteration()) {
            status = NewtonRaphsonStatus.MAX_ITERATION_REACHED;
        }

        double slackBusActivePowerMismatch = computeSlackBusActivePowerMismatch(equationSystem);

        return new NewtonRaphsonResult(status, iteration, x, slackBusActivePowerMismatch);
    }

    private void updateEquations() {
        observer.beforeEquationsUpdate(iteration);

        equationSystem.updateEquations(x);

        observer.afterEquationsUpdate(equationSystem, iteration);
    }

    @Override
    public void close() {
        j.cleanLU();
    }
}
