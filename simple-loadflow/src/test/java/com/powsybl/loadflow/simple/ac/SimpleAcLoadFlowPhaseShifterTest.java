/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.PhaseShifterTestCaseFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.DenseMatrixFactory;
import org.junit.Before;
import org.junit.Test;

import static com.powsybl.loadflow.simple.util.LoadFlowAssert.*;
import static org.junit.Assert.assertTrue;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleAcLoadFlowPhaseShifterTest {

    private Network network;
    private Bus bus1;
    private Bus bus2;
    private Bus bus3;
    private Line line1;
    private Line line2;
    private TwoWindingsTransformer ps1;

    private SimpleAcLoadFlow loadFlow;
    private LoadFlowParameters parameters;

    @Before
    public void setUp() {
        network = PhaseShifterTestCaseFactory.create();
        bus1 = network.getBusBreakerView().getBusStream().filter(b -> b.getId().equals("B1")).findFirst().orElseThrow(AssertionError::new);
        bus2 = network.getBusBreakerView().getBusStream().filter(b -> b.getId().equals("B2")).findFirst().orElseThrow(AssertionError::new);
        bus3 = network.getBusBreakerView().getBusStream().filter(b -> b.getId().equals("B3")).findFirst().orElseThrow(AssertionError::new);

        line1 = network.getLine("L1");
        line2 = network.getLine("L2");
        ps1 = network.getTwoWindingsTransformer("PS1");
        ps1.getPhaseTapChanger().getStep(0).setAlpha(5);
        ps1.getPhaseTapChanger().getStep(2).setAlpha(5);

        loadFlow = new SimpleAcLoadFlow(network, new DenseMatrixFactory());
        parameters = new LoadFlowParameters();
        parameters.addExtension(SimpleAcLoadFlowParameters.class, new SimpleAcLoadFlowParameters().setSlackBusSelection(SimpleAcLoadFlowParameters.SlackBusSelection.FIRST));
    }

    @Test
    public void baseCaseTest() {
        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
        assertTrue(result.isOk());

        assertVoltageEquals(400, bus1);
        assertAngleEquals(0, bus1);
        assertVoltageEquals(385.693, bus2);
        assertAngleEquals(-3.679206, bus2);
        assertVoltageEquals(392.644, bus3);
        assertAngleEquals(-1.806094, bus3);
        assertActivePowerEquals(50.084, line1.getTerminal1());
        assertReactivePowerEquals(29.201, line1.getTerminal1());
        assertActivePowerEquals(-50, line1.getTerminal2());
        assertReactivePowerEquals(-25, line1.getTerminal2());
        assertActivePowerEquals(50.042, line2.getTerminal1());
        assertReactivePowerEquals(27.1, line2.getTerminal1());
        assertActivePowerEquals(-50, line2.getTerminal2());
        assertReactivePowerEquals(-25, line2.getTerminal2());
    }

    @Test
    public void tapPlusOneTest() {
        ps1.getPhaseTapChanger().setTapPosition(2);

        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
        assertTrue(result.isOk());

        assertVoltageEquals(400, bus1);
        assertAngleEquals(0, bus1);
        assertVoltageEquals(385.296, bus2);
        assertAngleEquals(-1.186517, bus2);
        assertVoltageEquals(392.076, bus3);
        assertAngleEquals(1.964715, bus3);
        assertActivePowerEquals(16.541, line1.getTerminal1());
        assertReactivePowerEquals(29.241, line1.getTerminal1());
        assertActivePowerEquals(-16.513, line1.getTerminal2());
        assertReactivePowerEquals(-27.831, line1.getTerminal2());
        assertActivePowerEquals(83.587, line2.getTerminal1());
        assertReactivePowerEquals(27.195, line2.getTerminal1());
        assertActivePowerEquals(-83.487, line2.getTerminal2());
        assertReactivePowerEquals(-22.169, line2.getTerminal2());
    }
}
