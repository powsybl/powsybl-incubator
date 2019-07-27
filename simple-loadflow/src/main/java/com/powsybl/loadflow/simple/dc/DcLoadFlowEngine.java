/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc;

import com.powsybl.loadflow.simple.ac.nr.VoltageInitializer;
import com.powsybl.loadflow.simple.dc.equations.DcEquationSystem;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.math.matrix.LUDecomposition;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DcLoadFlowEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(DcLoadFlowEngine.class);

    private final LfNetwork network;

    private final VoltageInitializer voltageInitializer;

    private final MatrixFactory matrixFactory;

    public DcLoadFlowEngine(LfNetwork network, VoltageInitializer voltageInitializer, MatrixFactory matrixFactory) {
        this.network = Objects.requireNonNull(network);
        this.voltageInitializer = Objects.requireNonNull(voltageInitializer);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    public boolean run() {
        EquationSystem equationSystem = DcEquationSystem.create(network);

        double[] x = equationSystem.initStateVector(voltageInitializer);

        double[] targets = equationSystem.initTargetVector();

        equationSystem.updateEquationTerms(x);
        Matrix j = equationSystem.buildJacobian(matrixFactory).getMatrix();

        double[] dx = Arrays.copyOf(targets, targets.length);

        boolean status;
        try {
            try (LUDecomposition lu = j.decomposeLU()) {
                lu.solve(dx);
            }
            status = true;
        } catch (Exception e) {
            status = false;
            LOGGER.error("Failed to solve linear system for simple DC load flow.", e);
        }

        equationSystem.updateEquationTerms(dx);
        equationSystem.updateNetwork(dx);

        return status;
    }
}
