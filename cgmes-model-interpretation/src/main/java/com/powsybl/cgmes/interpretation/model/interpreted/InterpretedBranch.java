/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

import org.apache.commons.math3.complex.Complex;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretedBranch {

    public DetectedBranchModel detectedBranchModel() {
        return detectedBranchModel;
    }

    public void setDetectedBranchModel(DetectedBranchModel detectedBranchModel) {
        this.detectedBranchModel = detectedBranchModel;
    }

    public BranchAdmittanceMatrix admittanceMatrix() {
        Complex ysh1 = new Complex(end1.g, end1.b);
        Complex ysh2 = new Complex(end2.g, end2.b);
        return new BranchAdmittanceMatrix(
            end1.r + end2.r,
            end1.x + end2.x,
            end1.a, end1.angle,
            ysh1,
            end2.a, end2.angle,
            ysh2);
    }

    static class End {
        double r = 0.0;
        double x = 0.0;
        double g = 0.0;
        double b = 0.0;
        double a = 1.0;
        double angle = 0.0;
    }

    protected End end1 = new End();
    protected End end2 = new End();

    private DetectedBranchModel detectedBranchModel;
}
