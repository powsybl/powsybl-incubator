/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.substationdiagram.library.ComponentType;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
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

    public enum NodeType {
        BUS,
        FEEDER,
        FICTITIOUS,
        SWITCH,
        SHUNT,
        OTHER
    }

    protected final Graph graph;

    private NodeType type;

    private final String id;

    private final String name;

    private final ComponentType componentType;

    private final boolean fictitious;

    private double x = -1;
    private double y = -1;
    private List<Double> xs = new ArrayList<>();
    private List<Double> ys = new ArrayList<>();

    private boolean xPriority = false;
    private boolean yPriority = false;

    private AbstractCell cell;

    private boolean rotated = false;

    private boolean open = false;

    private final List<Edge> adjacentEdges = new ArrayList<>();

    private String label;

    /**
     * Constructor
     */
    protected Node(NodeType type, String id, String name, ComponentType componentType, boolean fictitious, Graph graph) {
        this.type = Objects.requireNonNull(type);
        this.name = name;
        this.componentType = Objects.requireNonNull(componentType);
        this.fictitious = fictitious;
        this.graph = Objects.requireNonNull(graph);
        // for unicity purpose (in substation diagram), we prefix the id of the fictitious node with the voltageLevel id and "_"
        String tmpId = Objects.requireNonNull(id);
        if (type == NodeType.FICTITIOUS && !StringUtils.startsWith(tmpId, "FICT_" + this.graph.getVoltageLevel().getId() + "_")) {
            this.id = "FICT_" + this.graph.getVoltageLevel().getId() + "_" + tmpId;
        } else {
            this.id = tmpId;
        }
    }

    public AbstractCell getCell() {
        return cell;
    }

    public void setCell(AbstractCell cell) {
        this.cell = cell;
    }

    public Graph getGraph() {
        return graph;
    }

    @Override
    public ComponentType getComponentType() {
        return componentType;
    }

    public boolean isFictitious() {
        return fictitious;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        if (label != null) {
            return label;
        } else {
            return graph.isUseName() ? name : id;
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

    public Stream<Node> getListNodeAdjInCell(AbstractCell cell) {
        return getAdjacentNodes().stream().filter(n -> cell.getNodes().contains(n));
    }

    @Override
    public double getX() {
        return x;
    }

    public void setX(double x) {
        setX(x, false, true);
    }

    public void setX(double x, boolean xPriority) {
        setX(x, xPriority, true);
    }

    public void setX(double x, boolean xPriority, boolean addXGraph) {
        double xNode = x;
        if (addXGraph) {
            xNode += graph.getX();
        }

        if (!this.xPriority && xPriority) {
            xs.clear();
            this.xPriority = true;
        }
        if (this.xPriority == xPriority) {
            this.x = xNode;
            xs.add(xNode);
        }
    }

    @Override
    public double getY() {
        return y;
    }

    public void setY(double y) {
        setY(y, false, true);
    }

    public void setY(double y, boolean yPriority) {
        setY(y, yPriority, true);
    }

    public void setY(double y, boolean yPriority, boolean addYGraph) {
        double yNode = y;
        if (addYGraph) {
            yNode += graph.getY();
        }

        if (!this.yPriority && yPriority) {
            ys.clear();
            this.yPriority = true;
        }
        if (this.yPriority == yPriority) {
            this.y = yNode;
            ys.add(yNode);
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

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
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

    public void finalizeCoord() {
        x = xs.stream().mapToDouble(d -> d).average().orElse(0);
        y = ys.stream().mapToDouble(d -> d).average().orElse(0);
    }

    public void writeJson(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("type", type.name());
        generator.writeStringField("id", id);
        if (name != null) {
            generator.writeStringField("name", name);
        }
        generator.writeEndObject();
    }

    @Override
    public String toString() {
        return "Node(id='" + getId() + "' name='" + name + "', type= " + type + ")";
    }
}
