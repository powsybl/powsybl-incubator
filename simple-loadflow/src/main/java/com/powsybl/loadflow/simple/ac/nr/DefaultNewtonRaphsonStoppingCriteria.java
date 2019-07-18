/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.nr;

import com.powsybl.loadflow.simple.ac.macro.AcLoadFlowObserver;
import com.powsybl.loadflow.simple.equations.Vectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DefaultNewtonRaphsonStoppingCriteria implements NewtonRaphsonStoppingCriteria {

    /**
     * Convergence epsilon per equation: 10^-4 in p.u => 10^-2 in Kv, Mw or MVar
     */
    public static final double CONV_EPS_PER_EQ = Math.pow(10, -4);

    @Override
    public boolean test(double[] fx, AcLoadFlowObserver observer) {
        // calculate norm L2 of equations mismatch vector
        double norm = Vectors.norm2(fx);
        observer.norm(norm);
        return norm < Math.sqrt(CONV_EPS_PER_EQ * CONV_EPS_PER_EQ * fx.length);
    }
}
