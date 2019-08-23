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

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NewtonRaphson implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewtonRaphson.class);

    private final LfNetwork network;

    private final MatrixFactory matrixFactory;

    private final AcLoadFlowObserver observer;

    private final EquationContext equationContext;

    private final EquationSystem equationSystem;

    private final NewtonRaphsonStoppingCriteria stoppingCriteria;

    private int iteration = 0;

    private Jacobian j;

    private double[] x;

    public NewtonRaphson(LfNetwork network, MatrixFactory matrixFactory, AcLoadFlowObserver observer,
                         EquationContext equationContext, EquationSystem equationSystem,
                         NewtonRaphsonStoppingCriteria stoppingCriteria) {
        this.network = Objects.requireNonNull(network);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.observer = Objects.requireNonNull(observer);
        this.equationContext = Objects.requireNonNull(equationContext);
        this.equationSystem = Objects.requireNonNull(equationSystem);
        this.stoppingCriteria = Objects.requireNonNull(stoppingCriteria);
    }

    private NewtonRaphsonStatus runIteration(double[] fx, double[] targets) {
        observer.beginIteration(iteration);

        try {
            // build jacobian
            observer.beforeJacobianBuild(iteration);

            if (j == null) {
                j = equationSystem.buildJacobian(matrixFactory);
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
            updateEquationTerms();

            // recalculate f(x) with new x
            observer.beforeEquationVectorUpdate(iteration);

            equationSystem.updateEquationVector(fx);

            observer.afterEquationVectorUpdate(equationSystem, iteration);

            Vectors.minus(fx, targets);

            if (stoppingCriteria.test(fx, observer)) {
                return NewtonRaphsonStatus.CONVERGED;
            }

            return null;
        } finally {
            observer.endIteration(iteration);
            iteration++;
        }
    }

    private double computeSlackBusActivePowerMismatch(EquationContext equationContext, EquationSystem equationSystem) {
        // find equation terms need to calculate slack bus active power injection
        LfBus slackBus = network.getSlackBus();
        Equation slackBusActivePowerEquation = equationContext.getEquation(slackBus.getNum(), EquationType.BUS_P);
        List<EquationTerm> slackBusActivePowerEquationTerms = equationSystem.getEquationTerms(slackBusActivePowerEquation);

        double p = 0;
        for (EquationTerm equationTerm : slackBusActivePowerEquationTerms) {
            p += equationTerm.eval();
        }

        // slack bus can also have real injection connected
        p -= slackBus.getTargetP();

        return p;
    }

    public NewtonRaphsonResult run(NewtonRaphsonParameters parameters) {
        Objects.requireNonNull(parameters);

        // initialize state vector
        if (x == null) {
            VoltageInitializer voltageInitializer = parameters.getVoltageInitializer();

            observer.beforeVoltageInitializerPreparation(voltageInitializer.getClass());

            voltageInitializer.prepare(network, matrixFactory);

            observer.afterVoltageInitializerPreparation();

            x = equationSystem.initStateVector(voltageInitializer);

            observer.stateVectorInitialized(x);

            updateEquationTerms();
        }

        // initialize target vector
        double[] targets = equationSystem.initTargetVector();

        // initialize mismatch vector (difference between equation values and targets)
        observer.beforeEquationVectorUpdate(iteration);

        double[] fx = equationSystem.initEquationVector();

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

        double slackBusActivePowerMismatch = computeSlackBusActivePowerMismatch(equationContext, equationSystem);

        return new NewtonRaphsonResult(status, iteration, x, slackBusActivePowerMismatch);
    }

    private void updateEquationTerms() {
        observer.beforeEquationTermsUpdate(iteration);

        equationSystem.updateEquationTerms(x);

        observer.afterEquationTermsUpdate(equationSystem, iteration);
    }

    @Override
    public void close() {
        j.cleanLU();
    }
}
