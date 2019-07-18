/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.substationdiagram.model.Graph;

/**
 * In charge of detecting {@link com.powsybl.substationdiagram.model.Cell Cells} in a {@link Graph}.
 * The detector will add detected cells to the graph. We assume that the graph has not knowledge of cells
 * prior to the call to {@link #detectCells(Graph)}.
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface CellDetector {

    void detectCells(Graph graph);
}
