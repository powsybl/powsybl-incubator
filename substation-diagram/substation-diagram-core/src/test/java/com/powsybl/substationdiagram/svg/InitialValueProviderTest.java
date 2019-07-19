/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import com.powsybl.iidm.network.StaticVarCompensator.RegulationMode;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.model.Graph;
import com.rte_france.powsybl.iidm.network.extensions.cvg.BusbarSectionPosition;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class InitialValueProviderTest {

    private Network network;
    private Substation substation;
    private VoltageLevel vl;

    @Before
    public void setUp() {
        network = NetworkFactory.create("testCase1", "test");
        substation = network.newSubstation().setId("s").setCountry(Country.FR).add();
        vl = substation.newVoltageLevel().setId("vl").setTopologyKind(TopologyKind.NODE_BREAKER).setNominalV(400).add();
        VoltageLevel.NodeBreakerView view = vl.getNodeBreakerView().setNodeCount(10);
        BusbarSection bbs = view.newBusbarSection().setId("bbs").setNode(0).add();
        bbs.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs, 1, 1));
        BusbarSection bbs2 = view.newBusbarSection().setId("bbs2").setNode(3).add();
        bbs2.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs2, 2, 2));
        vl.newStaticVarCompensator()
            .setId("svc")
            .setName("svc")
            .setNode(2)
            .setBmin(0.0002)
            .setBmax(0.0008)
            .setRegulationMode(RegulationMode.VOLTAGE)
            .setVoltageSetPoint(390)
            .add();
        vl.newVscConverterStation()
            .setId("vsc")
            .setName("Converter1")
            .setNode(1)
            .setLossFactor(0.011f)
            .setVoltageSetpoint(405.0)
            .setVoltageRegulatorOn(true)
            .add();
        vl.newShuntCompensator()
            .setId("C1")
            .setName("Filter 1")
            .setNode(4)
            .setbPerSection(1e-5)
            .setCurrentSectionCount(1)
            .setMaximumSectionCount(1)
            .add();
        view.newDisconnector().setId("d").setNode1(0).setNode2(1).add();
        view.newBreaker().setId("b").setNode1(1).setNode2(2).add();
        view.newBreaker().setId("b2").setNode1(3).setNode2(4).add();
    }

    @Test
    public void test() {
        Network network2 = NetworkFactory.create("testCase2", "test2");
        DefaultSubstationDiagramInitialValueProvider initProvider = new DefaultSubstationDiagramInitialValueProvider(network2);
        Map<String, Graph> map = Graph.create(network, false);
        InitialValue init = initProvider.getInitialValue(map.get(vl.getId()).getNode("svc"));
        assertFalse(init.getLabel1().isPresent());
        assertFalse(init.getLabel2().isPresent());
        assertFalse(init.getLabel3().isPresent());
        assertFalse(init.getLabel4().isPresent());
        assertFalse(init.getArrowDirection1().isPresent());
        assertFalse(init.getArrowDirection2().isPresent());
        DefaultSubstationDiagramInitialValueProvider initProvider1 = new DefaultSubstationDiagramInitialValueProvider(network);
        InitialValue init1 = initProvider1.getInitialValue(map.get(vl.getId()).getNode("svc"));
        assertTrue(init1.getLabel1().isPresent());
        assertTrue(init1.getLabel2().isPresent());
        assertFalse(init1.getLabel3().isPresent());
        assertFalse(init1.getLabel4().isPresent());
        assertTrue(init1.getArrowDirection1().isPresent());
        assertTrue(init1.getArrowDirection2().isPresent());
        InitialValue init2 = initProvider1.getInitialValue(map.get(vl.getId()).getNode("vsc"));
        assertTrue(init2.getLabel1().isPresent());
        assertTrue(init2.getLabel2().isPresent());
        assertFalse(init2.getLabel3().isPresent());
        assertFalse(init2.getLabel4().isPresent());
        assertTrue(init2.getArrowDirection1().isPresent());
        assertTrue(init2.getArrowDirection2().isPresent());
        InitialValue init3 = initProvider1.getInitialValue(map.get(vl.getId()).getNode("C1"));
        assertTrue(init3.getLabel1().isPresent());
        assertTrue(init3.getLabel2().isPresent());
        assertFalse(init3.getLabel3().isPresent());
        assertFalse(init3.getLabel4().isPresent());
        assertTrue(init3.getArrowDirection1().isPresent());
        assertTrue(init3.getArrowDirection2().isPresent());
        InitialValue init4 = initProvider1.getInitialValue(map.get(vl.getId()).getNode("b"));
        assertFalse(init4.getLabel1().isPresent());
        assertFalse(init4.getLabel2().isPresent());
        assertFalse(init4.getLabel3().isPresent());
        assertFalse(init4.getLabel4().isPresent());
        assertFalse(init4.getArrowDirection1().isPresent());
        assertFalse(init4.getArrowDirection2().isPresent());
    }
}
