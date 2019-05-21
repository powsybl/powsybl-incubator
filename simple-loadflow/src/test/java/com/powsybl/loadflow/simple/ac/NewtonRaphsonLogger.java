/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.equations.Vectors;
import com.powsybl.math.matrix.Matrix;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import java.io.PrintStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NewtonRaphsonLogger extends DefaultNewtonRaphsonObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewtonRaphsonLogger.class);

    @Override
    public void beginIteration(int iteration) {
        LOGGER.info("BEGIN ITERATION {}", iteration);
    }

    @Override
    public void endIteration(int iteration) {
        LOGGER.info("END ITERATION {}", iteration);
    }

    @Override
    public void afterStateUpdate(double[] x, EquationSystem equationSystem, int iteration) {
        Vectors.log(x, equationSystem.getColumnNames(), LOGGER, "X" + iteration);
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
        Vectors.log(fx, equationSystem.getRowNames(), LOGGER, "F(X" + iteration + ")");
    }
}
