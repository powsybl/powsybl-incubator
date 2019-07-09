/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.svg.WireConnection;
import javafx.scene.Node;
import javafx.scene.shape.Polyline;

import java.util.List;
import java.util.Objects;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class WireHandler {

    private final Polyline node;

    private final NodeHandler nodeHandler1;

    private final NodeHandler nodeHandler2;

    private final boolean straight;

    private final boolean snakeLine;

    private final GraphMetadata metadata;

    public WireHandler(Polyline node, NodeHandler nodeHandler1, NodeHandler nodeHandler2,
                       boolean straight, boolean snakeLine, GraphMetadata metadata) {
        this.node = Objects.requireNonNull(node);
        this.nodeHandler1 = Objects.requireNonNull(nodeHandler1);
        this.nodeHandler2 = Objects.requireNonNull(nodeHandler2);
        this.straight = straight;
        this.snakeLine = snakeLine;
        this.metadata = Objects.requireNonNull(metadata);
    }

    public Node getNode() {
        return this.node;
    }

    public boolean isSnakeLine() {
        return snakeLine;
    }

    public NodeHandler getNodeHandler1() {
        return nodeHandler1;
    }

    public NodeHandler getNodeHandler2() {
        return nodeHandler2;
    }

    public void refresh() {
        WireConnection wireConnection = WireConnection.searchBetterAnchorPoints(metadata, nodeHandler1, nodeHandler2);

        if (!snakeLine) {   // inside voltageLevel
            List<Double> pol = wireConnection.calculatePolylinePoints(nodeHandler1, nodeHandler2, straight);
            node.getPoints().setAll(pol);
        }
    }
}
