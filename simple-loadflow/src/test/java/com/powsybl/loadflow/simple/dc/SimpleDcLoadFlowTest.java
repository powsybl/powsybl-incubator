/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc;

import com.powsybl.iidm.api.Line;
import com.powsybl.iidm.api.Network;
import com.powsybl.iidm.api.TwoWindingsTransformer;
import com.powsybl.iidm.api.VariantManagerConstants;
import com.powsybl.iidm.api.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.api.test.PhaseShifterTestCaseFactory;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.simple.network.FourBusNetworkFactory;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SimpleDcLoadFlowTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDcLoadFlowTest.class);

    private final MatrixFactory matrixFactory = new DenseMatrixFactory();

    /**
     * Check behaviour of the load flow for simple manipulations on eurostag example 1 network.
     * - line opening
     * - load change
     */
    @Test
    public void tuto1Test() {
        Network network = EurostagTutorialExample1Factory.create();
        Line line1 = network.getLine("NHV1_NHV2_1");
        Line line2 = network.getLine("NHV1_NHV2_2");

        assertEquals(Double.NaN, line1.getTerminal1().getP(), 0);
        assertEquals(Double.NaN, line1.getTerminal2().getP(), 0);
        assertEquals(Double.NaN, line2.getTerminal1().getP(), 0);
        assertEquals(Double.NaN, line2.getTerminal2().getP(), 0);

        LoadFlowParameters parameters = new LoadFlowParameters();
        LoadFlow lf = new SimpleDcLoadFlowFactory(matrixFactory).create(network, null, 0);
        lf.run(network.getVariantManager().getWorkingVariantId(), parameters);

        assertEquals(300, line1.getTerminal1().getP(), 0.01);
        assertEquals(-300, line1.getTerminal2().getP(), 0.01);
        assertEquals(300, line2.getTerminal1().getP(), 0.01);
        assertEquals(-300, line2.getTerminal2().getP(), 0.01);

        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();

        lf.run(network.getVariantManager().getWorkingVariantId(), parameters);

        assertTrue(Double.isNaN(line1.getTerminal1().getP()));
        assertEquals(0, line1.getTerminal2().getP(), 0);
        assertEquals(600, line2.getTerminal1().getP(), 0.01);
        assertEquals(-600, line2.getTerminal2().getP(), 0.01);

        network.getLine("NHV1_NHV2_1").getTerminal1().connect();
        network.getLine("NHV1_NHV2_1").getTerminal2().disconnect();

        lf.run(network.getVariantManager().getWorkingVariantId(), parameters);

        assertEquals(0, line1.getTerminal1().getP(), 0);
        assertTrue(Double.isNaN(line1.getTerminal2().getP()));
        assertEquals(600, line2.getTerminal1().getP(), 0.01);
        assertEquals(-600, line2.getTerminal2().getP(), 0.01);

        network.getLine("NHV1_NHV2_1").getTerminal1().disconnect();
        network.getLoad("LOAD").setP0(450);

        lf.run(network.getVariantManager().getWorkingVariantId(), parameters);

        assertTrue(Double.isNaN(line1.getTerminal1().getP()));
        assertTrue(Double.isNaN(line1.getTerminal2().getP()));
        assertEquals(450, line2.getTerminal1().getP(), 0.01);
        assertEquals(-450, line2.getTerminal2().getP(), 0.01);
    }

    @Test
    public void fourBusesTest() {
        Network network = FourBusNetworkFactory.create();

        LoadFlow lf = new SimpleDcLoadFlowFactory(matrixFactory).create(network, null, 0);
        lf.run(VariantManagerConstants.INITIAL_VARIANT_ID, new LoadFlowParameters());

        Line l14 = network.getLine("l14");
        Line l12 = network.getLine("l12");
        Line l23 = network.getLine("l23");
        Line l34 = network.getLine("l34");
        Line l13 = network.getLine("l13");

        assertEquals(0.25, l14.getTerminal1().getP(), 0.01);
        assertEquals(-0.25, l14.getTerminal2().getP(), 0.01);
        assertEquals(0.25, l12.getTerminal1().getP(), 0.01);
        assertEquals(-0.25, l12.getTerminal2().getP(), 0.01);
        assertEquals(1.25, l23.getTerminal1().getP(), 0.01);
        assertEquals(-1.25, l23.getTerminal2().getP(), 0.01);
        assertEquals(-1.25, l34.getTerminal1().getP(), 0.01);
        assertEquals(1.25, l34.getTerminal2().getP(), 0.01);
        assertEquals(1.5, l13.getTerminal1().getP(), 0.01);
        assertEquals(-1.5, l13.getTerminal2().getP(), 0.01);
    }

    @Test
    public void phaseShifterTest() {
        Network network = PhaseShifterTestCaseFactory.create();
        Line l1 = network.getLine("L1");
        Line l2 = network.getLine("L2");
        TwoWindingsTransformer ps1 = network.getTwoWindingsTransformer("PS1");
        ps1.getPhaseTapChanger().getStep(0).setAlpha(5);
        ps1.getPhaseTapChanger().getStep(2).setAlpha(5);

        LoadFlowParameters parameters = new LoadFlowParameters();
        LoadFlow lf = new SimpleDcLoadFlowFactory(matrixFactory).create(network, null, 0);
        lf.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters);

        assertEquals(50, l1.getTerminal1().getP(), 0.01);
        assertEquals(-50, l1.getTerminal2().getP(), 0.01);
        assertEquals(50, l2.getTerminal1().getP(), 0.01);
        assertEquals(-50, l2.getTerminal2().getP(), 0.01);
        assertEquals(50, ps1.getTerminal1().getP(), 0.01);
        assertEquals(-50, ps1.getTerminal2().getP(), 0.01);

        ps1.getPhaseTapChanger().setTapPosition(2);

        lf.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters);

        assertEquals(18.5, l1.getTerminal1().getP(), 0.01);
        assertEquals(-18.5, l1.getTerminal2().getP(), 0.01);
        assertEquals(81.5, l2.getTerminal1().getP(), 0.01);
        assertEquals(-81.5, l2.getTerminal2().getP(), 0.01);
        assertEquals(81.5, ps1.getTerminal1().getP(), 0.01);
        assertEquals(-81.5, ps1.getTerminal2().getP(), 0.01);
    }
}
