/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.Matrix;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

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

    public static void log(double[] vector, List<String> names, Logger logger, String name) {
        Objects.requireNonNull(vector);
        Objects.requireNonNull(logger);
        try (PrintStream ps = LoggerFactory.getInfoPrintStream(logger)) {
            ps.print(name);
            ps.println("=");
            Matrix.createFromColumn(vector, new DenseMatrixFactory())
                    .print(ps, names, null);
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
