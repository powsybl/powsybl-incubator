/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple;

import com.google.common.collect.ImmutableList;
import com.powsybl.commons.io.table.AsciiTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.simple.equations.IndexedNetwork;
import com.powsybl.loadflow.simple.equations.LoadFlowMatrix;
import com.powsybl.math.matrix.*;
import com.powsybl.security.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SimpleLoadFlowTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLoadFlowTest.class);

    private final MatrixFactory matrixFactory = new DenseMatrixFactory();

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

        Matrix lfMatrix = LoadFlowMatrix.buildDc(indexedNetwork, slackBusNum, matrixFactory);
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

        assertEquals(-13688.153282299732d, denseLfMatrix.getValue(1, 0), 0d);
        assertEquals(22439.668433814884d, denseLfMatrix.getValue(1, 1), 0d);
        assertEquals(-8751.515151515152d, denseLfMatrix.getValue(1, 2), 0d);
        assertEquals(0d, denseLfMatrix.getValue(1, 3), 0d);

        assertEquals(0d, denseLfMatrix.getValue(2, 0), 0d);
        assertEquals(-8751.515151515152d, denseLfMatrix.getValue(2, 1), 0d);
        assertEquals(14314.85921296912d, denseLfMatrix.getValue(2, 2), 0d);
        assertEquals(-5563.344061453969d, denseLfMatrix.getValue(2, 3), 0d);

        assertEquals(0d, denseLfMatrix.getValue(3, 0), 0d);
        assertEquals(0d, denseLfMatrix.getValue(3, 1), 0d);
        assertEquals(-5563.344061453969d, denseLfMatrix.getValue(3, 2), 0d);
        assertEquals(5563.344061453969d, denseLfMatrix.getValue(3, 3), 0d);

        double[] rhs = LoadFlowMatrix.buildDcRhs(indexedNetwork, slackBusNum);

        try (LUDecomposition lu = lfMatrix.decomposeLU()) {
            lu.solve(rhs);
        }

        LOGGER.info("Result: {}", rhs);

        assertEquals(0d, rhs[0], 1E-14d);
        assertEquals(0.04383352433493455d, rhs[1], 1E-14d);
        assertEquals(0.11239308112163815d, rhs[2], 1E-14d);
        assertEquals(0.2202418845341654d, rhs[3], 1E-14d);

        LoadFlowMatrix.updateDcNetwork(indexedNetwork, rhs);

        logNetwork(network);

        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        indexedNetwork = IndexedNetwork.of(network);

        lfMatrix = LoadFlowMatrix.buildDc(indexedNetwork, slackBusNum, matrixFactory);
        rhs = LoadFlowMatrix.buildDcRhs(indexedNetwork, slackBusNum);
        try (LUDecomposition lu = lfMatrix.decomposeLU()) {
            lu.solve(rhs);
        }
        LoadFlowMatrix.updateDcNetwork(indexedNetwork, rhs);

        logNetwork(network);
    }

    private static void logNetwork(Network network) {
        network.getLoads().forEach(l ->  LOGGER.info("{} : p = {}.", l.getId(), l.getP0()));
        network.getGenerators().forEach(g ->  LOGGER.info("{} : p = {}.", g.getId(), g.getTargetP()));
        network.getBranchStream().forEach(b -> LOGGER.info("{} : p1 = {}, p2 = {}.",
                b.getId(), b.getTerminal1().getP(), b.getTerminal2().getP()));
    }

    /**
     * Check behaviour of the load flow for simple manipulations on eurostag example 1 network.
     *  - line opening
     *  - load change
     */
    @Test
    public void loadFlow() {
        Network network = EurostagTutorialExample1Factory.create();
        Line line1 = network.getLine("NHV1_NHV2_1");
        Line line2 = network.getLine("NHV1_NHV2_2");
        assertEquals(Double.NaN, line1.getTerminal1().getP(), 0);
        assertEquals(Double.NaN, line1.getTerminal2().getP(), 0);
        assertEquals(Double.NaN, line2.getTerminal1().getP(), 0);
        assertEquals(Double.NaN, line2.getTerminal2().getP(), 0);

        LoadFlow lf = new SimpleLoadFlowFactory(matrixFactory).create(network, null, 0);
        lf.run(network.getVariantManager().getWorkingVariantId(), new LoadFlowParameters());

        assertEquals(300, line1.getTerminal1().getP(), 0.01);
        assertEquals(-300, line1.getTerminal2().getP(), 0.01);
        assertEquals(300, line2.getTerminal1().getP(), 0.01);
        assertEquals(-300, line2.getTerminal2().getP(), 0.01);

        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        lf = new SimpleLoadFlowFactory(matrixFactory).create(network, null, 0);
        lf.run(network.getVariantManager().getWorkingVariantId(), new LoadFlowParameters());

        assertEquals(0, line1.getTerminal1().getP(), 0.01);
        assertEquals(0, line1.getTerminal2().getP(), 0.01);
        assertEquals(600, line2.getTerminal1().getP(), 0.01);
        assertEquals(-600, line2.getTerminal2().getP(), 0.01);

        network.getLoad("LOAD").setP0(450);

        lf = new SimpleLoadFlowFactory(matrixFactory).create(network, null, 0);
        lf.run(network.getVariantManager().getWorkingVariantId(), new LoadFlowParameters());

        assertEquals(0, line1.getTerminal1().getP(), 0.01);
        assertEquals(0, line1.getTerminal2().getP(), 0.01);
        assertEquals(450, line2.getTerminal1().getP(), 0.01);
        assertEquals(-450, line2.getTerminal2().getP(), 0.01);
    }

    @Test
    public void securityAnalysis() throws IOException {
        Network network = EurostagTutorialExample1Factory.createWithCurrentLimits();

        LimitViolationFilter currentFilter = new LimitViolationFilter(EnumSet.of(LimitViolationType.CURRENT));
        SecurityAnalysis securityAnalysis = new SimpleSecurityAnalysisFactory(matrixFactory).create(network, currentFilter, null, 0);

        ContingenciesProvider provider = n -> ImmutableList.of("NHV1_NHV2_1", "NHV1_NHV2_2").stream()
                .map(id -> new Contingency(id, new BranchContingency(id)))
                .collect(Collectors.toList());
        SecurityAnalysisResult res = securityAnalysis.run(network.getVariantManager().getWorkingVariantId(), new SecurityAnalysisParameters(), provider)
                .join();

        try (Writer writer = new OutputStreamWriter(LoggerFactory.getInfoPrintStream(LOGGER))) {
            Security.print(res, network, writer, new AsciiTableFormatterFactory(),
                    new Security.PostContingencyLimitViolationWriteConfig(null, new TableFormatterConfig(), true, false));
        }

        assertNotNull(res);
        assertNotNull(res.getPreContingencyResult());
        assertTrue(res.getPreContingencyResult().isComputationOk());

        //2 violations, 1 on each line
        assertTrue(res.getPreContingencyResult().getLimitViolations().isEmpty());

        List<PostContingencyResult> contingenciesResult = res.getPostContingencyResults();
        assertEquals(2, contingenciesResult.size());

        LimitViolationsResult contingency1 = contingenciesResult.get(0).getLimitViolationsResult();
        assertTrue(contingency1.isComputationOk());

        assertTrue(contingency1.getLimitViolations().isEmpty());

        LimitViolationsResult contingency2 = contingenciesResult.get(1).getLimitViolationsResult();
        assertTrue(contingency2.isComputationOk());

        //1 violation on the line which is still connected
        assertEquals(1, contingency2.getLimitViolations().stream()
                .filter(l -> l.getSubjectId().equals("NHV1_NHV2_1"))
                .count());
    }

}
