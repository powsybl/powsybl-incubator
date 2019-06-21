/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.simple.network.SlackBusSelectionMode;
import com.powsybl.loadflow.simple.network.TwoBusNetworkFactory;
import com.powsybl.math.matrix.DenseMatrixFactory;
import org.junit.Before;
import org.junit.Test;

import static com.powsybl.loadflow.simple.util.LoadFlowAssert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleAcLoadFlowTwoBusNetworkTest {

    private Network network;
    private Bus bus1;
    private Bus bus2;
    private Line line1;

    private SimpleAcLoadFlow loadFlow;
    private LoadFlowParameters parameters;

    @Before
    public void setUp() {
        network = TwoBusNetworkFactory.create();
        bus1 = network.getBusBreakerView().getBus("b1");
        bus2 = network.getBusBreakerView().getBus("b2");
        line1 = network.getLine("l12");

        loadFlow = new SimpleAcLoadFlow(network, new DenseMatrixFactory());
        parameters = new LoadFlowParameters();
        parameters.addExtension(SimpleAcLoadFlowParameters.class, new SimpleAcLoadFlowParameters().setSlackBusSelectionMode(SlackBusSelectionMode.FIRST));
    }

    @Test
    public void baseCaseTest() {
        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
        assertTrue(result.isOk());

        assertVoltageEquals(1, bus1);
        assertAngleEquals(0, bus1);
        assertVoltageEquals(0.855, bus2);
        assertAngleEquals(-13.521852, bus2);
        assertActivePowerEquals(2, line1.getTerminal1());
        assertReactivePowerEquals(1.683, line1.getTerminal1());
        assertActivePowerEquals(-2, line1.getTerminal2());
        assertReactivePowerEquals(-1, line1.getTerminal2());
    }

    @Test
    public void voltageInitModeTest() {
        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
        assertTrue(result.isOk());
        assertEquals("4", result.getMetrics().get("iterations"));
        // restart loadflow from previous calculated state, it should convergence in zero iteration
        result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters.setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES))
                .join();
        assertTrue(result.isOk());
        assertEquals("0", result.getMetrics().get("iterations"));
    }

    @Test
    public void withAnAdditionalBattery() {
        bus2.getVoltageLevel().newBattery()
                .setId("bt2")
                .setBus("b2")
                .setP0(1)
                .setQ0(0.1)
                .setMinP(0)
                .setMaxP(1)
                .add();
        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
        assertTrue(result.isOk());
        assertVoltageEquals(1, bus1);
        assertAngleEquals(0, bus1);
        assertVoltageEquals(0.783, bus2);
        assertAngleEquals(-22.518194, bus2);
        assertActivePowerEquals(3, line1.getTerminal1());
        assertReactivePowerEquals(2.764, line1.getTerminal1());
        assertActivePowerEquals(-3, line1.getTerminal2());
        assertReactivePowerEquals(-1.1, line1.getTerminal2());
    }
}
