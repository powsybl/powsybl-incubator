/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.powsybl.cgmes.iidm.extensions.dl.CouplingDeviseDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.DiagramPoint;
import com.powsybl.cgmes.iidm.extensions.dl.DiagramTerminal;
import com.powsybl.cgmes.iidm.extensions.dl.InjectionDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.LineDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.NodeDiagramData;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.BusNode;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class TestCase1 {

    private VoltageLevel voltageLevel;
    private VoltageLevel voltageLevelWithInternalConnections;

    private void createNetwork() {
        Network network = NetworkFactory.create("testCase1", "test");
        Substation substation1 = network.newSubstation()
                .setId("Substation")
                .setCountry(Country.FR)
                .add();
        voltageLevel = substation1.newVoltageLevel()
                .setId("VoltageLevel1")
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .setNominalV(400)
                .add();
        VoltageLevel.NodeBreakerView view = voltageLevel.getNodeBreakerView()
                .setNodeCount(10);
        Substation substation2 = network.newSubstation()
                .setId("Substation2")
                .setCountry(Country.FR)
                .add();
        VoltageLevel voltageLevel2 = substation2.newVoltageLevel()
                .setId("VoltageLevel2")
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .setNominalV(400)
                .add();
        voltageLevel2.getNodeBreakerView()
                .setNodeCount(10);
        BusbarSection busbarSection = view.newBusbarSection()
                .setId("BusbarSection")
                .setNode(0)
                .add();
        NodeDiagramData<BusbarSection> busbarDiagramData = new NodeDiagramData<>(busbarSection);
        busbarDiagramData.setPoint1(new DiagramPoint(20, 115, 1));
        busbarDiagramData.setPoint2(new DiagramPoint(180, 115, 2));
        busbarSection.addExtension(NodeDiagramData.class, busbarDiagramData);
        Generator generator = voltageLevel.newGenerator()
                .setId("Generator")
                .setNode(4)
                .setTargetP(100)
                .setTargetV(380)
                .setVoltageRegulatorOn(true)
                .setMaxP(100)
                .setMinP(0)
                .add();
        InjectionDiagramData<Generator> generatorDiagramData = new InjectionDiagramData<>(generator, new DiagramPoint(105, 230, 0), 0);
        generatorDiagramData.addTerminalPoint(new DiagramPoint(105, 225, 1));
        generatorDiagramData.addTerminalPoint(new DiagramPoint(105, 115, 2));
        generator.addExtension(InjectionDiagramData.class, generatorDiagramData);
        view.newInternalConnection()
                .setNode1(4)
                .setNode2(0)
                .add();
        Switch disconnector1 = view.newDisconnector()
                .setId("Disconnector1")
                .setNode1(0)
                .setNode2(1)
                .add();
        CouplingDeviseDiagramData<Switch> disconnector1DiagramData = new CouplingDeviseDiagramData<>(disconnector1, new DiagramPoint(105, 100, 0), 0);
        disconnector1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 95, 1));
        disconnector1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 90, 2));
        disconnector1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 105, 1));
        disconnector1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 115, 2));
        disconnector1.addExtension(CouplingDeviseDiagramData.class, disconnector1DiagramData);
        Switch breaker1 = view.newBreaker()
                .setId("Breaker1")
                .setNode1(1)
                .setNode2(2)
                .add();
        CouplingDeviseDiagramData<Switch> breaker1DiagramData = new CouplingDeviseDiagramData<>(breaker1, new DiagramPoint(105, 80, 0), 0);
        breaker1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 85, 1));
        breaker1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 90, 2));
        breaker1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 75, 1));
        breaker1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 70, 2));
        breaker1.addExtension(CouplingDeviseDiagramData.class, breaker1DiagramData);
        Switch disconnector2 = view.newDisconnector()
                .setId("Disconnector2")
                .setNode1(2)
                .setNode2(3)
                .add();
        CouplingDeviseDiagramData<Switch> disconnector2DiagramData = new CouplingDeviseDiagramData<>(disconnector2, new DiagramPoint(105, 60, 0), 0);
        disconnector2DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 65, 1));
        disconnector2DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 70, 2));
        disconnector2DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 55, 1));
        disconnector2DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 50, 2));
        disconnector2.addExtension(CouplingDeviseDiagramData.class, disconnector2DiagramData);
        Line line = network.newLine()
                .setId("Line")
                .setVoltageLevel1("VoltageLevel1")
                .setNode1(3)
                .setVoltageLevel2("VoltageLevel2")
                .setNode2(0)
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();
        LineDiagramData<Line> lineDiagramData = new LineDiagramData<>(line);
        lineDiagramData.addPoint(new DiagramPoint(105, 50, 1));
        lineDiagramData.addPoint(new DiagramPoint(105, 10, 2));
        line.addExtension(LineDiagramData.class, lineDiagramData);
    }

    private void createNetworkWithInternalConnections() {
        Network network = NetworkFactory.create("testCase1", "test");
        Substation substation1 = network.newSubstation()
                .setId("Substation")
                .setCountry(Country.FR)
                .add();
        voltageLevelWithInternalConnections = substation1.newVoltageLevel()
                .setId("VoltageLevel1")
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .setNominalV(400)
                .add();
        VoltageLevel.NodeBreakerView view = voltageLevelWithInternalConnections.getNodeBreakerView()
                .setNodeCount(20);
        Substation substation2 = network.newSubstation()
                .setId("Substation2")
                .setCountry(Country.FR)
                .add();
        VoltageLevel voltageLevel2 = substation2.newVoltageLevel()
                .setId("VoltageLevel2")
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .setNominalV(400)
                .add();
        voltageLevel2.getNodeBreakerView()
                .setNodeCount(10);
        BusbarSection busbarSection = view.newBusbarSection()
                .setId("BusbarSection")
                .setNode(4)
                .add();
        NodeDiagramData<BusbarSection> busbarDiagramData = new NodeDiagramData<>(busbarSection);
        busbarDiagramData.setPoint1(new DiagramPoint(20, 115, 1));
        busbarDiagramData.setPoint2(new DiagramPoint(180, 115, 2));
        busbarSection.addExtension(NodeDiagramData.class, busbarDiagramData);
        Generator generator = voltageLevelWithInternalConnections.newGenerator()
                .setId("Generator")
                .setNode(12)
                .setTargetP(100)
                .setTargetV(380)
                .setVoltageRegulatorOn(true)
                .setMaxP(100)
                .setMinP(0)
                .add();
        InjectionDiagramData<Generator> generatorDiagramData = new InjectionDiagramData<>(generator, new DiagramPoint(105, 230, 0), 0);
        generatorDiagramData.addTerminalPoint(new DiagramPoint(105, 225, 1));
        generatorDiagramData.addTerminalPoint(new DiagramPoint(105, 115, 1));
        generator.addExtension(InjectionDiagramData.class, generatorDiagramData);
        Switch disconnector1 = view.newDisconnector()
                .setId("Disconnector1")
                .setNode1(7)
                .setNode2(8)
                .add();
        CouplingDeviseDiagramData<Switch> disconnector1DiagramData = new CouplingDeviseDiagramData<>(disconnector1, new DiagramPoint(105, 100, 0), 0);
        disconnector1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 95, 1));
        disconnector1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 90, 2));
        disconnector1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 105, 1));
        disconnector1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 115, 2));
        disconnector1.addExtension(CouplingDeviseDiagramData.class, disconnector1DiagramData);
        Switch breaker1 = view.newBreaker()
                .setId("Breaker1")
                .setNode1(5)
                .setNode2(6)
                .add();
        CouplingDeviseDiagramData<Switch> breaker1DiagramData = new CouplingDeviseDiagramData<>(breaker1, new DiagramPoint(105, 80, 0), 0);
        breaker1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 85, 1));
        breaker1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 90, 2));
        breaker1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 75, 1));
        breaker1DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 70, 2));
        breaker1.addExtension(CouplingDeviseDiagramData.class, breaker1DiagramData);
        Switch disconnector2 = view.newDisconnector()
                .setId("Disconnector2")
                .setNode1(9)
                .setNode2(10)
                .add();
        CouplingDeviseDiagramData<Switch> disconnector2DiagramData = new CouplingDeviseDiagramData<>(disconnector2, new DiagramPoint(105, 60, 0), 0);
        disconnector2DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 65, 1));
        disconnector2DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(105, 70, 2));
        disconnector2DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 55, 1));
        disconnector2DiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(105, 50, 2));
        disconnector2.addExtension(CouplingDeviseDiagramData.class, disconnector2DiagramData);
        Line line = network.newLine()
                .setId("Line")
                .setVoltageLevel1("VoltageLevel1")
                .setNode1(11)
                .setVoltageLevel2("VoltageLevel2")
                .setNode2(0)
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();
        LineDiagramData<Line> lineDiagramData = new LineDiagramData<>(line);
        lineDiagramData.addPoint(new DiagramPoint(105, 50, 1));
        lineDiagramData.addPoint(new DiagramPoint(105, 10, 2));
        line.addExtension(LineDiagramData.class, lineDiagramData);
        view.newInternalConnection()
                .setNode1(4)
                .setNode2(0)
                .add();
        view.newInternalConnection()
                .setNode1(5)
                .setNode2(2)
                .add();
        view.newInternalConnection()
                .setNode1(6)
                .setNode2(1)
                .add();
        view.newInternalConnection()
                .setNode1(7)
                .setNode2(1)
                .add();
        view.newInternalConnection()
                .setNode1(8)
                .setNode2(0)
                .add();
        view.newInternalConnection()
                .setNode1(9)
                .setNode2(3)
                .add();
        view.newInternalConnection()
                .setNode1(10)
                .setNode2(2)
                .add();
        view.newInternalConnection()
                .setNode1(11)
                .setNode2(3)
                .add();
        view.newInternalConnection()
                .setNode1(12)
                .setNode2(0)
                .add();
    }

    @Before
    public void setUp() {
        createNetwork();
        createNetworkWithInternalConnections();
    }

    private void checkGraph(Graph graph) {
        assertEquals(6, graph.getNodes().size());

        assertEquals(Node.NodeType.BUS, graph.getNodes().get(0).getType());
        assertEquals(Node.NodeType.FEEDER, graph.getNodes().get(1).getType());
        assertEquals(Node.NodeType.FEEDER, graph.getNodes().get(2).getType());
        assertEquals(Node.NodeType.SWITCH, graph.getNodes().get(3).getType());
        assertEquals(Node.NodeType.SWITCH, graph.getNodes().get(4).getType());
        assertEquals(Node.NodeType.SWITCH, graph.getNodes().get(5).getType());

        assertEquals("BusbarSection", graph.getNodes().get(0).getId());
        assertEquals("Line_ONE", graph.getNodes().get(1).getId());
        assertEquals("Generator", graph.getNodes().get(2).getId());
        assertEquals("Disconnector1", graph.getNodes().get(3).getId());
        assertEquals("Breaker1", graph.getNodes().get(4).getId());
        assertEquals("Disconnector2", graph.getNodes().get(5).getId());

        assertEquals(ComponentType.BUSBAR_SECTION, graph.getNodes().get(0).getComponentType());
        assertEquals(ComponentType.LINE, graph.getNodes().get(1).getComponentType());
        assertEquals(ComponentType.GENERATOR, graph.getNodes().get(2).getComponentType());
        assertEquals(ComponentType.DISCONNECTOR, graph.getNodes().get(3).getComponentType());
        assertEquals(ComponentType.BREAKER, graph.getNodes().get(4).getComponentType());
        assertEquals(ComponentType.DISCONNECTOR, graph.getNodes().get(5).getComponentType());

        assertEquals(2, graph.getNodes().get(0).getAdjacentNodes().size());
        checkAdjacentNodes(graph.getNodes().get(0), Arrays.asList("Disconnector1", "Generator"));
        assertEquals(1, graph.getNodes().get(1).getAdjacentNodes().size());
        assertEquals("Disconnector2", graph.getNodes().get(1).getAdjacentNodes().get(0).getId());
        assertEquals(1, graph.getNodes().get(2).getAdjacentNodes().size());
        assertEquals("BusbarSection", graph.getNodes().get(2).getAdjacentNodes().get(0).getId());
        assertEquals(2, graph.getNodes().get(3).getAdjacentNodes().size());
        checkAdjacentNodes(graph.getNodes().get(3), Arrays.asList("BusbarSection", "Breaker1"));
        assertEquals(2, graph.getNodes().get(4).getAdjacentNodes().size());
        checkAdjacentNodes(graph.getNodes().get(4), Arrays.asList("Disconnector1", "Disconnector2"));
        assertEquals(2, graph.getNodes().get(5).getAdjacentNodes().size());
        checkAdjacentNodes(graph.getNodes().get(5), Arrays.asList("Breaker1", "Line_ONE"));

        assertEquals(5, graph.getEdges().size());
    }

    private void checkAdjacentNodes(Node node, List<String> expectedAdjacentNodes) {
        node.getAdjacentNodes().forEach(adjacentNode -> {
            assertTrue(expectedAdjacentNodes.contains(adjacentNode.getId()));
        });
    }

    private void checkCoordinates(Graph graph) {
        assertEquals(40, graph.getNodes().get(0).getX(), 0);
        assertEquals(150, graph.getNodes().get(0).getY(), 0);
        assertEquals(320, ((BusNode) graph.getNodes().get(0)).getPxWidth(), 0);
        assertFalse(graph.getNodes().get(0).isRotated());
        assertEquals(210, graph.getNodes().get(1).getX(), 0);
        assertEquals(20, graph.getNodes().get(1).getY(), 0);
        assertEquals(210, graph.getNodes().get(2).getX(), 0);
        assertEquals(380, graph.getNodes().get(2).getY(), 0);
        assertEquals(210, graph.getNodes().get(3).getX(), 0);
        assertEquals(120, graph.getNodes().get(3).getY(), 0);
        assertEquals(210, graph.getNodes().get(4).getX(), 0);
        assertEquals(80, graph.getNodes().get(4).getY(), 0);
        assertEquals(210, graph.getNodes().get(5).getX(), 0);
        assertEquals(40, graph.getNodes().get(5).getY(), 0);
    }

    private void test(VoltageLevel vl) {
        Graph graph = Graph.create(vl);
        LayoutParameters layoutParameters = new LayoutParameters();
        layoutParameters.setScaleFactor(2);
        new CgmesVoltageLevelLayout(graph).run(layoutParameters);
        checkGraph(graph);
        checkCoordinates(graph);
    }

    @Test
    public void test() {
        test(voltageLevel);
    }

    @Test
    public void testWithInternalConnections() {
        test(voltageLevelWithInternalConnections);
    }

}
