/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.math.matrix.Matrix;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DefaultAcLoadFlowObserver implements AcLoadFlowObserver {

    @Override
    public void beforeEquationSystemCreation() {
        // empty
    }

    @Override
    public void afterEquationSystemCreation() {
        // empty
    }

    @Override
    public void beginMacroIteration(int macroIteration) {
        // empty
    }

    @Override
    public void beginIteration(int iteration) {
        // empty
    }

    @Override
    public void norm(double norm) {
        // empty
    }

    @Override
    public void beforeEquationEvaluation(int iteration) {
        // empty
    }

    @Override
    public void afterEquationEvaluation(double[] fx, EquationSystem equationSystem, int iteration) {
        // empty
    }

    @Override
    public void beforeJacobianBuild(int iteration) {
        // empty
    }

    @Override
    public void afterJacobianBuild(Matrix j, EquationSystem equationSystem, int iteration) {
        // empty
    }

    @Override
    public void beforeLuDecomposition(int iteration) {
        // empty
    }

    @Override
    public void afterLuDecomposition(int iteration) {
        // empty
    }

    @Override
    public void beforeLuSolve(int iteration) {
        // empty
    }

    @Override
    public void afterLuSolve(int iteration) {
        // empty
    }

    @Override
    public void beforeStateUpdate(int iteration) {
        // empty
    }

    @Override
    public void afterStateUpdate(double[] x, EquationSystem equationSystem, int iteration) {
        // empty
    }

    @Override
    public void endIteration(int iteration) {
        // empty
    }

    @Override
    public void beforeMacroActionRun(int macroIteration, String macroActionName) {
        // empty
    }

    @Override
    public void afterMacroActionRun(int macroIteration, String macroActionName) {
        // empty
    }

    @Override
    public void endMacroIteration(int macroIteration) {
        // empty
    }
}
