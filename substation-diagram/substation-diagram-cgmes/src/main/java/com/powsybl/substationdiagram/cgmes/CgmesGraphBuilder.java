/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.DefaultTopologyVisitor;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.HvdcConverterStation;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.VoltageLevel.NodeBreakerView;
import com.powsybl.substationdiagram.model.BusNode;
import com.powsybl.substationdiagram.model.FeederNode;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.SwitchNode;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesGraphBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(CgmesGraphBuilder.class);

    private final VoltageLevel voltageLevel;
    private Graph graph;
    private final Map<Integer, Node> nodesByNumber = new HashMap<>();
    private final Map<Integer, Node> busbarsByNumber = new HashMap<>();
    private final Map<Integer, Integer> internalConnections = new HashMap<>();

    public CgmesGraphBuilder(VoltageLevel voltageLevel, boolean useName) {
        this.voltageLevel = Objects.requireNonNull(voltageLevel);
        this.graph = new Graph(useName);
    }

    public Graph build() {
        LOG.info("Starting to create graph from VoltageLevel {} name {}", voltageLevel.getId(), voltageLevel.getName());

        NodeBreakerView nodeBreakerView = voltageLevel.getNodeBreakerView();
        // store internal connections
        nodeBreakerView.getInternalConnectionStream().forEach(connection -> {
            internalConnections.put(connection.getNode1(), connection.getNode2());
        });
        // visit voltage level
        voltageLevel.visitEquipments(new BuildGraphTopologyVisitor());
        // add switches
        for (Switch sw : nodeBreakerView.getSwitches()) {
            SwitchNode node = SwitchNode.create(graph, sw);
            int node1 = nodeBreakerView.getNode1(sw.getId());
            int node2 = nodeBreakerView.getNode2(sw.getId());
            graph.addNode(node);
            if (busbarsByNumber.containsKey(node1)) {
                graph.addEdge(node, busbarsByNumber.get(node1));
            } else if (nodesByNumber.containsKey(node1)) {
                graph.addEdge(node, nodesByNumber.get(node1));
            } else {
                nodesByNumber.put(node1, node);
            }
            if (busbarsByNumber.containsKey(node2)) {
                graph.addEdge(node, busbarsByNumber.get(node2));
            } else if (nodesByNumber.containsKey(node2)) {
                graph.addEdge(node, nodesByNumber.get(node2));
            } else {
                nodesByNumber.put(node2, node);
            }
        }
        // follow internal connections of busbars
        Integer[] busbarsNumbers = busbarsByNumber.keySet().toArray(new Integer[busbarsByNumber.size()]);
        for (int initialBusbarNodeNumber : busbarsNumbers) {
            int busbarNodeNumber = initialBusbarNodeNumber;
            while (isNotBusbarNumber(busbarNodeNumber, initialBusbarNodeNumber) // until I reached another busbar
                    && !nodesByNumber.containsKey(busbarNodeNumber) // or I reached another node
                    && internalConnections.containsKey(busbarNodeNumber)) { // and there are internal connection to follow
                int oldNodeNumber = busbarNodeNumber;
                busbarNodeNumber = internalConnections.get(oldNodeNumber);
            }
            if (busbarNodeNumber != initialBusbarNodeNumber) {
                if (busbarsByNumber.containsKey(busbarNodeNumber)) {
                    graph.addEdge(busbarsByNumber.get(initialBusbarNodeNumber), busbarsByNumber.get(busbarNodeNumber));
                } else if (nodesByNumber.containsKey(busbarNodeNumber)) {
                    graph.addEdge(busbarsByNumber.get(initialBusbarNodeNumber), nodesByNumber.get(busbarNodeNumber));
                } else {
                    busbarsByNumber.put(busbarNodeNumber, busbarsByNumber.get(initialBusbarNodeNumber));
                }
            }
        }
        // follow internal connections of other nodes
        Integer[] nodesNumbers = nodesByNumber.keySet().toArray(new Integer[nodesByNumber.size()]);
        for (int initialNodeNumber : nodesNumbers) {
            int nodeNumber = initialNodeNumber;
            while (!busbarsByNumber.containsKey(nodeNumber) // until I reached a busbar
                    && isNotNodeNumber(nodeNumber, initialNodeNumber) // or I reached another node
                    && internalConnections.containsKey(nodeNumber)) { // and there are internal connection to follow
                int oldNodeNumber = nodeNumber;
                nodeNumber = internalConnections.get(oldNodeNumber);
            }
            if (nodeNumber != initialNodeNumber) {
                if (busbarsByNumber.containsKey(nodeNumber)) {
                    graph.addEdge(nodesByNumber.get(initialNodeNumber), busbarsByNumber.get(nodeNumber));
                } else if (nodesByNumber.containsKey(nodeNumber)) {
                    graph.addEdge(nodesByNumber.get(initialNodeNumber), nodesByNumber.get(nodeNumber));
                } else {
                    nodesByNumber.put(nodeNumber, nodesByNumber.get(initialNodeNumber));
                }
            }
        }

        return graph;
    }

    private boolean isNotBusbarNumber(int busbarNodeNumber, int initialBusbarNodeNumber) {
        return busbarNodeNumber == initialBusbarNodeNumber || !busbarsByNumber.containsKey(busbarNodeNumber);
    }

    private boolean isNotNodeNumber(int nodeNumber, int initialNodeNumber) {
        return nodeNumber == initialNodeNumber || !nodesByNumber.containsKey(nodeNumber);
    }

    private class BuildGraphTopologyVisitor extends DefaultTopologyVisitor {

        private void addNode(Node node, Terminal terminal) {
            nodesByNumber.put(terminal.getNodeBreakerView().getNode(), node);
            graph.addNode(node);
        }

        @Override
        public void visitBusbarSection(BusbarSection busbar) {
            Node node = BusNode.create(graph, busbar);
            busbarsByNumber.put(busbar.getTerminal().getNodeBreakerView().getNode(), node);
            graph.addNode(node);
        }

        @Override
        public void visitLoad(Load load) {
            addNode(FeederNode.create(graph, load), load.getTerminal());
        }

        @Override
        public void visitGenerator(Generator generator) {
            addNode(FeederNode.create(graph, generator), generator.getTerminal());
        }

        @Override
        public void visitShuntCompensator(ShuntCompensator shunt) {
            addNode(FeederNode.create(graph, shunt), shunt.getTerminal());
        }

        @Override
        public void visitDanglingLine(DanglingLine danglingLine) {
            addNode(FeederNode.create(graph, danglingLine), danglingLine.getTerminal());
        }

        @Override
        public void visitHvdcConverterStation(HvdcConverterStation<?> converterStation) {
            addNode(FeederNode.create(graph, converterStation), converterStation.getTerminal());
        }

        @Override
        public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
            addNode(FeederNode.create(graph, staticVarCompensator), staticVarCompensator.getTerminal());
        }

        @Override
        public void visitTwoWindingsTransformer(TwoWindingsTransformer transformer,
                                                TwoWindingsTransformer.Side side) {
            addNode(FeederNode.create(graph, transformer, side), transformer.getTerminal(side));
        }

        @Override
        public void visitLine(Line line, Line.Side side) {
            addNode(FeederNode.create(graph, line, side), line.getTerminal(side));
        }

        @Override
        public void visitThreeWindingsTransformer(ThreeWindingsTransformer transformer,
                                                  ThreeWindingsTransformer.Side side) {
            throw new AssertionError("TODO");
        }
    }

}
