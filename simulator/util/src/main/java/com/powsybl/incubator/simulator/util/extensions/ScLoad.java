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
public class ScLoad {

    private double bdEquivalent;
    private double gdEquivalent;

    public ScLoad(double gdEquivalent, double bdEquivalent) {
        this.bdEquivalent = bdEquivalent;
        this.gdEquivalent = gdEquivalent;
    }

    public double getBdEquivalent() {
        return bdEquivalent;
    }

    public double getGdEquivalent() {
        return gdEquivalent;
    }
}
