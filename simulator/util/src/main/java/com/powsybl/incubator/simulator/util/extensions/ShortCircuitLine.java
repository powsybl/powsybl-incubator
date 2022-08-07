/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitLine {

    private final double coeffXo; // Xo = Xd * coeffXo : value of the homopolar admittance (in ohms) expressed at the leg2 side
    private final double coeffRo; // Ro = Rd * coeffRo : value of the homopolar resistance (in ohms) expressed at the leg2 side

    ShortCircuitLine(double coeffRo, double coeffXo) {
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
    }

    public double getCoeffRo() {
        return coeffRo;
    }

    public double getCoeffXo() {
        return coeffXo;
    }
}
