/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.math.matrix.Matrix;

import java.util.Arrays;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface NewtonRaphsonObserver {

    static NewtonRaphsonObserver of(NewtonRaphsonObserver... observers) {
        return new MultipleNewtonRaphsonObserver(Arrays.asList(observers));
    }

    void beginIteration(int iteration, double fxNorm);

    void beforeEquationEvaluation(int iteration);

    void afterEquationEvaluation(double[] fx, EquationSystem equationSystem, int iteration);

    void beforeJacobianBuild(int iteration);

    void afterJacobianBuild(Matrix j, EquationSystem equationSystem, int iteration);

    void beforeLuDecomposition(int iteration);

    void afterLuDecomposition(int iteration);

    void beforeLuSolve(int iteration);

    void afterLuSolve(int iteration);

    void afterStateUpdate(double[] x, EquationSystem equationSystem, int iteration);

    void endIteration(int iteration);
}
