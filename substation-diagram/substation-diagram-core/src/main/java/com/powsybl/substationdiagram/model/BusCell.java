/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BusCell extends Cell {

    public enum Direction {
        TOP, BOTTOM, FLAT, UNDEFINED
    }

    private Direction direction = Direction.UNDEFINED;

    private List<PrimaryBlock> primaryBlocksConnectedToBus = new ArrayList<>();

    protected BusCell(Graph graph, CellType type) {
        super(graph, type);
    }

    public List<BusNode> getBusNodes() {
        return nodes.stream()
                .filter(n -> n.getType() == Node.NodeType.BUS)
                .map(BusNode.class::cast)
                .collect(Collectors.toList());
    }

    public void blocksSetting(Block rootBlock, List<PrimaryBlock> primaryBlocksConnectedToBus) {
        setRootBlock(rootBlock);
        this.primaryBlocksConnectedToBus = new ArrayList<>(primaryBlocksConnectedToBus);
    }

    public List<PrimaryBlock> getPrimaryBlocksConnectedToBus() {
        return new ArrayList<>(primaryBlocksConnectedToBus);
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Position getMaxBusPosition() {
        return graph.getMaxBusStructuralPosition();
    }

    public Position getRootPosition() {
        return getRootBlock().getPosition();
    }

    @Override
    public String toString() {
        return "Cell(type=" + getType() + ", direction=" + direction + ", nodes=" + nodes + ")";
    }

}
