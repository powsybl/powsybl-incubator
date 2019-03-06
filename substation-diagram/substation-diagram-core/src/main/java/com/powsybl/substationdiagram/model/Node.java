/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.substationdiagram.library.ComponentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Node implements BaseNode {
    private static double applyAsDouble(Double x) {
        return x;
    }

    public enum NodeType {
        BUS,
        FEEDER,
        FICTITIOUS,
        SWITCH,
        FICTITIOUS_SWITCH,
        SHUNT,
        OTHER
    }

    protected final Graph graph;

    private NodeType type;

    private final String id;

    private final String name;

    private final ComponentType componentType;

    private double x = -1;
    private double y = -1;
    private List<Double> xs = new ArrayList<>();
    private List<Double> ys = new ArrayList<>();

    private boolean xPriority = false;
    private boolean yPriority = false;

    @JsonIgnore
    private Cell cell;

    @JsonIgnore
    private boolean rotated = false;

    @JsonIgnore
    private final List<Edge> adjacentEdges = new ArrayList<>();

    private String label;

    /**
     * Constructor
     */
    protected Node(NodeType type, String id, String name, ComponentType componentType, Graph graph) {
        this.type = Objects.requireNonNull(type);
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.componentType = Objects.requireNonNull(componentType);
        this.graph = Objects.requireNonNull(graph);
    }

    public Cell getCell() {
        return cell;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    @Override
    public ComponentType getComponentType() {
        return componentType;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    @Override
    public String getId() {
        return graph.isUseName() ? name : id;
    }

    public String getLabel() {
        if (label != null) {
            return label;
        } else {
            return getId();
        }
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Node> getAdjacentNodes() {
        return adjacentEdges.stream()
                .map(edge -> edge.getNode1() == Node.this ? edge.getNode2() : edge.getNode1())
                .collect(Collectors.toList());
    }

    public List<Edge> getAdjacentEdges() {
        return adjacentEdges;
    }

    void addAdjacentEdge(Edge e) {
        adjacentEdges.add(e);
    }

    void removeAdjacentEdge(Edge e) {
        adjacentEdges.remove(e);
    }

    public Stream<Node> getListNodeAdjInCell(Cell cell) {
        return getAdjacentNodes().stream().filter(n -> cell.getNodes().contains(n));
    }

    @Override
    public double getX() {
        return x;
    }

    public void setX(double x) {
        setX(x, false);
    }

    public void setX(double x, boolean xPriority) {
        if (!this.xPriority && xPriority) {
            xs.clear();
            this.xPriority = true;
        }
        if (this.xPriority == xPriority) {
            this.x = x;
            xs.add(x);
        }
    }

    @Override
    public double getY() {
        return y;
    }

    public void setY(double y) {
        setY(y, false);
    }

    public void setY(double y, boolean yPriority) {
        if (!this.yPriority && yPriority) {
            ys.clear();
            this.yPriority = true;
        }
        if (this.yPriority == yPriority) {
            this.y = y;
            ys.add(y);
        }
    }

    public NodeType getType() {
        return this.type;
    }

    @Override
    public boolean isRotated() {
        return rotated;
    }

    public void setRotated(boolean rotated) {
        this.rotated = rotated;
    }

    /**
     * Check similarity with another node
     *
     * @param n the node to compare with
     * @return true IF the both are the same OR they are both Busbar OR they are both EQ(but not Busbar);
     * false otherwise
     **/
    public boolean checkNodeSimilarity(Node n) {
        return this.equals(n)
                || ((similarToAFeederNode(this)) && (similarToAFeederNode(n)))
                || ((this instanceof BusNode) && (n instanceof BusNode));
    }

    public boolean similarToAFeederNode(Node n) {
        return (n instanceof FeederNode)
                || (n.getType() == NodeType.FICTITIOUS && n.adjacentEdges.size() == 1);
    }

    @Override
    public String toString() {
        return "Node(id='" + getId() + "', type= " + type + ")";
    }

    public void finalizeCoord() {
        x = xs.stream().mapToDouble(Node::applyAsDouble).average().orElse(0);
        y = ys.stream().mapToDouble(Node::applyAsDouble).average().orElse(0);
    }
}
