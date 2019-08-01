/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import com.powsybl.cgmes.iidm.extensions.dl.*;
import com.powsybl.iidm.network.*;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.BusNode;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class BusTopologyTest extends AbstractCgmesVoltageLevelLayoutTest {

    private VoltageLevel voltageLevel;

    @Before
    public void setUp() {
        createNetwork();
    }

    private void createNetwork() {
        Network network = Network.create("test", "test");
        Substation substation = network.newSubstation()
                .setId("Substation")
                .setCountry(Country.FR)
                .add();
        voltageLevel = createFirstVoltageLevel(substation);
        createSecondVoltageLevel(substation);
        createTransformer(substation);
        addDiagramData(network);
    }

    private VoltageLevel createFirstVoltageLevel(Substation substation) {
        VoltageLevel voltageLevel1 = substation.newVoltageLevel()
                .setId("VoltageLevel1")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevel1.getBusBreakerView().newBus()
                .setId("Bus1")
                .add();
        voltageLevel1.newLoad()
                .setId("Load")
                .setBus("Bus1")
                .setConnectableBus("Bus1")
                .setP0(100)
                .setQ0(50)
                .add();
        voltageLevel1.newShuntCompensator()
                .setId("Shunt")
                .setBus("Bus1")
                .setConnectableBus("Bus1")
                .setbPerSection(1e-5)
                .setCurrentSectionCount(1)
                .setMaximumSectionCount(1)
                .add();
        voltageLevel1.newDanglingLine()
                .setId("DanglingLine")
                .setBus("Bus1")
                .setR(10.0)
                .setX(1.0)
                .setB(10e-6)
                .setG(10e-5)
                .setP0(50.0)
                .setQ0(30.0)
                .add();
        return voltageLevel1;
    }

    private void createSecondVoltageLevel(Substation substation) {
        VoltageLevel voltageLevel2 = substation.newVoltageLevel()
                .setId("VoltageLevel2")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevel2.getBusBreakerView().newBus()
                .setId("Bus2")
                .add();
    }

    private void createTransformer(Substation substation) {
        int zb380 = 380 * 380 / 100;
        substation.newTwoWindingsTransformer()
                .setId("Transformer")
                .setVoltageLevel1("VoltageLevel1")
                .setBus1("Bus1")
                .setConnectableBus1("Bus1")
                .setRatedU1(24.0)
                .setVoltageLevel2("VoltageLevel2")
                .setBus2("Bus2")
                .setConnectableBus2("Bus2")
                .setRatedU2(400.0)
                .setR(0.24 / 1300 * zb380)
                .setX(Math.sqrt(10 * 10 - 0.24 * 0.24) / 1300 * zb380)
                .setG(0.0)
                .setB(0.0)
                .add();
    }

    private void addDiagramData(Network network) {
        Bus bus = voltageLevel.getBusBreakerView().getBus("Bus1");
        NodeDiagramData<Bus> busDiagramData = new NodeDiagramData<>(bus);
        busDiagramData.setPoint1(new DiagramPoint(60, 10, 1));
        busDiagramData.setPoint2(new DiagramPoint(60, 70, 2));
        bus.addExtension(NodeDiagramData.class, busDiagramData);

        Load load = network.getLoad("Load");
        InjectionDiagramData<Load> loadDiagramData = new InjectionDiagramData<>(load, new DiagramPoint(10, 20, 0), 90);
        loadDiagramData.addTerminalPoint(new DiagramPoint(15, 20, 2));
        loadDiagramData.addTerminalPoint(new DiagramPoint(60, 20, 1));
        load.addExtension(InjectionDiagramData.class, loadDiagramData);

        ShuntCompensator shunt = network.getShuntCompensator("Shunt");
        InjectionDiagramData<ShuntCompensator> shuntDiagramData = new InjectionDiagramData<>(shunt, new DiagramPoint(15, 55, 0), 90);
        shuntDiagramData.addTerminalPoint(new DiagramPoint(20, 55, 1));
        shuntDiagramData.addTerminalPoint(new DiagramPoint(60, 55, 2));
        shunt.addExtension(InjectionDiagramData.class, shuntDiagramData);

        DanglingLine danglingLine = network.getDanglingLine("DanglingLine");
        LineDiagramData<DanglingLine> danglingLineDiagramData = new LineDiagramData<>(danglingLine);
        danglingLineDiagramData.addPoint(new DiagramPoint(60, 60, 1));
        danglingLineDiagramData.addPoint(new DiagramPoint(120, 60, 2));
        danglingLine.addExtension(LineDiagramData.class, danglingLineDiagramData);

        TwoWindingsTransformer twt = network.getTwoWindingsTransformer("Transformer");
        CouplingDeviceDiagramData<TwoWindingsTransformer> twtDiagramData = new CouplingDeviceDiagramData<>(twt, new DiagramPoint(100, 15, 0), 90);
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(95, 15, 1));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(60, 15, 2));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 15, 1));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(120, 15, 2));
        twt.addExtension(CouplingDeviceDiagramData.class, twtDiagramData);
    }

    @Test
    public void test() {
        test(voltageLevel);
    }

    @Override
    protected void checkGraph(Graph graph) {
        assertEquals(5, graph.getNodes().size());

        assertEquals(Node.NodeType.BUS, graph.getNodes().get(0).getType());
        assertEquals(Node.NodeType.FEEDER, graph.getNodes().get(1).getType());
        assertEquals(Node.NodeType.FEEDER, graph.getNodes().get(2).getType());
        assertEquals(Node.NodeType.FEEDER, graph.getNodes().get(3).getType());
        assertEquals(Node.NodeType.FEEDER, graph.getNodes().get(4).getType());

        assertEquals("Bus1", graph.getNodes().get(0).getId());
        assertEquals("Load", graph.getNodes().get(1).getId());
        assertEquals("Shunt", graph.getNodes().get(2).getId());
        assertEquals("DanglingLine", graph.getNodes().get(3).getId());
        assertEquals("Transformer_ONE", graph.getNodes().get(4).getId());

        assertEquals(ComponentType.BUSBAR_SECTION, graph.getNodes().get(0).getComponentType());
        assertEquals(ComponentType.LOAD, graph.getNodes().get(1).getComponentType());
        assertEquals(ComponentType.CAPACITOR, graph.getNodes().get(2).getComponentType());
        assertEquals(ComponentType.DANGLING_LINE, graph.getNodes().get(3).getComponentType());
        assertEquals(ComponentType.TWO_WINDINGS_TRANSFORMER, graph.getNodes().get(4).getComponentType());

        assertEquals(4, graph.getNodes().get(0).getAdjacentNodes().size());
        checkAdjacentNodes(graph.getNodes().get(0), Arrays.asList("Load", "Shunt", "DanglingLine", "Transformer_ONE"));
        checkBusConnection(graph.getNodes().get(1));
        checkBusConnection(graph.getNodes().get(2));
        checkBusConnection(graph.getNodes().get(3));
        checkBusConnection(graph.getNodes().get(4));

        assertEquals(4, graph.getEdges().size());
    }

    private void checkBusConnection(Node node) {
        assertEquals(1, node.getAdjacentNodes().size());
        assertEquals("Bus1", node.getAdjacentNodes().get(0).getId());
    }

    @Override
    protected void checkCoordinates(Graph graph) {
        assertEquals(120, graph.getNodes().get(0).getX(), 0);
        assertEquals(10, graph.getNodes().get(0).getY(), 0);
        assertEquals(120, ((BusNode) graph.getNodes().get(0)).getPxWidth(), 0);
        assertTrue(graph.getNodes().get(0).isRotated());
        assertEquals(20, graph.getNodes().get(1).getX(), 0);
        assertEquals(30, graph.getNodes().get(1).getY(), 0);
        assertTrue(graph.getNodes().get(1).isRotated());
        assertEquals(30, graph.getNodes().get(2).getX(), 0);
        assertEquals(100, graph.getNodes().get(2).getY(), 0);
        assertTrue(graph.getNodes().get(2).isRotated());
        assertEquals(160, graph.getNodes().get(3).getX(), 0);
        assertEquals(110, graph.getNodes().get(3).getY(), 0);
        assertTrue(graph.getNodes().get(3).isRotated());
        assertEquals(200, graph.getNodes().get(4).getX(), 0);
        assertEquals(20, graph.getNodes().get(4).getY(), 0);
        assertFalse(graph.getNodes().get(4).isRotated());
    }

}
