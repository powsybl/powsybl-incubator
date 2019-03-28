/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.math.matrix.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

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
    public void buildDcMatrix() {
        Network network = EurostagTutorialExample1Factory.create();

        logNetwork(network);

        network.getBusView().getBusStream()
                .forEach(b -> {
                    b.setAngle(0);
                    b.setV(b.getVoltageLevel().getNominalV());
                });

        IndexedNetwork indexedNetwork = IndexedNetwork.of(network);

        int slackBusNum = 0;

        Matrix lfMatrix = LoadFlowMatrix.buildDc(indexedNetwork, slackBusNum, matrixFactory, new double[indexedNetwork.getBusCount()]);
        DenseMatrix denseLfMatrix = lfMatrix.toDense();

        List<String> busNames = indexedNetwork.getBuses().stream().map(Bus::getId).collect(Collectors.toList());
        try (PrintStream ps = LoggerFactory.getInfoPrintStream(LOGGER)) {
            ps.println();
            denseLfMatrix.print(ps, busNames, busNames);
        }

        assertEquals(1d, denseLfMatrix.getValue(0, 0), 0d);
        assertEquals(0d, denseLfMatrix.getValue(0, 1), 0d);
        assertEquals(0d, denseLfMatrix.getValue(0, 2), 0d);
        assertEquals(0d, denseLfMatrix.getValue(0, 3), 0d);

        assertEquals(0d, denseLfMatrix.getValue(1, 0), 0d);
        assertEquals(-22439.668433814884d, denseLfMatrix.getValue(1, 1), 0d);
        assertEquals(8751.515151515152d, denseLfMatrix.getValue(1, 2), 0d);
        assertEquals(0d, denseLfMatrix.getValue(1, 3), 0d);

        assertEquals(0d, denseLfMatrix.getValue(2, 0), 0d);
        assertEquals(8751.515151515152d, denseLfMatrix.getValue(2, 1), 0d);
        assertEquals(-14314.85921296912d, denseLfMatrix.getValue(2, 2), 0d);
        assertEquals(5563.344061453969d, denseLfMatrix.getValue(2, 3), 0d);

        assertEquals(0d, denseLfMatrix.getValue(3, 0), 0d);
        assertEquals(0d, denseLfMatrix.getValue(3, 1), 0d);
        assertEquals(5563.344061453969d, denseLfMatrix.getValue(3, 2), 0d);
        assertEquals(-5563.344061453969d, denseLfMatrix.getValue(3, 3), 0d);

        double[] rhs = LoadFlowMatrix.buildDcRhs(indexedNetwork, slackBusNum);

        try (LUDecomposition lu = lfMatrix.decomposeLU()) {
            lu.solve(rhs);
        }

        LOGGER.info("Result: {}", rhs);

        assertEquals(0d, rhs[0], 1E-14d);
        assertEquals(-0.04383352433493455d, rhs[1], 1E-14d);
        assertEquals(-0.11239308112163815d, rhs[2], 1E-14d);
        assertEquals(-0.2202418845341654d, rhs[3], 1E-14d);

        LoadFlowMatrix.updateDcNetwork(indexedNetwork, rhs);

        logNetwork(network);

        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        indexedNetwork = IndexedNetwork.of(network);

        lfMatrix = LoadFlowMatrix.buildDc(indexedNetwork, slackBusNum, matrixFactory, new double[indexedNetwork.getBusCount()]);
        rhs = LoadFlowMatrix.buildDcRhs(indexedNetwork, slackBusNum);
        try (LUDecomposition lu = lfMatrix.decomposeLU()) {
            lu.solve(rhs);
        }
        LoadFlowMatrix.updateDcNetwork(indexedNetwork, rhs);

        logNetwork(network);
    }

}
