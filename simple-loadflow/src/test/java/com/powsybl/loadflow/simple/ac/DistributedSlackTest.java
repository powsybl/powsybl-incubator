/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.network.extensions.ActivePowerControl;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.simple.SimpleLoadFlow;
import com.powsybl.loadflow.simple.SimpleLoadFlowParameters;
import com.powsybl.loadflow.simple.SlackBusSelectionMode;
import com.powsybl.loadflow.simple.network.DistributedSlackNetworkFactory;
import com.powsybl.math.matrix.DenseMatrixFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.powsybl.loadflow.simple.util.LoadFlowAssert.DELTA_POWER;
import static com.powsybl.loadflow.simple.util.LoadFlowAssert.assertActivePowerEquals;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DistributedSlackTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Network network;
    private Generator g1;
    private Generator g2;
    private Generator g3;
    private Generator g4;
    private SimpleLoadFlow loadFlow;
    private LoadFlowParameters parameters;

    @Before
    public void setUp() {
        network = DistributedSlackNetworkFactory.create();
        g1 = network.getGenerator("g1");
        g2 = network.getGenerator("g2");
        g3 = network.getGenerator("g3");
        g4 = network.getGenerator("g4");
        loadFlow = new SimpleLoadFlow(network, new DenseMatrixFactory());
        parameters = new LoadFlowParameters();
        SimpleLoadFlowParameters parametersExt = new SimpleLoadFlowParameters()
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED)
                .setDistributedSlack(true);
        parameters.addExtension(SimpleLoadFlowParameters.class, parametersExt);
    }

    @Test
    public void test() {
        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
        assertTrue(result.isOk());
        assertEquals(-115, g1.getTerminal().getP(), DELTA_POWER);
        assertEquals(-245, g2.getTerminal().getP(), DELTA_POWER);
        assertEquals(-105, g3.getTerminal().getP(), DELTA_POWER);
        assertEquals(-135, g4.getTerminal().getP(), DELTA_POWER);
        Line l14 = network.getLine("l14");
        Line l24 = network.getLine("l24");
        Line l34 = network.getLine("l34");
        assertActivePowerEquals(115, l14.getTerminal1());
        assertActivePowerEquals(-115, l14.getTerminal2());
        assertActivePowerEquals(245, l24.getTerminal1());
        assertActivePowerEquals(-245, l24.getTerminal2());
        assertActivePowerEquals(240, l34.getTerminal1());
        assertActivePowerEquals(-240, l34.getTerminal2());
    }

    @Test
    public void maxTest() {
        // decrease g1 max limit power, so that distributed slack algo reach the g1 max
        g1.setMaxP(105);
        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
        assertTrue(result.isOk());
        assertEquals(-105, g1.getTerminal().getP(), DELTA_POWER);
        assertEquals(-249.285, g2.getTerminal().getP(), DELTA_POWER);
        assertEquals(-106.428, g3.getTerminal().getP(), DELTA_POWER);
        assertEquals(-139.285, g4.getTerminal().getP(), DELTA_POWER);
    }

    @Test
    public void minTest() {
        // increase g1 min limit power and global load so that distributed slack algo reach the g1 min
        g1.setMinP(90);
        network.getLoad("l1").setP0(400);
        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
        assertTrue(result.isOk());
        assertEquals(-90, g1.getTerminal().getP(), DELTA_POWER);
        assertEquals(-170, g2.getTerminal().getP(), DELTA_POWER);
        assertEquals(-80, g3.getTerminal().getP(), DELTA_POWER);
        assertEquals(-60, g4.getTerminal().getP(), DELTA_POWER);
    }

    @Test
    public void zeroParticipatingGeneratorsTest() {
        g1.getExtension(ActivePowerControl.class).setDroop(2);
        g2.getExtension(ActivePowerControl.class).setDroop(-3);
        g3.getExtension(ActivePowerControl.class).setDroop(0);
        g4.getExtension(ActivePowerControl.class).setDroop(0);
        exception.expectCause(isA(PowsyblException.class));
        exception.expectMessage("No more generator participating to slack distribution");
        loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
    }

    @Test
    public void notEnoughActivePowerFailureTest() {
        network.getLoad("l1").setP0(1000);
        exception.expectCause(isA(PowsyblException.class));
        exception.expectMessage("Failed to distribute slack bus active power mismatch");
        loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
    }
}
