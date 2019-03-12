/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public abstract class AbstractDcFlowEquations implements DcFlowEquations {

    @Override
    public double p2() {
        return -p1();
    }

    @Override
    public double dp1dph2() {
        return -dp1dph1();
    }

    @Override
    public double dp2dph1() {
        return -dp1dph1();
    }

    @Override
    public double dp2dph2() {
        return dp1dph1();
    }
}