/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.equations.EquationContext;
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
        EquationContext context = new EquationContext();

        EquationSystem system = new AcEquationSystemMaker()
                .make(networkContext, context);

        // initialize state vector (flat start)
        double[] x = system.initState();
        observer.x(x, system, 0);

        double[] fx = new double[system.getEquations().size()];
        system.evalFx(x, fx);

        int iteration = 0;
        double norm;
        NewtonRaphsonStatus status = NewtonRaphsonStatus.CONVERGED;
        while (iteration < parameters.getMaxIteration() && (norm = Vectors.norm2(fx)) > EPS_CONV) {
            observer.beginIteration(iteration);

            // build jacobian
            Matrix j = system.buildJacobian(matrixFactory, x);
            observer.j(j, system, iteration);

            // evaluate f(x)
            system.evalFx(x, fx);
            observer.fx(fx, system, iteration);

            // solve f(x) = j * dx
            try (LUDecomposition lu = j.decomposeLU()) {
                lu.solve(fx);
            } catch (Exception e) {
                LOGGER.error(e.toString(), e);
                status = NewtonRaphsonStatus.SOLVER_FAILED;
            }

            observer.endIteration(iteration, norm);

            // update x
            Vectors.minus(x, fx);
            observer.x(x, system, iteration + 1);

            iteration++;
        }

        networkContext.resetState();

        if (iteration < parameters.getMaxIteration()) {
            system.updateState(x);
        } else {
            status = NewtonRaphsonStatus.MAX_ITERATION_REACHED;
        }

        return new NewtonRaphsonResult(status, iteration - 1);
    }
}
