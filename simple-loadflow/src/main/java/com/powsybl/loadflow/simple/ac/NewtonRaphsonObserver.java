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
public interface NewtonRaphsonObserver {

    void beginIteration(int iteration);

    void endIteration(int iteration, double fxNorm);

    void j(Matrix j, EquationSystem equationSystem, int iteration);

    void x(double[] x, EquationSystem equationSystem, int iteration);

    void fx(double[] fx, EquationSystem equationSystem, int iteration);
}
