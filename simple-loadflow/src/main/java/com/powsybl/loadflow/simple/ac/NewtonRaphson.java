/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.ac.equations.AcEquationSystem;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.equations.Vectors;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.math.matrix.LUDecomposition;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NewtonRaphson {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewtonRaphson.class);

    private static final double EPS_CONV = Math.pow(10, -4);

    private final NetworkContext networkContext;

    private final MatrixFactory matrixFactory;

    private final NewtonRaphsonObserver observer;

    public NewtonRaphson(NetworkContext networkContext, MatrixFactory matrixFactory, NewtonRaphsonObserver observer) {
        this.networkContext = Objects.requireNonNull(networkContext);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.observer = Objects.requireNonNull(observer);
    }

    public NewtonRaphsonResult run(NewtonRaphsonParameters parameters) {
        Objects.requireNonNull(parameters);

        EquationContext context = new EquationContext();

        EquationSystem system = AcEquationSystem.create(networkContext, context);

        // initialize state vector (flat start)
        double[] x = system.initState(parameters.getVoltageInitMode());
        observer.afterStateUpdate(x, system, 0);

        double[] fx = new double[system.getEquations().size()];

        int iteration = 0;

        NewtonRaphsonStatus status = NewtonRaphsonStatus.CONVERGED;
        while (iteration < parameters.getMaxIteration()) {

            // evaluate equations
            observer.beforeEquationEvaluation(iteration);

            system.evalEquations(x, fx);

            observer.afterEquationEvaluation(fx, system, iteration);

            // calculate norm L2 of equations
            double norm = Vectors.norm2(fx);
            observer.beginIteration(iteration, norm);
            if (norm < EPS_CONV) { // perfect match!
                break;
            }

            // build jacobian
            observer.beforeJacobianBuild(iteration);

            Matrix j = system.buildJacobian(matrixFactory, x);

            observer.afterJacobianBuild(j, system, iteration);

            // solve f(x) = j * dx

            observer.beforeLuDecomposition(iteration);

            try (LUDecomposition lu = j.decomposeLU()) {
                observer.afterLuDecomposition(iteration);

                observer.beforeLuSolve(iteration);

                lu.solve(fx);

                observer.afterLuSolve(iteration);
            } catch (Exception e) {
                LOGGER.error(e.toString(), e);
                status = NewtonRaphsonStatus.SOLVER_FAILED;
            }

            observer.endIteration(iteration);

            // update x
            Vectors.minus(x, fx);

            iteration++;

            observer.afterStateUpdate(x, system, iteration);
        }

        networkContext.resetState();

        if (iteration < parameters.getMaxIteration()) {
            system.updateState(x);
        } else {
            status = NewtonRaphsonStatus.MAX_ITERATION_REACHED;
        }

        return new NewtonRaphsonResult(status, iteration);
    }
}
