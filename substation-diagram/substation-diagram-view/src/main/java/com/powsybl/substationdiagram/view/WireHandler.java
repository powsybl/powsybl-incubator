/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.powsybl.substationdiagram.library.AnchorOrientation;
import com.powsybl.substationdiagram.library.ComponentSize;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.WireConnection;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Polyline;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WireHandler {

    private final Polyline polyline;

    private final List<Group> arrows = new ArrayList<>();

    private final NodeHandler nodeHandler1;

    private final NodeHandler nodeHandler2;

    private final GraphMetadata metadata;

    public WireHandler(Polyline polyline, NodeHandler nodeHandler1, NodeHandler nodeHandler2, GraphMetadata metadata) {
        this.polyline = Objects.requireNonNull(polyline);
        this.nodeHandler1 = Objects.requireNonNull(nodeHandler1);
        this.nodeHandler2 = Objects.requireNonNull(nodeHandler2);
        this.metadata = Objects.requireNonNull(metadata);
    }

    public Node getPolyline() {
        return this.polyline;
    }

    public NodeHandler getNodeHandler1() {
        return nodeHandler1;
    }

    public NodeHandler getNodeHandler2() {
        return nodeHandler2;
    }

    public void addArrow(Group g) {
        arrows.add(g);
    }

    public void refresh() {

        WireConnection wireConnection = WireConnection.searchBetterAnchorPoints(metadata, nodeHandler1, nodeHandler2);

        // update polyline
        double x1 = nodeHandler1.getX() + wireConnection.getAnchorPoint1().getX();
        double y1 = nodeHandler1.getY() + wireConnection.getAnchorPoint1().getY();
        double x2 = nodeHandler2.getX() + wireConnection.getAnchorPoint2().getX();
        double y2 = nodeHandler2.getY() + wireConnection.getAnchorPoint2().getY();

        if (x1 == x2 || y1 == y2) {
            polyline.getPoints().setAll(x1, y1, x2, y2);
        } else {
            switch (wireConnection.getAnchorPoint1().getOrientation()) {
                case VERTICAL:
                    if (wireConnection.getAnchorPoint2().getOrientation().equals(AnchorOrientation.VERTICAL)) {
                        polyline.getPoints().setAll(x1, y1, x1, (y1 + y2) / 2, x2, (y1 + y2) / 2, x2, y2);
                    } else {
                        polyline.getPoints().setAll(x1, y1, x1, y2, x2, y2);
                    }
                    break;
                case HORIZONTAL:

                    if (wireConnection.getAnchorPoint2().getOrientation().equals(AnchorOrientation.HORIZONTAL)) {
                        polyline.getPoints().setAll(x1, y1, (x1 + x2) / 2, y1, (x1 + x2) / 2, y2, x2, y2);
                    } else {
                        polyline.getPoints().setAll(x1, y1, x2, y1, x2, y2);
                    }
                    break;
                case NONE:
                    // Case none-none is not handled, it never happens atm
                    if (wireConnection.getAnchorPoint2().getOrientation().equals(AnchorOrientation.HORIZONTAL)) {
                        polyline.getPoints().setAll(x1, y1, x1, y2, x2, y2);
                    } else {
                        polyline.getPoints().setAll(x1, y1, x1, (y1 + y2) / 2, x2, (y1 + y2) / 2, x2, y2);
                    }
                    break;
                default:
                    break;
            }
        }

        relocateArrows();
    }

    private void relocateArrows() {
        for (int i = 0; i < arrows.size(); i++) {
            relocateArrow(polyline, arrows.get(i), i);
        }
    }

    private void relocateArrow(Polyline polyline, Group arrow, int arrowNum) {
        ComponentSize arrowSize = metadata.getComponentMetadata(ComponentType.ARROW).getSize();
        Point2D center = new Point2D(arrowSize.getWidth() / 2, arrowSize.getHeight() / 2);
        double distance = metadata.getArrowMetadata(arrow.getId()).getDistance() + arrowNum * arrowSize.getHeight() * 2;
        relocateArrow(polyline, arrow, center, distance);
    }


    /**
     * A position in the 2D plane together with an orientation
     */
    static class OrientedPosition {

        private final Point2D point;
        private final double orientation;

        public Point2D getPoint() {
            return point;
        }

        public double getOrientation() {
            return orientation;
        }

        public OrientedPosition(Point2D point, double orientation) {
            this.point = point;
            this.orientation = orientation;
        }
    }

    private static void relocateArrow(Polyline polyline, Group arrow, Point2D center, double distance) {
        OrientedPosition newCenterPosition = positionAtDistance(getPoints(polyline), distance);
        Point2D newCenterPoint = newCenterPosition.getPoint().subtract(center);

        arrow.getTransforms().clear();
        arrow.getTransforms().add(new Translate(newCenterPoint.getX(), newCenterPoint.getY()));
        arrow.getTransforms().add(new Rotate(newCenterPosition.getOrientation(), center.getX(), center.getY()));
    }

    private static boolean isEven(int number) {
        return (number & 1) == 0;
    }

    private static List<Point2D> getPoints(Polyline line) {
        List<Point2D> points = new ArrayList<>();
        List<Double> coordinates = line.getPoints();
        checkArgument(isEven(coordinates.size()), "Number of coordinates of polyline points should be even.");

        for (int i = 0; i < coordinates.size(); i = i + 2) {
            points.add(new Point2D(coordinates.get(i), coordinates.get(i+1)));
        }
        return ImmutableList.copyOf(points);
    }

    /**
     * Computes the position at the specified distance on the line represented by a list of points,
     * from the starting point to the end point.
     */
    static OrientedPosition positionAtDistance(List<Point2D> points, double distance) {
        double residual = distance;

        if (points.size() < 2) {
            throw new IllegalArgumentException("Line must contain at least one segment");
        }

        Point2D start = null;
        Point2D end = null;
        for (int i = 0; i < points.size() - 1; i++) {
            start = points.get(i);
            end = points.get(i + 1);

            double segmentLength = start.distance(end);
            if (segmentLength < residual) {
                residual -= segmentLength;
            } else {
                return positionAtRatio(start, end, residual/segmentLength);
            }
        }

        return positionAtRatio(start, end, 1f);
    }

    /**
     * Computes the position at the specified ratio from the starting point to the end point.
     */
    private static OrientedPosition positionAtRatio(Point2D start, Point2D end, double ratio) {
        Point2D segment = end.subtract(start);
        Point2D point = start.add(end.subtract(start).multiply(ratio));
        double angle = orientedAngle(new Point2D(0, 1), segment);
        return new OrientedPosition(point, angle);
    }

    /**
     * Computes the oriented angle between 2 segments, in degrees.
     */
    private static double orientedAngle(Point2D segment1, Point2D segment2) {
        double x = segment1.dotProduct(segment2);
        double y = segment1.crossProduct(segment2).dotProduct(0, 0, 1);
        return Math.toDegrees(Math.atan2(y, x));
    }

}
