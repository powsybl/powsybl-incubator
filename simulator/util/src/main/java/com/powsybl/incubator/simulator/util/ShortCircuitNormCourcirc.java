/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNormCourcirc extends ShortCircuitNorm {

    public ShortCircuitNormCourcirc() {
        super();
    }

    @Override
    public double getCmaxVoltageFactor(double nominalVoltage) {
        double cmax = 1.0;
        return cmax;
    }

    @Override
    public double getCminVoltageFactor(double nominalVoltage) {
        double cmin = 1.0;
        return cmin;
    }

    @Override
    public NormType getNormType() {
        return NormType.RTE_COURCIRC;
    }
}
