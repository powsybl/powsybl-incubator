/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.math.matrix.Matrix;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MultipleNewtonRaphsonObserver implements NewtonRaphsonObserver {

    private final List<NewtonRaphsonObserver> observers;

    public MultipleNewtonRaphsonObserver(List<NewtonRaphsonObserver> observers) {
        this.observers = Objects.requireNonNull(observers);
    }

    @Override
    public void beginIteration(int iteration, double fxNorm) {
        observers.forEach(o -> o.beginIteration(iteration, fxNorm));
    }

    @Override
    public void beforeEquationEvaluation(int iteration) {
        observers.forEach(o -> o.beforeEquationEvaluation(iteration));
    }

    @Override
    public void afterEquationEvaluation(double[] fx, EquationSystem equationSystem, int iteration) {
        observers.forEach(o -> o.afterEquationEvaluation(fx, equationSystem, iteration));
    }

    @Override
    public void beforeJacobianBuild(int iteration) {
        observers.forEach(o -> o.beforeJacobianBuild(iteration));
    }

    @Override
    public void afterJacobianBuild(Matrix j, EquationSystem equationSystem, int iteration) {
        observers.forEach(o -> o.afterJacobianBuild(j, equationSystem, iteration));
    }

    @Override
    public void beforeLuDecomposition(int iteration) {
        observers.forEach(o -> o.beforeLuDecomposition(iteration));
    }

    @Override
    public void afterLuDecomposition(int iteration) {
        observers.forEach(o -> o.afterLuDecomposition(iteration));
    }

    @Override
    public void beforeLuSolve(int iteration) {
        observers.forEach(o -> o.beforeLuSolve(iteration));
    }

    @Override
    public void afterLuSolve(int iteration) {
        observers.forEach(o -> o.afterLuSolve(iteration));
    }

    @Override
    public void afterStateUpdate(double[] x, EquationSystem equationSystem, int iteration) {
        observers.forEach(o -> o.afterStateUpdate(x, equationSystem, iteration));
    }

    @Override
    public void endIteration(int iteration) {
        observers.forEach(o -> o.endIteration(iteration));
    }
}
