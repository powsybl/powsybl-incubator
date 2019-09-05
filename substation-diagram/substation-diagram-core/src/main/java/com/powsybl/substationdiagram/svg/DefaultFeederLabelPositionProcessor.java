/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.model.*;

import java.util.Objects;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class DefaultFeederLabelPositionProcessor implements LabelPositionProcessor {

    final double defaultOffsetX;
    final double defaultOffsetY;
    final ComponentLibrary componentLibrary;
    final int fontSize;

    public DefaultFeederLabelPositionProcessor(double defaultOffsetX, double defaultOffsetY, int fontSize, ComponentLibrary componentLibrary) {
        Objects.requireNonNull(componentLibrary);
        this.defaultOffsetX = defaultOffsetX;
        this.defaultOffsetY = defaultOffsetY;
        this.componentLibrary = componentLibrary;
        this.fontSize = fontSize;
    }

    @Override
    public Position compute(Node node, BusCell.Direction direction) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(direction);
        double yShift = -defaultOffsetY;
        if (node.getCell() != null) {
            yShift = direction == BusCell.Direction.TOP
                    ? -defaultOffsetY
                    : ((int) (componentLibrary.getSize(node.getComponentType()).getHeight()) + fontSize + defaultOffsetY);
        }
        return new Position(-defaultOffsetX, yShift);
    }
}
