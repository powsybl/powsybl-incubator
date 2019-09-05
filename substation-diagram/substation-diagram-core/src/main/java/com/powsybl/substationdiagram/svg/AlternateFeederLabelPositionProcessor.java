/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AlternateFeederLabelPositionProcessor shifts adjacent, same-direction feeders labels' vertical positions
 * on a ramp, to prevent long labels overlapping.
 *
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class AlternateFeederLabelPositionProcessor implements LabelPositionProcessor {

    final double defaultXoffset;
    final double defaultyoffset;
    final ComponentLibrary componentLibrary;
    final int fontSize;

    final Map<BusCell.Direction, Integer> directionsMaxComponentsHeights = new EnumMap<>(BusCell.Direction.class);
    final Map<Node, Integer> nodesCounters = new HashMap<>();
    final int sequenceMod;

    public AlternateFeederLabelPositionProcessor(Map<Node, BusCell.Direction> feederNodesDirections, int rampSize,
                                                 double defaultXoffset, double defaultYoffset, int fontSize, ComponentLibrary componentLibrary) {
        Objects.requireNonNull(feederNodesDirections);
        Objects.requireNonNull(componentLibrary);
        this.sequenceMod = rampSize;
        this.defaultXoffset = defaultXoffset;
        this.defaultyoffset = defaultYoffset;
        this.componentLibrary = componentLibrary;
        this.fontSize = fontSize;

        Map<BusCell.Direction, Integer> directionsCounters = new EnumMap<>(BusCell.Direction.class);

        Stream.of(BusCell.Direction.values()).forEach(direction -> {
            directionsCounters.put(direction, 0);
            directionsMaxComponentsHeights.put(direction, 0);
        });

        List<Node> orderedNodes = feederNodesDirections.keySet().stream().sorted(Comparator.comparing(Node::getX)).collect(Collectors.toList());
        orderedNodes.forEach(node -> {
            BusCell.Direction nodeDirection = feederNodesDirections.get(node);
            nodesCounters.put(node, directionsCounters.get(nodeDirection));
            directionsCounters.put(nodeDirection, (directionsCounters.get(nodeDirection) + 1) % rampSize);
            if (node.getCell() != null) {
                int componentHeight = (int) (componentLibrary.getSize(node.getComponentType()).getHeight());
                if (componentHeight > directionsMaxComponentsHeights.get(nodeDirection)) {
                    directionsMaxComponentsHeights.put(nodeDirection, componentHeight);
                }
            }
        });
    }

    @Override
    public Position compute(Node node, BusCell.Direction direction) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(direction);

        double yShift = -defaultyoffset;
        if (node.getCell() != null) {
            int componentHeight = (int) (componentLibrary.getSize(node.getComponentType()).getHeight());
            if (direction == BusCell.Direction.TOP) {
                int componentDisplacement = (componentHeight == 0) ? directionsMaxComponentsHeights.get(direction) / 2 : 0;
                yShift = -(componentDisplacement + defaultyoffset  + nodesCounters.get(node) * fontSize);
            } else {
                int componentDisplacement = (componentHeight == 0) ? directionsMaxComponentsHeights.get(direction) / 2 : componentHeight;
                yShift = componentDisplacement + defaultyoffset + (nodesCounters.get(node) + 1) * fontSize;
            }
        }
        return new Position(-defaultXoffset, yShift);
    }
}
