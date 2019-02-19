/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import com.powsybl.substationdiagram.library.AnchorPoint;
import com.powsybl.substationdiagram.library.AnchorPointProvider;
import com.powsybl.substationdiagram.model.BaseNode;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WireConnection {

    private AnchorPoint anchorPoint1;

    private AnchorPoint anchorPoint2;

    WireConnection(AnchorPoint anchorPoint1, AnchorPoint anchorPoint2) {
        this.anchorPoint1 = Objects.requireNonNull(anchorPoint1);
        this.anchorPoint2 = Objects.requireNonNull(anchorPoint2);
    }

    /**
     * Calculates the distance between two points.
     *
     * @param x1 x1
     * @param y1 y1
     * @param x2 x2
     * @param y2 y2
     * @return distance
     */
    private static double calculateDistancePoint(double x1, double y1, double x2, double y2) {
        return (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
    }

    private static List<AnchorPoint> getAnchorPoints(AnchorPointProvider anchorPointProvider, BaseNode node) {
        return anchorPointProvider.getAnchorPoints(node.getComponentType(), node.getId())
                .stream()
                .map(anchorPoint -> node.isRotated() ? anchorPoint.rotate() : anchorPoint)
                .collect(Collectors.toList());
    }

    public static WireConnection searchBetterAnchorPoints(AnchorPointProvider anchorPointProvider,
                                                          BaseNode node1,
                                                          BaseNode node2) {
        Objects.requireNonNull(anchorPointProvider);
        Objects.requireNonNull(node1);
        Objects.requireNonNull(node2);

        List<AnchorPoint> anchorPoints1 = getAnchorPoints(anchorPointProvider, node1);
        List<AnchorPoint> anchorPoints2 = getAnchorPoints(anchorPointProvider, node2);
        AnchorPoint firstAnchorPoint1 = anchorPoints1.get(0);
        AnchorPoint firstAnchorPoint2 = anchorPoints2.get(0);

        double currentDistance = calculateDistancePoint(node1.getX() + firstAnchorPoint1.getX(),
                                                        node1.getY() + firstAnchorPoint1.getY(),
                                                        node2.getX() + firstAnchorPoint2.getX(),
                                                        node2.getY() + firstAnchorPoint2.getY());

        if (anchorPoints1.size() > 1) { // plusieurs
            // points de
            // connexions
            for (AnchorPoint anchorPoint1 : anchorPoints1) {
                double distance = calculateDistancePoint(node1.getX() + anchorPoint1.getX(),
                                                         node1.getY() + anchorPoint1.getY(),
                                                         node2.getX() + firstAnchorPoint2.getX(),
                                                         node2.getY() + firstAnchorPoint2.getY());
                if (distance < currentDistance) {
                    firstAnchorPoint1 = anchorPoint1;
                    currentDistance = distance;
                }
            }
        }

        if (anchorPoints2.size() > 1) { // plusieurs points de connexions
            for (AnchorPoint anchorPoint2 : anchorPoints2) {
                double distance = calculateDistancePoint(node1.getX() + firstAnchorPoint1.getX(),
                                                         node1.getY() + firstAnchorPoint1.getY(),
                                                         node2.getX() + anchorPoint2.getX(),
                                                         node2.getY() + anchorPoint2.getY());
                if (distance < currentDistance) {
                    firstAnchorPoint2 = anchorPoint2;
                    currentDistance = distance;
                }
            }
        }
        return new WireConnection(firstAnchorPoint1, firstAnchorPoint2);
    }

    public AnchorPoint getAnchorPoint1() {
        return anchorPoint1;
    }

    public AnchorPoint getAnchorPoint2() {
        return anchorPoint2;
    }
}
