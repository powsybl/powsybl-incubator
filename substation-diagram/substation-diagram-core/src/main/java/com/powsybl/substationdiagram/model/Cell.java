/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.core.JsonGenerator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Cell implements Comparable<Cell> {

    public enum Direction {
        TOP, BOTTOM, FLAT, UNDEFINED
    }

    public enum CellType {
        INTERN, INTERNBOUND, EXTERN, SHUNT, UNDEFINED
    }

    final Graph graph;

    private int number;

    private final List<Node> nodes = new ArrayList<>();

    private CellType type;

    private int order = -1;

    private Direction direction = Direction.UNDEFINED;

    private Block rootBlock;

    private List<PrimaryBlock> primaryBlocksConnectedToBus = new ArrayList<>();

    private final List<Cell> cellBridgingWith = new ArrayList<>();

    public Cell(Graph graph) {
        this(graph, CellType.UNDEFINED);
    }

    public Cell(Graph graph, CellType type) {
        this.graph = Objects.requireNonNull(graph);
        this.type = Objects.requireNonNull(type);
        number = graph.getNextCellIndex();
        graph.addCell(this);
    }

    void addNode(Node node) {
        nodes.add(node);
        node.setCell(this);
    }

    public List<Node> getNodes() {
        return new ArrayList<>(nodes);
    }

    public void removeAllNodes(List<Node> nodeToRemove) {
        nodes.removeAll(nodeToRemove);
    }

    public void setNodes(List<Node> nodes) {
        this.nodes.addAll(nodes);
        // the cell of the node of a SHUNT node (which belongs to a SHUNT and an EXTERN cells)
        // is the cell of the EXTERN cell
        if (type != CellType.SHUNT) {
            nodes.forEach(node -> node.setCell(this));
        } else {
            nodes.stream().filter(node -> !node.isShunt()).forEach(node -> node.setCell(this));
        }
    }

    public void setBridgingCellsFromShuntNodes() {
        if (type == CellType.SHUNT) {
            List<Node> shuntNodes = nodes.stream().filter(Node::isShunt).collect(
                    Collectors.toList());
            Cell cell0 = shuntNodes.get(0).getCell();
            Cell cell1 = shuntNodes.get(1).getCell();
            cell0.addCellBridgingWith(cell1);
            cell1.addCellBridgingWith(cell0);
        }
    }

    public void setType(CellType type) {
        this.type = type;
    }

    public CellType getType() {
        return this.type;
    }

    public List<BusNode> getBusNodes() {
        return nodes.stream()
                .filter(n -> n.getType() == Node.NodeType.BUS)
                .map(n -> (BusNode) n)
                .collect(Collectors.toList());
    }

    public void orderFromFeederOrders() {
        int sumOrder = 0;
        int nbFeeder = 0;
        for (FeederNode node : getNodes().stream()
                .filter(node -> node.getType() == Node.NodeType.FEEDER)
                .map(node -> (FeederNode) node).collect(Collectors.toList())) {
            sumOrder += node.getOrder();
            nbFeeder++;
        }
        if (nbFeeder != 0) {
            setOrder(sumOrder / nbFeeder);
        }
    }

    public void blocksSetting(Block rootBlock, List<PrimaryBlock> primaryBlocksConnectedToBus) {
        this.rootBlock = rootBlock;
        this.primaryBlocksConnectedToBus = new ArrayList<>(primaryBlocksConnectedToBus);
    }

    public Block getRootBlock() {
        return rootBlock;
    }

    public List<PrimaryBlock> getPrimaryBlocksConnectedToBus() {
        return new ArrayList<>(primaryBlocksConnectedToBus);
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public List<Cell> getCellBridgingWith() {
        return new ArrayList<>(cellBridgingWith);
    }

    private void addCellBridgingWith(Cell cell) {
        cellBridgingWith.add(cell);
    }

    public int getNumber() {
        return number;
    }

    public Position getMaxBusPosition() {
        return graph.getMaxBusStructuralPosition();
    }

    public Position getRootPosition() {
        return getRootBlock().getPosition();
    }

    public void writeJson(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("type", type.name());
        if (rootBlock != null) {
            generator.writeFieldName("rootBlock");
            rootBlock.writeJson(generator);
        }
        generator.writeEndObject();
    }

    @Override
    public int compareTo(@Nonnull Cell o) {
        if (order == o.order && !this.equals(o)) {
            return number - o.number;
        }
        return Comparator.comparingInt(Cell::getOrder).compare(this, o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public String getFullId() {
        return type + nodes.stream().map(Node::getId).sorted().collect(Collectors.toList()).toString();
    }

    @Override
    public String toString() {
        return "Cell(type=" + type + ", order=" + order + ", direction=" + direction + ", nodes=" + nodes + ")";
    }
}
