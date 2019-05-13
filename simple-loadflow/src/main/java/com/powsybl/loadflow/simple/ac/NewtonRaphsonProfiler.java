/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.google.common.base.Stopwatch;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.math.matrix.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NewtonRaphsonProfiler extends DefaultNewtonRaphsonObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewtonRaphsonProfiler.class);

    private final Stopwatch iterationStopwatch = Stopwatch.createUnstarted();

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();

    @Override
    public void beginIteration(int iteration, double fxNorm) {
        restart(iterationStopwatch);
    }

    @Override
    public void beforeEquationEvaluation(int iteration) {
        restart(stopwatch);
    }

    private static void restart(Stopwatch stopwatch) {
        stopwatch.reset();
        stopwatch.start();
    }

    @Override
    public void afterEquationEvaluation(double[] fx, EquationSystem equationSystem, int iteration) {
        stopwatch.stop();
        LOGGER.debug("Equations evaluated at iteration {} in {} ms", iteration, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public void beforeJacobianBuild(int iteration) {
        restart(stopwatch);
    }

    @Override
    public void afterJacobianBuild(Matrix j, EquationSystem equationSystem, int iteration) {
        stopwatch.stop();
        LOGGER.debug("Jacobian built at iteration {} in {} ms", iteration, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public void beforeLuDecomposition(int iteration) {
        restart(stopwatch);
    }

    @Override
    public void afterLuDecomposition(int iteration) {
        stopwatch.stop();
        LOGGER.debug("LU decomposed at iteration {} in {} ms", iteration, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public void beforeLuSolve(int iteration) {
        restart(stopwatch);
    }

    @Override
    public void afterLuSolve(int iteration) {
        stopwatch.stop();
        LOGGER.debug("LU solved at iteration {} in {} ms", iteration, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public void endIteration(int iteration) {
        iterationStopwatch.stop();
        LOGGER.debug("Iteration {} complete in {} ms", iteration, iterationStopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}