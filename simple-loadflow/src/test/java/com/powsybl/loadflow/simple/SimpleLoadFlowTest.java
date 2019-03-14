/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple;

import com.google.common.collect.ImmutableList;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.simple.equations.IndexedNetwork;
import com.powsybl.loadflow.simple.equations.LoadFlowMatrix;
import com.powsybl.security.*;
import org.junit.Test;
import org.ojalgo.RecoverableCondition;
import org.ojalgo.matrix.decomposition.LU;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PrimitiveDenseStore;
import org.ojalgo.matrix.store.SparseStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SimpleLoadFlowTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLoadFlowTest.class);

    @Test
    public void buildDcMatrix() throws RecoverableCondition {
        Network network = EurostagTutorialExample1Factory.create();

        logNetwork(network);

        network.getBusView().getBusStream()
                .forEach(b -> {
                    b.setAngle(0);
                    b.setV(b.getVoltageLevel().getNominalV());
                });

        IndexedNetwork indexedNetwork = IndexedNetwork.of(network);

        SparseStore<Double> lfMatrix = LoadFlowMatrix.buildDc(indexedNetwork);
        LOGGER.info("{}", lfMatrix);

        assertEquals(1d, lfMatrix.get(0, 0), 0d);
        assertEquals(0d, lfMatrix.get(0, 1), 0d);
        assertEquals(0d, lfMatrix.get(0, 2), 0d);
        assertEquals(0d, lfMatrix.get(0, 3), 0d);

        assertEquals(-821.2891969379839d, lfMatrix.get(1, 0), 0d);
        assertEquals(9572.804348453135d, lfMatrix.get(1, 1), 0d);
        assertEquals(-8751.515151515152d, lfMatrix.get(1, 2), 0d);
        assertEquals(0d, lfMatrix.get(1, 3), 0d);

        assertEquals(0d, lfMatrix.get(2, 0), 0d);
        assertEquals(-8751.515151515152d, lfMatrix.get(2, 1), 0d);
        assertEquals(22826.54714230771d, lfMatrix.get(2, 2), 0d);
        assertEquals(-14075.031990792557d, lfMatrix.get(2, 3), 0d);

        assertEquals(0d, lfMatrix.get(3, 0), 0d);
        assertEquals(0d, lfMatrix.get(3, 1), 0d);
        assertEquals(-14075.031990792557d, lfMatrix.get(3, 2), 0d);
        assertEquals(14075.031990792557d, lfMatrix.get(3, 3), 0d);

        PrimitiveDenseStore rhs = LoadFlowMatrix.buildDcRhs(indexedNetwork);

        LU<Double> lu = LU.PRIMITIVE.make();

        MatrixStore<Double> lhs = lu.solve(lfMatrix, rhs);

        LOGGER.info("Result: {}", lhs);

        assertEquals(2.214797674592096E-15d, lhs.get(0), 0d);
        assertEquals(0.7305587389155768, lhs.get(1), 0d);
        assertEquals(0.7991182957022801, lhs.get(2), 0d);
        assertEquals(0.8417469732351269, lhs.get(3), 0d);

        LoadFlowMatrix.updateNetwork(indexedNetwork, lhs);

        logNetwork(network);

        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        indexedNetwork = IndexedNetwork.of(network);

        lfMatrix = LoadFlowMatrix.buildDc(indexedNetwork);
        rhs = LoadFlowMatrix.buildDcRhs(indexedNetwork);
        lhs = lu.solve(lfMatrix, rhs);
        LoadFlowMatrix.updateNetwork(indexedNetwork, lhs);

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

        LoadFlow lf = new SimpleLoadFlowFactory().create(network, null, 0);
        lf.run(network.getVariantManager().getWorkingVariantId(), new LoadFlowParameters());

        assertEquals(-300, line1.getTerminal1().getP(), 0.01);
        assertEquals(300, line1.getTerminal2().getP(), 0.01);
        assertEquals(-300, line2.getTerminal1().getP(), 0.01);
        assertEquals(300, line2.getTerminal2().getP(), 0.01);

        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        lf = new SimpleLoadFlowFactory().create(network, null, 0);
        lf.run(network.getVariantManager().getWorkingVariantId(), new LoadFlowParameters());

        assertEquals(0, line1.getTerminal1().getP(), 0.01);
        assertEquals(0, line1.getTerminal2().getP(), 0.01);
        assertEquals(-600, line2.getTerminal1().getP(), 0.01);
        assertEquals(600, line2.getTerminal2().getP(), 0.01);

        network.getLoad("LOAD").setP0(450);

        lf = new SimpleLoadFlowFactory().create(network, null, 0);
        lf.run(network.getVariantManager().getWorkingVariantId(), new LoadFlowParameters());

        assertEquals(0, line1.getTerminal1().getP(), 0.01);
        assertEquals(0, line1.getTerminal2().getP(), 0.01);
        assertEquals(-450, line2.getTerminal1().getP(), 0.01);
        assertEquals(450, line2.getTerminal2().getP(), 0.01);
    }

    @Test
    public void securityAnalysis() {
        Network network = EurostagTutorialExample1Factory.createWithCurrentLimits();

        LimitViolationFilter currentFilter = new LimitViolationFilter(EnumSet.of(LimitViolationType.CURRENT));
        SecurityAnalysis securityAnalysis = new SimpleSecurityAnalysisFactory().create(network, currentFilter, null, 0);

        ContingenciesProvider provider = n -> ImmutableList.of("NHV1_NHV2_1", "NHV1_NHV2_2").stream()
                .map(id -> new Contingency(id, new BranchContingency(id)))
                .collect(Collectors.toList());
        SecurityAnalysisResult res = securityAnalysis.run(network.getVariantManager().getWorkingVariantId(), new SecurityAnalysisParameters(), provider)
                .join();

        assertNotNull(res);
        assertNotNull(res.getPreContingencyResult());
        assertTrue(res.getPreContingencyResult().isComputationOk());

        //2 violations, 1 on each line
        List<LimitViolation> nViolations = res.getPreContingencyResult().getLimitViolations();
        assertEquals(2, nViolations.size());

        List<PostContingencyResult> contingenciesResult = res.getPostContingencyResults();
        assertEquals(2, contingenciesResult.size());

        LimitViolationsResult contingency1 = contingenciesResult.get(0).getLimitViolationsResult();
        assertTrue(contingency1.isComputationOk());

        //2 violations on the line which is still connected
        List<LimitViolation> cont1Violations = contingency1.getLimitViolations().stream()
                .filter(l -> l.getSubjectId().equals("NHV1_NHV2_2"))
                .collect(Collectors.toList());
        assertEquals(2, cont1Violations.size());
    }

}
