/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.commons.PowsyblException;
import com.powsybl.substationdiagram.model.Cell;
import com.powsybl.substationdiagram.model.Coord;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.Side;

import java.util.List;
import java.util.Map;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public interface SubstationLayout {

    /**
     * Calculate real coordinate of voltageLevels in the substation graph
     */
    Coord calculateCoordVoltageLevel(LayoutParameters layoutParam, Graph vlGraph);

    /*
     * Calculate polyline points of a snakeLine in the substation graph
     */
    List<Double> calculatePolylineSnakeLine(LayoutParameters layoutParam,
                                            Edge edge,
                                            Map<Cell.Direction, Integer> nbSnakeLinesTopBottom,
                                            Map<Side, Integer> nbSnakeLinesLeftRight,
                                            Map<String, Integer> nbSnakeLinesBetween,
                                            Map<String, Integer> nbSnakeLinesBottomVL,
                                            Map<String, Integer> nbSnakeLinesTopVL);

    default void checkNodes(Node node1, Node node2) {
        if (node1.getType() != Node.NodeType.FEEDER) {
            throw new PowsyblException("Node 1 is not a feeder node");
        }
        if (node2.getType() != Node.NodeType.FEEDER) {
            throw new PowsyblException("Node 2 is not a feeder node");
        }

        Cell.Direction dNode1 = node1.getCell() != null ? node1.getCell().getDirection() : Cell.Direction.TOP;
        Cell.Direction dNode2 = node2.getCell() != null ? node2.getCell().getDirection() : Cell.Direction.TOP;

        if (dNode1 != Cell.Direction.TOP && dNode1 != Cell.Direction.BOTTOM) {
            throw new PowsyblException("Node 1 cell direction not TOP or BOTTOM");
        }
        if (dNode2 != Cell.Direction.TOP && dNode2 != Cell.Direction.BOTTOM) {
            throw new PowsyblException("Node 2 cell direction not TOP or BOTTOM");
        }
    }
}
