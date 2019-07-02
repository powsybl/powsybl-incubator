/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.ac.nr.NewtonRaphsonStatus;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class AcLoadFlowResult {

    private final int macroIterations;

    private final int newtowRaphsonIterations;

    private final NewtonRaphsonStatus newtonRaphsonStatus;

    public AcLoadFlowResult(int macroIterations, int newtowRaphsonIterations, NewtonRaphsonStatus newtonRaphsonStatus) {
        this.macroIterations = macroIterations;
        this.newtowRaphsonIterations = newtowRaphsonIterations;
        this.newtonRaphsonStatus = newtonRaphsonStatus;
    }

    public int getMacroIterations() {
        return macroIterations;
    }

    public int getNewtowRaphsonIterations() {
        return newtowRaphsonIterations;
    }

    public NewtonRaphsonStatus getNewtonRaphsonStatus() {
        return newtonRaphsonStatus;
    }
}
