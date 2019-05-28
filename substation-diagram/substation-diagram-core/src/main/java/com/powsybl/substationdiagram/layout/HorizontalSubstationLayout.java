/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.substationdiagram.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class HorizontalSubstationLayout implements SubstationLayout {

    public HorizontalSubstationLayout(SubstationGraph graph) {
    }

    /**
     * Calculate relative coordinate of voltageLevel in the substation
     */
    @Override
    public Coord calculateCoordVoltageLevel(LayoutParameters layoutParam, Graph vlGraph) {
        int maxH = vlGraph.getNodeBuses().stream()
                .mapToInt(nodeBus -> nodeBus.getPosition().getH() + nodeBus.getPosition().getHSpan())
                .max().orElse(0);

        double x = layoutParam.getInitialXBus() + (maxH + 2) * layoutParam.getCellWidth();
        double y = 0;
        return new Coord(x, y);
    }

    /*
     * Calculate polyline points of a snakeLine in the substation graph
     */
    public List<Double> calculatePolylineSnakeLine(LayoutParameters layoutParam,
                                                   Edge edge,
                                                   Map<AbstractBusCell.Direction, Integer> nbSnakeLinesTopBottom,
                                                   Map<Side, Integer> nbSnakeLinesLeftRight,
                                                   Map<String, Integer> nbSnakeLinesBetween,
                                                   Map<String, Integer> nbSnakeLinesBottomVL,
                                                   Map<String, Integer> nbSnakeLinesTopVL) {
        Node node1 = edge.getNode1();
        Node node2 = edge.getNode2();

        AbstractBusCell.Direction dNode1 = getNodeDirection(node1, 1);
        AbstractBusCell.Direction dNode2 = getNodeDirection(node2, 2);

        double xMaxGraph;
        String idMaxGraph;

        if (node1.getGraph().getX() > node2.getGraph().getX()) {
            xMaxGraph = node1.getGraph().getX();
            idMaxGraph = node1.getGraph().getVoltageLevel().getId();
        } else {
            xMaxGraph = node2.getGraph().getX();
            idMaxGraph = node2.getGraph().getVoltageLevel().getId();
        }

        double x1 = node1.getX();
        double y1 = node1.getY();
        double x2 = node2.getX();
        double y2 = node2.getY();

        List<Double> pol = new ArrayList<>();
        switch (dNode1) {
            case BOTTOM:
                if (dNode2 == AbstractBusCell.Direction.BOTTOM) {  // BOTTOM to BOTTOM
                    nbSnakeLinesTopBottom.compute(dNode1, (k, v) -> v + 1);
                    double decalV = nbSnakeLinesTopBottom.get(dNode1) * layoutParam.getVerticalSnakeLinePadding();
                    double yDecal = Math.max(y1 + decalV, y2 + decalV);

                    pol.addAll(Arrays.asList(x1, y1,
                            x1, yDecal,
                            x2, yDecal,
                            x2, y2));

                } else {  // BOTTOM to TOP
                    nbSnakeLinesTopBottom.compute(dNode1, (k, v) -> v + 1);
                    nbSnakeLinesTopBottom.compute(dNode2, (k, v) -> v + 1);
                    nbSnakeLinesBetween.compute(idMaxGraph, (k, v) -> v + 1);
                    double decal1V = nbSnakeLinesTopBottom.get(dNode1) * layoutParam.getVerticalSnakeLinePadding();
                    double decal2V = nbSnakeLinesTopBottom.get(dNode2) * layoutParam.getVerticalSnakeLinePadding();
                    double xBetweenGraph = xMaxGraph - (nbSnakeLinesBetween.get(idMaxGraph) * layoutParam.getHorizontalSnakeLinePadding());

                    pol.addAll(Arrays.asList(x1, y1,
                            x1, y1 + decal1V,
                            xBetweenGraph, y1 + decal1V,
                            xBetweenGraph, y2 - decal2V,
                            x2, y2 - decal2V,
                            x2, y2));
                }
                break;

            case TOP:
                if (dNode2 == AbstractBusCell.Direction.TOP) {  // TOP to TOP
                    nbSnakeLinesTopBottom.compute(dNode1, (k, v) -> v + 1);
                    double decalV = nbSnakeLinesTopBottom.get(dNode1) * layoutParam.getVerticalSnakeLinePadding();
                    double yDecal = Math.min(y1 - decalV, y2 - decalV);

                    pol.addAll(Arrays.asList(x1, y1,
                            x1, yDecal,
                            x2, yDecal,
                            x2, y2));
                } else {  // TOP to BOTTOM
                    nbSnakeLinesTopBottom.compute(dNode1, (k, v) -> v + 1);
                    nbSnakeLinesTopBottom.compute(dNode2, (k, v) -> v + 1);
                    nbSnakeLinesBetween.compute(idMaxGraph, (k, v) -> v + 1);
                    double decal1V = nbSnakeLinesTopBottom.get(dNode1) * layoutParam.getVerticalSnakeLinePadding();
                    double decal2V = nbSnakeLinesTopBottom.get(dNode2) * layoutParam.getVerticalSnakeLinePadding();

                    double xBetweenGraph = xMaxGraph - (nbSnakeLinesBetween.get(idMaxGraph) * layoutParam.getHorizontalSnakeLinePadding());

                    pol.addAll(Arrays.asList(x1, y1,
                            x1, y1 - decal1V,
                            xBetweenGraph, y1 - decal1V,
                            xBetweenGraph, y2 + decal2V,
                            x2, y2 + decal2V,
                            x2, y2));
                }
                break;
            default:
        }
        return pol;
    }
}
