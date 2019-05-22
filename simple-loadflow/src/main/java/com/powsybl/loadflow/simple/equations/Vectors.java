/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class Vectors {

    private Vectors() {
    }

    public static void minus(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("a and b have different length");
        }
        for (int i = 0; i < a.length; i++) {
            a[i] -= b[i];
        }
    }

    public static double norm2(double[] vector) {
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        return Math.sqrt(norm);
    }
}
