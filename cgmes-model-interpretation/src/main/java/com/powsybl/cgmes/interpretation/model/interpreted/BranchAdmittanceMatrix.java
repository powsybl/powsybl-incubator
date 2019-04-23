/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexUtils;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class BranchAdmittanceMatrix {

    public BranchAdmittanceMatrix(Complex y11, Complex y12, Complex y21, Complex y22) {
        this.y11 = y11;
        this.y12 = y12;
        this.y21 = y21;
        this.y22 = y22;
    }

    public BranchAdmittanceMatrix(
        double r, double x,
        double a1, double angleDegrees1,
        Complex ysh1,
        double a2, double angleDegrees2,
        Complex ysh2) {
        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);

        Complex aA1 = ComplexUtils.polar2Complex(a1, angle1);
        Complex aA2 = ComplexUtils.polar2Complex(a2, angle2);

        Complex z = new Complex(r, x);
        y11 = z.reciprocal().add(ysh1).divide(aA1.conjugate().multiply(aA1));
        y12 = z.reciprocal().negate().divide(aA1.conjugate().multiply(aA2));
        y21 = z.reciprocal().negate().divide(aA2.conjugate().multiply(aA1));
        y22 = z.reciprocal().add(ysh2).divide(aA2.conjugate().multiply(aA2));
    }

    public Complex y11() {
        return y11;
    }

    public Complex y12() {
        return y12;
    }

    public Complex y21() {
        return y21;
    }

    public Complex y22() {
        return y22;
    }

    private final Complex y11;
    private final Complex y12;
    private final Complex y21;
    private final Complex y22;
}
