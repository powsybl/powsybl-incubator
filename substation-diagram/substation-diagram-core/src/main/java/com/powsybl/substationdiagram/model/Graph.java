/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.powsybl.iidm.network.*;
import com.rte_france.powsybl.iidm.network.extensions.cvg.BusbarSectionPosition;
import com.rte_france.powsybl.iidm.network.extensions.cvg.ConnectablePosition;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.Pseudograph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class builds the connectivity among the elements of a voltageLevel
 * buildGraphAndDetectCell establishes the List of nodes, edges and nodeBuses
 * cells is built by the PatternCellDetector Class
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Graph {

    @JsonIgnore
    private static final Logger LOGGER = LoggerFactory.getLogger(Graph.class);

    @JsonIgnore
    private final boolean useName;

    @JsonIgnore
    private final List<Node> nodes = new ArrayList<>();

    @JsonIgnore
    private final List<Edge> edges = new ArrayList<>();

    @JsonManagedReference
    private final SortedSet<Cell> cells = new TreeSet<>(
            Comparator.comparingInt(Cell::getNumber)); // cells sorted to avoid randomness

    @JsonIgnore
    private final Map<Node.NodeType, List<Node>> nodesByType = new EnumMap<>(Node.NodeType.class);

    private final Map<String, Node> nodesById = new HashMap<>();

    @JsonIgnore
    private Position maxBusStructuralPosition = new Position(0, 0);

    @JsonIgnore
    private Map<Integer, Map<Integer, BusNode>> vPosToHPosToNodeBus;

    @JsonIgnore
    private int cellCounter = 0;

    /**
     * Constructor
     */
    public Graph(boolean useName) {
        this.useName = useName;
    }

    boolean isUseName() {
        return useName;
    }

    public static Graph create(VoltageLevel vl) {
        return create(vl, false);
    }

    public static Graph create(VoltageLevel vl, boolean useName) {
        Objects.requireNonNull(vl);
        Graph g = new Graph(useName);
        g.buildGraph(vl);
        return g;
    }

    public static Map<String, Graph> create(Network network) {
        return create(network, false);
    }

    public static Map<String, Graph> create(Network network, boolean useName) {
        Map<String, Graph> graphs = new HashMap<>();
        for (VoltageLevel vl : network.getVoltageLevels()) {
            graphs.put(vl.getId(), create(vl, useName));
        }
        return graphs;
    }

    int getNextCellIndex() {
        return cellCounter++;
    }

    private abstract class AbstractGraphBuilder extends DefaultTopologyVisitor {

        protected abstract void addFeeder(FeederNode node, Terminal terminal);

        @Override
        public void visitLoad(Load load) {
            addFeeder(FeederNode.create(Graph.this, load), load.getTerminal());
        }

        @Override
        public void visitGenerator(Generator generator) {
            addFeeder(FeederNode.create(Graph.this, generator), generator.getTerminal());
        }

        @Override
        public void visitShuntCompensator(ShuntCompensator sc) {
            addFeeder(FeederNode.create(Graph.this, sc), sc.getTerminal());
        }

        @Override
        public void visitDanglingLine(DanglingLine danglingLine) {
            addFeeder(FeederNode.create(Graph.this, danglingLine), danglingLine.getTerminal());
        }

        @Override
        public void visitHvdcConverterStation(HvdcConverterStation<?> converterStation) {
            addFeeder(FeederNode.create(Graph.this, converterStation), converterStation.getTerminal());
        }

        @Override
        public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
            addFeeder(FeederNode.create(Graph.this, staticVarCompensator), staticVarCompensator.getTerminal());
        }

        @Override
        public void visitTwoWindingsTransformer(TwoWindingsTransformer transformer,
                                                TwoWindingsTransformer.Side side) {
            addFeeder(FeederNode.create(Graph.this, transformer, side), transformer.getTerminal(side));
        }

        @Override
        public void visitLine(Line line, Line.Side side) {
            addFeeder(FeederNode.create(Graph.this, line, side), line.getTerminal(side));
        }

        @Override
        public void visitThreeWindingsTransformer(ThreeWindingsTransformer transformer,
                                                  ThreeWindingsTransformer.Side side) {
            throw new AssertionError("TODO");
        }
    }

    private class NodeBreakerGraphBuilder extends AbstractGraphBuilder {

        private final Map<Integer, Node> nodesByNumber;

        NodeBreakerGraphBuilder(Map<Integer, Node> nodesByNumber) {
            this.nodesByNumber = Objects.requireNonNull(nodesByNumber);
        }

        public ConnectablePosition.Feeder getFeeder(Terminal terminal) {
            Connectable connectable = terminal.getConnectable();
            ConnectablePosition position = (ConnectablePosition) connectable.getExtension(ConnectablePosition.class);
            if (position == null) {
                return null;
            }
            if (connectable instanceof Injection) {
                return position.getFeeder();
            } else if (connectable instanceof Branch) {
                Branch branch = (Branch) connectable;
                if (branch.getTerminal1() == terminal) {
                    return position.getFeeder1();
                } else if (branch.getTerminal2() == terminal) {
                    return position.getFeeder2();
                } else {
                    throw new AssertionError();
                }
            } else if (connectable instanceof ThreeWindingsTransformer) {
                ThreeWindingsTransformer twt = (ThreeWindingsTransformer) connectable;
                if (twt.getLeg1().getTerminal() == terminal) {
                    return position.getFeeder1();
                } else if (twt.getLeg2().getTerminal() == terminal) {
                    return position.getFeeder2();
                } else if (twt.getLeg3().getTerminal() == terminal) {
                    return position.getFeeder3();
                } else {
                    throw new AssertionError();
                }
            } else {
                throw new AssertionError();
            }
        }

        protected void addFeeder(FeederNode node, Terminal terminal) {
            ConnectablePosition.Feeder feeder = getFeeder(terminal);
            if (feeder != null) {
                node.setOrder(feeder.getOrder());
                node.setLabel(feeder.getName());
                node.setDirection(Cell.Direction.valueOf(feeder.getDirection().toString()));
            }
            nodesByNumber.put(terminal.getNodeBreakerView().getNode(), node);
            addNode(node);
        }

        @Override
        public void visitBusbarSection(BusbarSection busbarSection) {
            BusbarSectionPosition extension = busbarSection.getExtension(BusbarSectionPosition.class);
            BusNode node = BusNode.create(Graph.this, busbarSection);
            if (extension != null) {
                node.setStructuralPosition(new Position(extension.getSectionIndex(), extension.getBusbarIndex())
                        .setHSpan(1));
            }
            nodesByNumber.put(busbarSection.getTerminal().getNodeBreakerView().getNode(), node);
            addNode(node);
        }
    }

    private class BusBreakerGraphBuilder extends AbstractGraphBuilder {

        private final Map<String, Node> nodesByBusId;

        private int order = 1;

        BusBreakerGraphBuilder(Map<String, Node> nodesByBusId) {
            this.nodesByBusId = Objects.requireNonNull(nodesByBusId);
        }

        protected void addFeeder(FeederNode node, Terminal terminal) {
            node.setOrder(order++);
            node.setDirection(order % 2 == 0 ? Cell.Direction.TOP : Cell.Direction.BOTTOM);
            addNode(node);
            SwitchNode nodeSwitch = SwitchNode.create(Graph.this, terminal);
            addNode(nodeSwitch);
            String busId = terminal.getBusBreakerView().getConnectableBus().getId();
            addEdge(nodesByBusId.get(busId), nodeSwitch);
            addEdge(nodeSwitch, node);
        }
    }

    private void buildBusBreakerGraph(VoltageLevel vl) {
        Map<String, Node> nodesByBusId = new HashMap<>();

        int v = 1;
        for (Bus b : vl.getBusBreakerView().getBuses()) {
            BusNode busNode = BusNode.create(this, b);
            nodesByBusId.put(b.getId(), busNode);
            busNode.setStructuralPosition(new Position(1, v++));
            addNode(busNode);
        }

        // visit equipments
        vl.visitEquipments(new BusBreakerGraphBuilder(nodesByBusId));
    }

    private void buildNodeBreakerGraph(VoltageLevel vl) {
        Map<Integer, Node> nodesByNumber = new HashMap<>();

        // visit equipments
        vl.visitEquipments(new NodeBreakerGraphBuilder(nodesByNumber));

        // switches
        for (Switch sw : vl.getNodeBreakerView().getSwitches()) {
            SwitchNode n = SwitchNode.create(Graph.this, sw);

            int node1 = vl.getNodeBreakerView().getNode1(sw.getId());
            int node2 = vl.getNodeBreakerView().getNode2(sw.getId());

            ensureNodeExists(node1, nodesByNumber);
            ensureNodeExists(node2, nodesByNumber);

            addEdge(nodesByNumber.get(node1), n);
            addEdge(n, nodesByNumber.get(node2));
            addNode(n);
        }

        // internal connections
        vl.getNodeBreakerView().getInternalConnectionStream().forEach(internalConnection -> {
            int node1 = internalConnection.getNode1();
            int node2 = internalConnection.getNode2();

            ensureNodeExists(node1, nodesByNumber);
            ensureNodeExists(node2, nodesByNumber);

            addEdge(nodesByNumber.get(node1), nodesByNumber.get(node2));
        });
    }

    private void buildGraph(VoltageLevel vl) {
        LOGGER.info("Building '{}' graph...", vl.getId());

        switch (vl.getTopologyKind()) {
            case BUS_BREAKER:
                buildBusBreakerGraph(vl);
                break;
            case NODE_BREAKER:
                buildNodeBreakerGraph(vl);
                break;
            default:
                throw new AssertionError("Unknown topology kind: " + vl.getTopologyKind());
        }

        LOGGER.info("Number of node : {} ", nodes.size());

        boolean connected = checkConnectedGraph();
        if (!connected) {
            LOGGER.warn("The graph is not connected!");
        }
    }

    private void removeUnnecessaryFictitiousNodes() {
        List<Node> fictitiousNodesToRemove = nodes.stream()
                .filter(node -> node.getType() == Node.NodeType.FICTITIOUS)
                .filter(node -> node.getAdjacentNodes().size() == 2)
                .collect(Collectors.toList());
        for (Node n : fictitiousNodesToRemove) {
            Node node1 = n.getAdjacentNodes().get(0);
            Node node2 = n.getAdjacentNodes().get(1);
            LOGGER.info("Remove unnecessary node between {} and {}", node1.getId(), node2.getId());
            removeNode(n);
            addEdge(node1, node2);
        }
    }

    private void ensureNodeExists(int n, Map<Integer, Node> nodesByNumber) {
        if (!nodesByNumber.containsKey(n)) {
            FicticiousNode node = new FicticiousNode(Graph.this, "" + n);
            nodesByNumber.put(n, node);
            addNode(node);
        }
    }

    public void logCellDetectionStatus() {
        Set<Cell> cells = new HashSet<>();
        Map<Cell.CellType, Integer> cellCountByType = new EnumMap<>(Cell.CellType.class);
        for (Cell.CellType cellType : Cell.CellType.values()) {
            cellCountByType.put(cellType, 0);
        }
        int remainingNodeCount = 0;
        Map<Node.NodeType, Integer> remainingNodeCountByType = new EnumMap<>(Node.NodeType.class);
        for (Node.NodeType nodeType : Node.NodeType.values()) {
            remainingNodeCountByType.put(nodeType, 0);
        }
        for (Node node : nodes) {
            Cell cell = node.getCell();
            if (cell != null) {
                if (cells.add(cell)) {
                    cellCountByType.put(cell.getType(), cellCountByType.get(cell.getType()) + 1);
                }
            } else {
                remainingNodeCount++;
                remainingNodeCountByType.put(node.getType(), remainingNodeCountByType.get(node.getType()) + 1);
            }
        }
        if (cells.isEmpty()) {
            LOGGER.warn("No cell detected");
        } else {
            LOGGER.info("{} cells detected ({})", cells.size(), cellCountByType);
        }
        if (remainingNodeCount > 0) {
            LOGGER.warn("{}/{} nodes not associated to a cell ({})",
                        remainingNodeCount, nodes.size(), remainingNodeCountByType);
        }
    }

    public void whenSerializingUsingJsonAnyGetterThenCorrect(Writer writer) {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        try {
            mapper.writeValue(writer, this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private UndirectedGraph<Node, Edge> toJgrapht() {
        UndirectedGraph<Node, Edge> graph = new Pseudograph<>(Edge.class);
        for (Node node : nodes) {
            graph.addVertex(node);
        }
        for (Edge edge : edges) {
            graph.addEdge(edge.getNode1(), edge.getNode2(), edge);
        }
        return graph;
    }

    /**
     * Check if the graph is connected or not
     *
     * @return true if connected, false otherwise
     */
    private boolean checkConnectedGraph() {
        List<Set<Node>> connectedSets = new ConnectivityInspector<>(toJgrapht()).connectedSets();
        if (connectedSets.size() != 1) {
            LOGGER.warn("{} connected components found", connectedSets.size());
            connectedSets.stream()
                    .sorted(Comparator.comparingInt(Set::size))
                    .map(nodes -> nodes.stream().map(Node::getId).collect(Collectors.toSet()))
                    .forEach(strings -> LOGGER.warn("   - {}", strings));
        }
        return connectedSets.size() == 1;
    }

    public void addNode(Node node) {
        nodes.add(node);
        nodesByType.computeIfAbsent(node.getType(), nodeType -> new ArrayList<>()).add(node);
        nodesById.put(node.getId(), node);
    }

    private void removeNode(Node node) {
        nodes.remove(node);
        nodesByType.computeIfAbsent(node.getType(), nodeType -> new ArrayList<>()).remove(node);
        nodesById.remove(node.getId());
        for (Edge edge : node.getAdjacentEdges()) {
            if (edge.getNode1() == node) {
                edge.getNode2().removeAdjacentEdge(edge);
            } else {
                edge.getNode1().removeAdjacentEdge(edge);
            }
            edges.remove(edge);
        }
    }

    public Node getNode(String id) {
        Objects.requireNonNull(id);
        return nodesById.get(id);
    }

    /**
     * Add an edge between the two nodes
     *
     * @param n1 first node
     * @param n2 second node
     */
    public void addEdge(Node n1, Node n2) {
        Edge edge = new Edge(n1, n2);
        edges.add(edge);
        n1.addAdjacentEdge(edge);
        n2.addAdjacentEdge(edge);
    }

    /**
     * Remove an edge between two nodes
     *
     * @param n1 first node
     * @param n2 second node
     */
    void removeEdge(Node n1, Node n2) {
        for (Edge edge : edges) {
            if ((edge.getNode1().equals(n1) && edge.getNode2().equals(n2))
                    || (edge.getNode1().equals(n2) && edge.getNode2().equals(n1))) {
                n1.removeAdjacentEdge(edge);
                n2.removeAdjacentEdge(edge);
                edges.remove(edge);
                return;
            }
        }
    }

    /**
     * Resolve when one EQ is connected with 2 switchs component
     */

    private void rIdentifyConnexComponent(Node node, List<Node> nodesIn, List<Node> connexComponent) {
        if (!connexComponent.contains(node)) {
            connexComponent.add(node);
            List<Node> nodesToVisit = node.getAdjacentNodes()
                    .stream()
                    .filter(nodesIn::contains)
                    .collect(Collectors.toList());
            for (Node n : nodesToVisit) {
                rIdentifyConnexComponent(n, nodesIn, connexComponent);
            }
        }
    }

    public List<List<Node>> getConnexComponents(List<Node> nodesIn) {
        List<Node> nodesToHandle = new ArrayList<>(nodesIn);
        List<List<Node>> result = new ArrayList<>();
        while (!nodesToHandle.isEmpty()) {
            Node n = nodesToHandle.get(0);
            List<Node> connexComponent = new ArrayList<>();
            rIdentifyConnexComponent(n, nodesIn, connexComponent);
            nodesToHandle.removeAll(connexComponent);
            result.add(connexComponent);
        }
        return result;
    }

    public List<String> signatureSortedCellsContent() {
        return cells.stream().map(Cell::getFullId).sorted().collect(Collectors.toList());
    }

    public boolean compareCellDetection(Graph graph) {
        return signatureSortedCellsContent().equals(graph.signatureSortedCellsContent());
    }

    public Position getMaxBusStructuralPosition() {
        return maxBusStructuralPosition;
    }

    public void setMaxBusPosition() {
        List<Integer> h = new ArrayList<>();
        List<Integer> v = new ArrayList<>();
        getNodeBuses().forEach(nodeBus -> {
            v.add(nodeBus.getStructuralPosition().getV());
            h.add(nodeBus.getStructuralPosition().getH());
        });
        maxBusStructuralPosition.setH(Collections.max(h));
        maxBusStructuralPosition.setV(Collections.max(v));
    }

    public Stream<Cell> getBusCells() {
        return cells.stream().filter(cell -> !cell.getPrimaryBlocksConnectedToBus().isEmpty());
    }

    private void buildVPosToHposToNodeBus() {
        vPosToHPosToNodeBus = new HashMap<>();
        getNodeBuses()
                .forEach(nodeBus -> {
                    int vPos = nodeBus.getStructuralPosition().getV();
                    int hPos = nodeBus.getStructuralPosition().getH();
                    vPosToHPosToNodeBus.putIfAbsent(vPos, new HashMap<>());
                    vPosToHPosToNodeBus.get(vPos).put(hPos, nodeBus);
                });
    }

    public void extendFeederWithMultipleSwitches() {
        List<Node> nodesToAdd = new ArrayList<>();
        for (Node n : nodes) {
            if (n instanceof FeederNode && n.getAdjacentNodes().size() > 1) {
                // Create a new fictitious node
                FicticiousNode nf = new FicticiousNode(Graph.this, n.getId() + "Fictif");
                nodesToAdd.add(nf);
                // Create all new edges and remove old ones
                List<Node> oldNeighboor = new ArrayList<>(n.getAdjacentNodes());
                for (Node neighboor : oldNeighboor) {
                    addEdge(nf, neighboor);
                    removeEdge(n, neighboor);
                }
                addEdge(n, nf);
            }
        }
        nodes.addAll(nodesToAdd);
    }

    public void extendFirstOutsideNode() {
        getNodeBuses().stream()
                .flatMap(node -> node.getAdjacentNodes().stream())
                .filter(node -> node.getType() == Node.NodeType.SWITCH)
                .forEach(nodeSwitch -> {
                    nodeSwitch.getAdjacentNodes().stream()
                            .filter(node -> node.getType() == Node.NodeType.SWITCH)
                            .forEach(node -> {
                                removeEdge(node, nodeSwitch);
                                FicticiousNode newNode = new FicticiousNode(Graph.this, nodeSwitch.getId() + "Fictif");
                                addNode(newNode);
                                addEdge(node, newNode);
                                addEdge(nodeSwitch, newNode);
                            });
                });
    }

    public void extendBreakerConnectedToBus() {
        getNodeBuses().forEach(nodeBus -> nodeBus.getAdjacentNodes().stream()
                .filter(node -> node.getType() == Node.NodeType.SWITCH
                        && ((SwitchNode) node).getKind() != SwitchKind.DISCONNECTOR)
                .forEach(nodeSwitch -> addDoubleNode(nodeBus, (SwitchNode) nodeSwitch, "")));
    }

    public void extendSwitchBetweenBus(SwitchNode nodeSwitch) {
        List<Node> copyAdj = new ArrayList<>(nodeSwitch.getAdjacentNodes());
        addDoubleNode((BusNode) copyAdj.get(0), nodeSwitch, "0");
        addDoubleNode((BusNode) copyAdj.get(1), nodeSwitch, "1");
    }

    private void addDoubleNode(BusNode busNode, SwitchNode nodeSwitch, String suffix) {
        removeEdge(busNode, nodeSwitch);
        FicticiousNode fNodeToBus = new FicticiousNode(Graph.this, nodeSwitch.getId() + "fSwitch" + suffix,
                                                       true);
        addNode(fNodeToBus);
        FicticiousNode fNodeToSw = new FicticiousNode(Graph.this, nodeSwitch.getId() + "fNode" + suffix);
        addNode(fNodeToSw);
        addEdge(busNode, fNodeToBus);
        addEdge(fNodeToBus, fNodeToSw);
        addEdge(fNodeToSw, nodeSwitch);
    }


    public BusNode getVHNodeBus(int v, int h) {
        if (vPosToHPosToNodeBus == null) {
            buildVPosToHposToNodeBus();
        }
        if (!vPosToHPosToNodeBus.containsKey(v)) {
            return null;
        }
        if (!vPosToHPosToNodeBus.get(v).containsKey(h)) {
            return null;
        }
        return vPosToHPosToNodeBus.get(v).get(h);
    }

    public void addCell(Cell c) {
        cells.add(c);
    }

    public void removeCell(Cell c) {
        cells.remove(c);
    }

    public List<BusNode> getNodeBuses() {
        return nodesByType.computeIfAbsent(Node.NodeType.BUS, nodeType -> new ArrayList<>())
                .stream()
                .map(BusNode.class::cast)
                .collect(Collectors.toList());
    }

    public List<Node> getNodes() {
        return new ArrayList<>(nodes);
    }

    public List<Edge> getEdges() {
        return new ArrayList<>(edges);
    }

    public Set<Cell> getCells() {
        return new TreeSet<>(cells);
    }
}
