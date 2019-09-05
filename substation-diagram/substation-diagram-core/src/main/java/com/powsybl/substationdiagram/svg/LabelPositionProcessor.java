/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import com.powsybl.substationdiagram.model.BusCell;
import com.powsybl.substationdiagram.model.Node;

/**
 * A LabelPositionProcessor determines the position of a label for a node,
 * given a BusCell.Direction info.
 *
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public interface LabelPositionProcessor {
    final class Position {
        private double x = -1;
        private double y = -1;

        public Position(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    Position compute(Node node, BusCell.Direction direction);
}
