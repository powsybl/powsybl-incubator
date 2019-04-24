/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.loadflow.simple.ac.NewtonRaphson;
import com.powsybl.loadflow.simple.ac.NewtonRaphsonParameters;
import com.powsybl.loadflow.simple.dc.DcEquationSystemMaker;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationType;
import com.powsybl.loadflow.simple.equations.VariableType;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.math.matrix.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;


/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LoadFlowMatrixTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowMatrixTest.class);

    private final MatrixFactory matrixFactory = new DenseMatrixFactory();

    private static void logNetwork(Network network) {
        network.getLoads().forEach(l ->  LOGGER.info("{} : p = {}.", l.getId(), l.getP0()));
        network.getGenerators().forEach(g ->  LOGGER.info("{} : p = {}.", g.getId(), g.getTargetP()));
        network.getBranchStream().forEach(b -> LOGGER.info("{} : p1 = {}, p2 = {}.",
                b.getId(), b.getTerminal1().getP(), b.getTerminal2().getP()));
    }

    @Test
    public void testAc() {
        //Network network = TwoBusNetworkFactory.create();
        Network network = EurostagTutorialExample1Factory.create();

        logNetwork(network);

        NetworkContext networkContext = NetworkContext.of(network);

        new NewtonRaphson(networkContext, matrixFactory, new NewtonRaphsonLogger())
                .run(new NewtonRaphsonParameters());

        NetworkXml.write(network, System.out);
    }

    @Test
    public void buildDcMatrix() {
        Network network = EurostagTutorialExample1Factory.create();

        logNetwork(network);

        NetworkContext networkContext = NetworkContext.of(network);

        EquationContext context = new EquationContext();
        for (Bus b : networkContext.getBuses()) {
            context.getEquation(b.getId(), EquationType.BUS_P);
            context.getVariable(b.getId(), VariableType.BUS_PHI);
        }

        EquationSystem equationSystem = new DcEquationSystemMaker()
                .make(networkContext, context);

        double[] x = equationSystem.initState();
        try (PrintStream ps = LoggerFactory.getInfoPrintStream(LOGGER)) {
            ps.println("X=");
            Matrix.createFromColumn(x, new DenseMatrixFactory())
                    .print(ps, equationSystem.getColumnNames(), null);
        }

        Matrix j = equationSystem.buildJacobian(matrixFactory, x);
        try (PrintStream ps = LoggerFactory.getInfoPrintStream(LOGGER)) {
            ps.println("J=");
            j.print(ps, equationSystem.getRowNames(), equationSystem.getColumnNames());
        }

        assertEquals(1d, j.toDense().getValue(0, 0), 0d);
        assertEquals(0d, j.toDense().getValue(0, 1), 0d);
        assertEquals(0d, j.toDense().getValue(0, 2), 0d);
        assertEquals(0d, j.toDense().getValue(0, 3), 0d);

        assertEquals(0d, j.toDense().getValue(1, 0), 0d);
        assertEquals(22439.668433814884d, j.toDense().getValue(1, 1), 0d);
        assertEquals(-8751.515151515152d, j.toDense().getValue(1, 2), 0d);
        assertEquals(0d, j.toDense().getValue(1, 3), 0d);

        assertEquals(0d, j.toDense().getValue(2, 0), 0d);
        assertEquals(-8751.515151515152d, j.toDense().getValue(2, 1), 0d);
        assertEquals(14314.85921296912d, j.toDense().getValue(2, 2), 0d);
        assertEquals(-5563.344061453969d, j.toDense().getValue(2, 3), 0d);

        assertEquals(0d, j.toDense().getValue(3, 0), 0d);
        assertEquals(0d, j.toDense().getValue(3, 1), 0d);
        assertEquals(-5563.344061453969d, j.toDense().getValue(3, 2), 0d);
        assertEquals(5563.344061453969d, j.toDense().getValue(3, 3), 0d);

        double[] targets = equationSystem.getTargets();
        try (PrintStream ps = LoggerFactory.getInfoPrintStream(LOGGER)) {
            ps.println("TGT=");
            Matrix.createFromColumn(targets, matrixFactory)
                    .print(ps, equationSystem.getRowNames(), null);
        }

        double[] dx = Arrays.copyOf(targets, targets.length);
        try (LUDecomposition lu = j.decomposeLU()) {
            lu.solve(dx);
        }

        assertEquals(0d, dx[0], 1E-14d);
        assertEquals(-0.04383352433493455d, dx[1], 1E-14d);
        assertEquals(-0.11239308112163815d, dx[2], 1E-14d);
        assertEquals(-0.2202418845341654d, dx[3], 1E-14d);

        networkContext.resetState();
        equationSystem.updateState(dx);

        logNetwork(network);

        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        networkContext = NetworkContext.of(network);

        equationSystem = new DcEquationSystemMaker()
                .make(networkContext, context);

        j = equationSystem.buildJacobian(matrixFactory, x);

        dx = Arrays.copyOf(targets, targets.length);
        try (LUDecomposition lu = j.decomposeLU()) {
            lu.solve(dx);
        }

        networkContext.resetState();
        equationSystem.updateState(dx);

        logNetwork(network);
    }

}
