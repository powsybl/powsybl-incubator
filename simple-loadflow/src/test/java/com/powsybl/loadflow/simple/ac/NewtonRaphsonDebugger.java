/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.equations.EquationSystem;
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
public class NewtonRaphsonDebugger extends DefaultNewtonRaphsonObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewtonRaphsonDebugger.class);

    @Override
    public void beginIteration(int iteration) {
        LOGGER.info("BEGIN ITERATION {}", iteration);
    }

    @Override
    public void endIteration(int iteration) {
        LOGGER.info("END ITERATION {}", iteration);
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

    @Override
    public void afterStateUpdate(double[] x, EquationSystem equationSystem, int iteration) {
        log(x, equationSystem.getColumnNames(), LOGGER, "X" + iteration);
    }

    @Override
    public void afterJacobianBuild(Matrix j, EquationSystem equationSystem, int iteration) {
        try (PrintStream ps = LoggerFactory.getInfoPrintStream(LOGGER)) {
            ps.println("J(X" + iteration + ")=");
            j.print(ps, equationSystem.getRowNames(), equationSystem.getColumnNames());
        }
    }

    @Override
    public void afterEquationEvaluation(double[] fx, EquationSystem equationSystem, int iteration) {
        log(fx, equationSystem.getRowNames(), LOGGER, "F(X" + iteration + ")");
    }
}
