/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

/**
 *
 * A layout is in charge of defining the actual coordinates of each {@link com.powsybl.substationdiagram.model.Node Node}
 * of the {@link com.powsybl.substationdiagram.model.Graph Graph}.
 *
 * <p>Implementations may rely on previously computed abstractions such as {@link com.powsybl.substationdiagram.model.Block Blocks}
 * and {@link com.powsybl.substationdiagram.model.Cell Cells}.
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface VoltageLevelLayout {

    /**
     * Calculate real coordinate of busbar and blocks connected to busbar
     */
    void run(LayoutParameters layoutParam);
}
