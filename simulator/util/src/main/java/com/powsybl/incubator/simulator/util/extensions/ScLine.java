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
public class ScLine {

    private final double xo; // Xo : value of the homopolar admittance (in pu, same base as X) expressed at the leg2 side
    private final double ro; // Ro : value of the homopolar resistance (in pu, same base as R) expressed at the leg2 side

    ScLine(double ro, double xo) {
        this.ro = ro;
        this.xo = xo;
    }

    public double getRo() {
        return ro;
    }

    public double getXo() {
        return xo;
    }
}
