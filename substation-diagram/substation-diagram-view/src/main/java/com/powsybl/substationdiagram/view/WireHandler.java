/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import com.powsybl.substationdiagram.library.AnchorOrientation;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.WireConnection;
import javafx.scene.Node;
import javafx.scene.shape.Polyline;

import java.util.Objects;

/**
 * @author Jeanson Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WireHandler {

    private final Polyline node;

    private final NodeHandler nodeHandler1;

    private final NodeHandler nodeHandler2;

    private final GraphMetadata metadata;

    public WireHandler(Polyline node, NodeHandler nodeHandler1, NodeHandler nodeHandler2,
                       GraphMetadata metadata) {
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
        WireConnection wireConnection = WireConnection.searchBetterAnchorPoints(metadata, nodeHandler1, nodeHandler2);

        // update polyline
        double x1 = nodeHandler1.getX() + wireConnection.getAnchorPoint1().getX();
        double y1 = nodeHandler1.getY() + wireConnection.getAnchorPoint1().getY();
        double x2 = nodeHandler2.getX() + wireConnection.getAnchorPoint2().getX();
        double y2 = nodeHandler2.getY() + wireConnection.getAnchorPoint2().getY();

        if (x1 == x2 || y1 == y2) {
            node.getPoints().setAll(x1, y1, x2, y2);
        } else {
            switch (wireConnection.getAnchorPoint1().getOrientation()) {
                case VERTICAL:
                    if (wireConnection.getAnchorPoint2().getOrientation().equals(AnchorOrientation.VERTICAL)) {
                        node.getPoints().setAll(x1, y1, x1, (y1 + y2) / 2, x2, (y1 + y2) / 2, x2, y2);
                    } else {
                        node.getPoints().setAll(x1, y1, x1, y2, x2, y2);
                    }
                    break;
                case HORIZONTAL:
                    if (wireConnection.getAnchorPoint2().getOrientation().equals(AnchorOrientation.HORIZONTAL)) {
                        node.getPoints().setAll(x1, y1, (x1 + x2) / 2, y1, (x1 + x2) / 2, y2, x2, y2);
                    } else {
                        node.getPoints().setAll(x2, y2, x2, y1, x1, y1);
                    }
                    break;
                case NONE:
                    // Case none-none is not handled, it never happens atm
                    if (wireConnection.getAnchorPoint2().getOrientation().equals(AnchorOrientation.HORIZONTAL)) {
                        node.getPoints().setAll(x1, y1, x1, y2, x2, y2);
                    } else {
                        node.getPoints().setAll(x2, y2, x2, y1, x1, y1);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
