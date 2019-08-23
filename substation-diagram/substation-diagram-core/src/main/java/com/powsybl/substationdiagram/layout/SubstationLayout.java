/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import java.util.List;
import java.util.Map;

import com.powsybl.substationdiagram.model.BusCell;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Side;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public interface SubstationLayout {

    /**
     * Calculate real coordinates of nodes in the substation graph
     */
    void run(LayoutParameters layoutParameters);

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

}
