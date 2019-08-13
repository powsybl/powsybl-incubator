/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.commons.PowsyblException;
import com.powsybl.substationdiagram.layout.LayoutParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PrimaryBlock extends AbstractBlock {

    private final List<Node> nodes = new ArrayList<>();

    private final List<PrimaryBlock> stackableBlocks;

    /**
     * Constructor.
     * A layout.block primary is oriented in order to have :
     * <ul>
     * <li>BUS - when in the layout.block - as starting node
     * <li>FEEDER - when in the layout.block - as ending node
     * </ul>
     *
     * @param nodes nodes
     */

    public PrimaryBlock(List<Node> nodes) {
        super(Type.PRIMARY);
        if (nodes.isEmpty()) {
            throw new PowsyblException("Empty node list");
        }
        this.stackableBlocks = new ArrayList<>();
        this.nodes.addAll(nodes);
        //convention of orientation, to be respected
        if (getStartingNode().getType() == Node.NodeType.FEEDER
                || getEndingNode().getType() == Node.NodeType.BUS) {
            reverseBlock();
        }
        if (getStartingNode().getType() == Node.NodeType.BUS) {
            setBusNode((BusNode) getStartingNode());
        }
        setCardinalityStart(1);
        setCardinalityEnd(1);
    }

    public PrimaryBlock(List<Node> nodes, Cell cell) {
        this(nodes);
        setCell(cell);
    }

    @Override
    public Graph getGraph() {
        return nodes.get(0).getGraph();
    }

    @Override
    public boolean isEmbedingNodeType(Node.NodeType type) {
        return nodes.stream().anyMatch(n -> n.getType() == type);
    }

    public List<Node> getNodes() {
        return new ArrayList<>(nodes);
    }

    @Override
    public void reverseBlock() {
        Collections.reverse(nodes);
    }

    @Override
    public Node getStartingNode() {
        return nodes.get(0);
    }

    @Override
    public int getOrder() {
        return getStartingNode().getType() == Node.NodeType.FEEDER ?
                ((FeederNode) getStartingNode()).getOrder() : 0;
    }

    @Override
    public Node getEndingNode() {
        return nodes.get(nodes.size() - 1);
    }

    public void addStackableBlock(PrimaryBlock block) {
        stackableBlocks.add(block);
    }

    public List<PrimaryBlock> getStackableBlocks() {
        return new ArrayList<>(stackableBlocks);
    }

    @Override
    public void calculateDimensionAndInternPos() {
        if (isEmbedingNodeType(Node.NodeType.BUS)
                && (((BusCell) getCell()).getDirection() == BusCell.Direction.FLAT || !getStackableBlocks().isEmpty())) {
            getPosition().setHSpan(0);
            getPosition().setVSpan(0);
            return;
        }
        if (isEmbedingNodeType(Node.NodeType.BUS)) {
            getPosition().setHSpan(1);
            getPosition().setVSpan(0);
            return;
        }
        int nbEdges = nodes.size() - 1;
        if (getPosition().getOrientation() == Orientation.VERTICAL) {
            getPosition().setHSpan(1);
            // in the case of vertical Blocks the x Spanning is a ratio of the nb of edges of the blocks/overall edges
            getPosition().setVSpan(nbEdges);
        } else {
            // in the case of horizontal Blocks having 1 switch/1 position => 1 hPos / 2 edges rounded to the superior int
            getPosition().setHSpan(nbEdges / 2);
            getPosition().setVSpan(1);
        }

    }

    @Override
    public void coordVerticalCase(LayoutParameters layoutParam) {
        if (isEmbedingNodeType(Node.NodeType.BUS)) {
            Node nodeBus = getBusNode();
            Node nodeMiddle = nodes.get(1);
            nodeMiddle.setX(getCoord().getX());
            nodeMiddle.setY(nodeBus.getY(), false, false);
            if (nodes.size() == 3) {
                Node nodeSide = nodeBus == nodes.get(0) ? nodes.get(2) : nodes.get(0);
                nodeSide.setX(getCoord().getX(), true);
                if (getCell().getType() == Cell.CellType.INTERN && ((InternCell) getCell()).getCentralBlock() == null) {
                    nodeSide.setY(layoutParam.getInitialYBus() - layoutParam.getInternCellHeight());
                }
            }
        } else {
            int sign = ((BusCell) getCell()).getDirection() == BusCell.Direction.TOP ? 1 : -1;
            double y0 = getCoord().getY() + sign * getCoord().getYSpan() / 2;
            double yPxStep = calcYPxStep(sign);
            int v = 0;
            for (Node node : nodes) {
                node.setX(getCoord().getX());
                node.setY(y0 - yPxStep * v);
                node.setRotated(false);
                v++;
            }
        }
    }

    @Override
    public void coordHorizontalCase(LayoutParameters layoutParam) {
        if (isEmbedingNodeType(Node.NodeType.BUS)) {
            Node nodeBus = getBusNode();
            Node nodeMiddle = nodes.get(1);
            nodeMiddle.setX(getCoord().getX() + getCoord().getXSpan() / 2);
            nodeMiddle.setY(nodeBus.getY(), false, false);
            if (nodes.size() == 3) {
                Node nodeSide = nodeBus == nodes.get(0) ? nodes.get(2) : nodes.get(0);
                nodeSide.setY(nodeBus.getY(), true, false);
            }
            return;
        }
        double x0 = getCoord().getX() - getCoord().getXSpan() / 2;
        double xPxStep = getCoord().getXSpan() / (nodes.size() - 1);
        int h = 0;
        for (Node node : nodes) {
            node.setY(getCoord().getY());
            node.setX(x0 + xPxStep * h);
            node.setRotated(true);
            h++;
        }
    }

    void coordShuntCase() {
        double x0 = getStartingNode().getX();
        double x1 = getEndingNode().getX();
        double y0 = getStartingNode().getY();
        double y1 = getEndingNode().getY();
        double dx = (x1 - x0) / (nodes.size() - 1);
        double dy = (y1 - y0) / (nodes.size() - 1);
        for (int i = 1; i < nodes.size() - 1; i++) {
            Node node = nodes.get(i);
            node.setX(x0 + i * dx, false, false);
            node.setY(y0 + i * dy, false, false);
            if (dy == 0) {
                node.setRotated(true);
            }
        }
    }

    private double calcYPxStep(int sign) {
        if (getPosition().getVSpan() == 0) {
            return 0;
        }
        return sign * getCoord().getYSpan() / (nodes.size() - 1);
    }

    @Override
    protected void writeJsonContent(JsonGenerator generator) throws IOException {
        generator.writeFieldName("nodes");
        generator.writeStartArray();
        for (Node node : nodes) {
            node.writeJson(generator);
        }
        generator.writeEndArray();
    }

    @Override
    public String toString() {
        return "PrimaryBlock(nodes=" + nodes + ")";
    }
}
