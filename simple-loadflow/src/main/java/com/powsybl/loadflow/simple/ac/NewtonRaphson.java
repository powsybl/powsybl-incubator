/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.google.common.base.Stopwatch;
import com.powsybl.loadflow.simple.ac.equations.AcEquationSystem;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.equations.Vectors;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.math.matrix.LUDecomposition;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NewtonRaphson {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewtonRaphson.class);

    private static final double EPS_CONV = Math.pow(10, -4);

    private final NetworkContext networkContext;

    private final MatrixFactory matrixFactory;

    private final NewtonRaphsonObserver observer;

    static class NewtonRaphsonContext {
        Matrix j;
        LUDecomposition lu;
    }

    public NewtonRaphson(NetworkContext networkContext, MatrixFactory matrixFactory, NewtonRaphsonObserver observer) {
        this.networkContext = Objects.requireNonNull(networkContext);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.observer = Objects.requireNonNull(observer);
    }

    private NewtonRaphsonStatus runIteration(int iteration, EquationSystem system, double[] x, double[] fx,
                                             NewtonRaphsonContext context) {
        observer.beginIteration(iteration);

        // evaluate equations
        observer.beforeEquationEvaluation(iteration);

        system.updateEquationTerms(x);
        system.evalEquations(fx);

        observer.afterEquationEvaluation(fx, system, iteration);

        // calculate norm L2 of equations
        double norm = Vectors.norm2(fx);
        observer.norm(norm);
        if (norm < EPS_CONV) { // perfect match!
            observer.endIteration(iteration);
            return NewtonRaphsonStatus.CONVERGED;
        }

        // build jacobian
        observer.beforeJacobianBuild(iteration);

        if (context.j == null) {
            context.j = system.buildJacobian(matrixFactory);
        } else {
            system.updateJacobian(context.j);
        }

        observer.afterJacobianBuild(context.j, system, iteration);

        // solve f(x) = j * dx

        observer.beforeLuDecomposition(iteration);

        if (context.lu == null) {
            context.lu = context.j.decomposeLU();
        } else {
            context.lu.update();
        }

        observer.afterLuDecomposition(iteration);

        try {
            observer.beforeLuSolve(iteration);

            context.lu.solve(fx);

            observer.afterLuSolve(iteration);
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            return NewtonRaphsonStatus.SOLVER_FAILED;
        }

        observer.endIteration(iteration);

        observer.beforeStateUpdate(iteration);

        // update x
        Vectors.minus(x, fx);

        observer.afterStateUpdate(x, system, iteration);

        return null;
    }

    public NewtonRaphsonResult run(NewtonRaphsonParameters parameters) {
        Objects.requireNonNull(parameters);

        Stopwatch stopwatch = Stopwatch.createStarted();

        observer.beforeEquationSystemCreation();

        EquationSystem system = AcEquationSystem.create(networkContext);

        observer.afterEquationSystemCreation();

        // initialize state vector (flat start)
        double[] x = system.initState(parameters.getVoltageInitMode());

        double[] fx = new double[system.getEquations().size()];

        int iteration = 0;

        NewtonRaphsonContext context = new NewtonRaphsonContext();

        NewtonRaphsonStatus status = NewtonRaphsonStatus.NO_CALCULATION;
        while (iteration <= parameters.getMaxIteration()) {
            NewtonRaphsonStatus newStatus = runIteration(iteration, system, x, fx, context);
            if (newStatus != null) {
                status = newStatus;
                break;
            }
            iteration++;
        }

        if (context.lu != null) {
            context.lu.close();
        }

        networkContext.resetState();

        if (iteration < parameters.getMaxIteration()) {
            system.updateState(x);
        } else {
            status = NewtonRaphsonStatus.MAX_ITERATION_REACHED;
        }

        stopwatch.stop();
        LOGGER.debug("Newton Raphson done in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return new NewtonRaphsonResult(status, iteration);
    }
}
