/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.commons.PowsyblException;
import com.powsybl.substationdiagram.model.*;

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
                                            Map<BusCell.Direction, Integer> nbSnakeLinesTopBottom,
                                            Map<Side, Integer> nbSnakeLinesLeftRight,
                                            Map<String, Integer> nbSnakeLinesBetween,
                                            Map<String, Integer> nbSnakeLinesBottomVL,
                                            Map<String, Integer> nbSnakeLinesTopVL);

    default BusCell.Direction getNodeDirection(Node node, int nb) {
        if (node.getType() != Node.NodeType.FEEDER) {
            throw new PowsyblException("Node " + nb + " is not a feeder node");
        }
        BusCell.Direction dNode = node.getCell() != null ? ((ExternCell) node.getCell()).getDirection() : BusCell.Direction.TOP;
        if (dNode != BusCell.Direction.TOP && dNode != BusCell.Direction.BOTTOM) {
            throw new PowsyblException("Node " + nb + " cell direction not TOP or BOTTOM");
        }
        return dNode;
    }
}
