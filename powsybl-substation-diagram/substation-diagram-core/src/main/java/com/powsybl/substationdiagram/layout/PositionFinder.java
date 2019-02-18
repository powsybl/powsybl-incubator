/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.substationdiagram.model.Graph;

/**
 * a PositionFinder determines:
 * <ul>
 *     <li>the positions of nodeBuses</li>
 *     <li>cell order and direction of each cell connected to Bus (ie all cells except Shunt ones)</li>
 * </ul>
 *
 * @author Jeanson Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface PositionFinder {

    void buildLayout(Graph graph);
}
