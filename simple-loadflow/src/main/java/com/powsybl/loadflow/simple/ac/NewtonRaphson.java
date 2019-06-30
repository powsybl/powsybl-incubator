/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.google.common.base.Stopwatch;
import com.powsybl.loadflow.simple.ac.equations.AcEquationSystem;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.math.matrix.LUDecomposition;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

        double[] x;

        double[] targets;

        double[] fx;

        Matrix j;

        LUDecomposition lu;
    }

    public NewtonRaphson(NetworkContext networkContext, MatrixFactory matrixFactory, NewtonRaphsonObserver observer) {
        this.networkContext = Objects.requireNonNull(networkContext);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
        this.observer = Objects.requireNonNull(observer);
    }

    private NewtonRaphsonStatus runIteration(int iteration, EquationSystem system, NewtonRaphsonContext context) {
        observer.beginIteration(iteration);

        // evaluate equations
        observer.beforeEquationEvaluation(iteration);

        system.updateEquationTerms(context.x);
        system.evalEquations(context.fx);
        Vectors.minus(context.fx, context.targets);

        observer.afterEquationEvaluation(context.fx, system, iteration);

        // calculate norm L2 of equations
        double norm = Vectors.norm2(context.fx);
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

            context.lu.solve(context.fx);

            observer.afterLuSolve(iteration);
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            return NewtonRaphsonStatus.SOLVER_FAILED;
        }

        observer.endIteration(iteration);

        observer.beforeStateUpdate(iteration);

        // update x
        Vectors.minus(context.x, context.fx);

        observer.afterStateUpdate(context.x, system, iteration);

        return null;
    }

    private double computeSlackBusActivePowerMismatch(EquationContext equationContext, EquationSystem equationSystem) {
        // find equation terms need to calculate slack bus active power injection
        LfBus slackBus = networkContext.getSlackBus();
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

        Stopwatch stopwatch = Stopwatch.createStarted();

        observer.beforeEquationSystemCreation();

        EquationContext equationContext = new EquationContext();
        EquationSystem equationSystem = AcEquationSystem.create(networkContext, equationContext);

        observer.afterEquationSystemCreation();

        NewtonRaphsonContext context = new NewtonRaphsonContext();

        // initialize state vector (flat start)
        context.x = equationSystem.initState(parameters.getVoltageInitMode());

        // initialize target vector
        context.targets = equationSystem.initTargets();

        // initialize mismatch vector (difference between equation values and targets)
        context.fx = new double[equationSystem.getEquationsToSolve().size()];

        int iteration = 0;

        NewtonRaphsonStatus status = NewtonRaphsonStatus.NO_CALCULATION;
        while (iteration <= parameters.getMaxIteration()) {
            NewtonRaphsonStatus newStatus = runIteration(iteration, equationSystem, context);
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
            equationSystem.updateState(context.x);
        } else {
            status = NewtonRaphsonStatus.MAX_ITERATION_REACHED;
        }

        double slackBusActivePowerMismatch = computeSlackBusActivePowerMismatch(equationContext, equationSystem);

        stopwatch.stop();
        LOGGER.debug("Newton Raphson done in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return new NewtonRaphsonResult(status, iteration, slackBusActivePowerMismatch);
    }
}
