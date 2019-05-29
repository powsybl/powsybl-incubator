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

import com.powsybl.substationdiagram.library.AnchorOrientation;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.WireConnection;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WireHandler {

    private final Polyline node;

    private final List<Group> arrows = new ArrayList();

    private final NodeHandler nodeHandler1;

    private final NodeHandler nodeHandler2;

    private final GraphMetadata metadata;
    private static final int UP = 1;
    private static final int DOWN = 2;
    private static final int HV = 1;
    private static final int VH = 2;
    private int lastEvent;
    private int lastOrientation;
    private boolean transponed0;
    private boolean transponed1;

    public WireHandler(Polyline node, NodeHandler nodeHandler1, NodeHandler nodeHandler2, GraphMetadata metadata) {
        this.node = Objects.requireNonNull(node);
        this.nodeHandler1 = Objects.requireNonNull(nodeHandler1);
        this.nodeHandler2 = Objects.requireNonNull(nodeHandler2);
        this.metadata = Objects.requireNonNull(metadata);
    }

    public Node getNode() {
        return this.node;
    }

    public NodeHandler getNodeHandler1() {
        return nodeHandler1;
    }

    public NodeHandler getNodeHandler2() {
        return nodeHandler2;
    }

    public void refresh() {
        boolean done0 = false;
        boolean done1 = false;
        double dist = arrows.size() > 0 ? metadata.getArrowMetadata(arrows.get(0).getId()).getDistance() : 20;

        double arrowSize = metadata.getComponentMetadata(ComponentType.ARROW).getSize().getHeight();
        double dist0 = dist;
        double dist1 = dist + 2 * arrowSize;

        WireConnection wireConnection = WireConnection.searchBetterAnchorPoints(metadata, nodeHandler1, nodeHandler2);

        double x01 = node.getPoints().get(0);
        double y01 = node.getPoints().get(1);
        double x02 = node.getPoints().get(node.getPoints().size() - 2);
        double y02 = node.getPoints().get(node.getPoints().size() - 1);

        // update polyline
        double x1 = nodeHandler1.getX() + wireConnection.getAnchorPoint1().getX();
        double y1 = nodeHandler1.getY() + wireConnection.getAnchorPoint1().getY();
        double x2 = nodeHandler2.getX() + wireConnection.getAnchorPoint2().getX();
        double y2 = nodeHandler2.getY() + wireConnection.getAnchorPoint2().getY();

        if (x1 == x2 || y1 == y2) {
            node.getPoints().setAll(x1, y1, x2, y2);
            if (transponed0) {
                translateArrow(0, x1 - x01, (y1 - y01) / 2, 0, arrowSize / 2, arrowSize / 2, false);
            } else {
                translateArrow(0, x1 - x01, y1 - y01, 0, arrowSize / 2, arrowSize / 2, false);
            }
            if (transponed1) {
                translateArrow(1, x1 - x01, (y1 - y01) / 2, 0, arrowSize / 2, arrowSize / 2, false);
            } else {
                translateArrow(1, x1 - x01, y1 - y01, 0, arrowSize / 2, arrowSize / 2, false);
            }
        } else {
            switch (wireConnection.getAnchorPoint1().getOrientation()) {
                case VERTICAL:
                    if (wireConnection.getAnchorPoint2().getOrientation().equals(AnchorOrientation.VERTICAL)) {
                        node.getPoints().setAll(x1, y1, x1, (y1 + y2) / 2, x2, (y1 + y2) / 2, x2, y2);
                        if (Math.abs(y2 - (y1 + y2) / 2) < 30) {
                            if (Math.abs(x2 - x1) < 30) {
                                //arrows.forEach(a -> a.setVisible(false));
                            } else {
                                if (Math.abs(y02 - (y01 + y02) / 2) >= 30 && !transponed0) {
                                    if (y2 >= y1) {
                                        translateArrow(0, Math.signum(x1 - x2) * (-dist0 - (x01 - x1)), (y2 + y1) / 2  - y01 - dist0, Math.signum(x1 - x2) * 90, arrowSize / 2, arrowSize / 2, true);
                                    } else {
                                        translateArrow(0, Math.signum(x1 - x2) * (-dist0 - (x01 - x1)), (y2 + y1) / 2  - y01 + dist0, Math.signum(x1 - x2) * Math.signum(y2 - y1) * 90, arrowSize / 2, arrowSize / 2, true);
                                    }
                                    transponed0 = true;
                                    done0 = true;
                                }
                            }
                        } else if (Math.abs(y2 -  (y1 + y2) / 2) < 50) {

                            if (Math.abs(y02 -  (y01 + y02) / 2) >= 50 && !transponed1) {
                                if (y2 >= y1) {
                                    translateArrow(1, Math.signum(x1 - x2) * (-dist1 - (x01 - x1)),   (y2 + y1) / 2  - y01 - dist1, Math.signum(x1 - x2) * 90, arrowSize / 2, arrowSize / 2, true);
                                } else {
                                    translateArrow(1, Math.signum(x1 - x2) * (-dist1 - (x01 - x1)),   (y2 + y1) / 2  - y01 + dist1, Math.signum(x1 - x2) * Math.signum(y2 - y1) * 90, arrowSize / 2, arrowSize / 2, true);

                                }
                                transponed1 = true;
                                done1 = true;
                            }
                            if (Math.abs(y2 - (y1 + y2) / 2) >= 30 && transponed0) {
                                if (y2 >= y1) {
                                    translateArrow(0, Math.signum(x1 - x2) * (dist0 - (x01 - x1)), -((y2 + y01) / 2  - y1)  + dist0, Math.signum(x1 - x2) * Math.signum(y1 - y2) * 90, arrowSize / 2, arrowSize / 2, true);
                                } else {
                                    translateArrow(0, Math.signum(x1 - x2) * (dist0 - (x01 - x1)), -((y2 + y01) / 2  - y1)  - dist0, Math.signum(x1 - x2) * Math.signum(y1 - y2) * 90, arrowSize / 2, arrowSize / 2, true);

                                }
                                transponed0 = false;
                                done0 = true;
                            }
                        } else {

                            if (Math.abs(y2 - (y1 + y2) / 2) >= 50 && transponed1) {
                                if (y2 >= y1) {
                                    translateArrow(1, Math.signum(x1 - x2) * (dist1 - (x01 - x1)), -((y2 + y01) / 2  - y1)  + dist1, Math.signum(x1 - x2) * Math.signum(y1 - y2) * 90, arrowSize / 2, arrowSize / 2, true);
                                } else {
                                    translateArrow(1, Math.signum(x1 - x2) * (dist1 - (x01 - x1)), -((y2 + y01) / 2  - y1)  - dist1, Math.signum(x1 - x2) * Math.signum(y1 - y2) * 90, arrowSize / 2, arrowSize / 2, true);
                                }
                                transponed1 = false;
                                done1 = true;
                            }
                        }
                        if (!done0) {
                            if (transponed0) {
                                translateArrow(0, x1 - x01, (y1 - y01 + y2 - y02) / 2, 0, arrowSize / 2, arrowSize / 2, false);
                            } else {
                                translateArrow(0, x1 - x01, y1 - y01, 0, arrowSize / 2, arrowSize / 2, false);
                            }
                        }
                        if (!done1) {
                            if (transponed1) {
                                translateArrow(1, x1 - x01, (y1 - y01 + y2 - y02) / 2, 0, arrowSize / 2, arrowSize / 2, false);
                            } else {
                                translateArrow(1, x1 - x01, y1 - y01, 0, arrowSize / 2, arrowSize / 2, false);
                            }
                        }
                    } else {
                        node.getPoints().setAll(x1, y1, x1, y2, x2, y2);

                        if (lastOrientation == HV) {
                            translateArrow(0, dist0 + x1 - x01, dist0 + y1 - y01, -90, arrowSize / 2, arrowSize / 2, true);
                            translateArrow(1, dist1 + x1 - x01, dist1 + y1 - y01, -90, arrowSize / 2, arrowSize / 2, true);

                        } else {
                            translateArrows(x1 - x01, y1 - y01, 0, arrowSize / 2, arrowSize / 2, true);
                        }

                        lastOrientation = VH;
                    }
                    break;
                case HORIZONTAL:

                    if (wireConnection.getAnchorPoint2().getOrientation().equals(AnchorOrientation.HORIZONTAL)) {
                        node.getPoints().setAll(x1, y1, (x1 + x2) / 2, y1, (x1 + x2) / 2, y2, x2, y2);
                        if (y02 <= y01 && y2 > y01) {
                            if (lastEvent != DOWN) {
                                translateArrows(x1 - x01, y1 - y01 - 60, 180, arrowSize / 2, arrowSize / 2, false);
                                lastEvent = DOWN;
                            }
                        } else if (y02 > y01 && y2 <= y01) {
                            if (lastEvent != UP) {
                                translateArrows(x1 - x01, y1 - y01 + 60, 180, arrowSize / 2, arrowSize / 2, false);
                                lastEvent = UP;
                            }
                        } else {
                            translateArrows(x1 - x01, y1 - y01, 0, arrowSize / 2, arrowSize / 2, false);
                        }
                    } else {
                        node.getPoints().setAll(x1, y1, x2, y1, x2, y2);
                        if (lastOrientation == VH) {
                            translateArrow(0, -dist0 - (x01 - x1), -dist0 - (y01 - y1), 90, arrowSize / 2, arrowSize / 2, true);
                            translateArrow(1, -dist1 - (x01 - x1), -dist1 - (y01 - y1), 90, arrowSize / 2, arrowSize / 2, true);
                        } else {
                            translateArrows(x1 - x01, y1 - y01, 0, arrowSize / 2, arrowSize / 2, false);
                        }
                        lastOrientation = HV;
                    }
                    break;
                case NONE:
                    // Case none-none is not handled, it never happens atm
                    if (wireConnection.getAnchorPoint2().getOrientation().equals(AnchorOrientation.HORIZONTAL)) {
                        node.getPoints().setAll(x1, y1, x1, y2, x2, y2);
                    } else {
                        node.getPoints().setAll(x1, y1, x1, (y1 + y2) / 2, x2, (y1 + y2) / 2, x2, y2);
                        if (y02 <= y01 && y2 > y01) {
                            if (lastEvent != DOWN) {
                                translateArrows(x1 - x01, y1 - y01 - 60, 180, arrowSize / 2, arrowSize / 2, false);
                                lastEvent = DOWN;
                            }
                        } else if (y02 > y01 && y2 <= y01) {
                            if (lastEvent != UP) {
                                translateArrows(x1 - x01, y1 - y01 + 60, 180, arrowSize / 2, arrowSize / 2, false);
                                lastEvent = UP;
                            }
                        } else {
                            translateArrows(x1 - x01, y1 - y01, 0, arrowSize / 2, arrowSize / 2, false);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void addArrow(Group g) {
        arrows.add(g);
    }

    private void translateArrows(double dx, double dy, double angle, double pivotX, double pivotY, boolean rotateText) {
        translateArrow(0, dx, dy, angle, pivotX, pivotY, rotateText);
        translateArrow(1, dx, dy, angle, pivotX, pivotY, rotateText);
    }

    private void translateArrow(int index, double dx, double dy, double angle, double pivotX, double pivotY, boolean rotateText) {
        if (arrows.size() > index) {
            Group a = arrows.get(index);
            if (angle != 0) {
                Rotate rotate = new Rotate();
                rotate.setAngle(angle);
                rotate.setPivotX(pivotX);
                rotate.setPivotY(pivotY);

                a.getChildren().forEach(c -> {
                    if (c instanceof Group) {
                        c.getTransforms().add(rotate);
                    } else if (rotateText && c instanceof Text) {
                        c.getTransforms().add(rotate);
                    }
                });
            }

            Translate translate = new Translate();
            translate.setX(dx);
            translate.setY(dy);
            translate.setZ(0);
            a.getTransforms().addAll(translate);
        }
    }

}
