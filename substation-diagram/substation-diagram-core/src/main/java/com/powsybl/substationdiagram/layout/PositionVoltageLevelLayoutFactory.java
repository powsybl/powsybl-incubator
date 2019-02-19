/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.substationdiagram.model.Graph;

import java.util.Objects;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PositionVoltageLevelLayoutFactory implements VoltageLevelLayoutFactory {

    private final CellDetector cellDetector;

    private final PositionFinder positionFinder;

    private boolean stack = true;

    public PositionVoltageLevelLayoutFactory() {
        this(new ImplicitCellDetector(), new PositionFromExtension());
    }

    public PositionVoltageLevelLayoutFactory(CellDetector cellDetector, PositionFinder positionFinder) {
        this.cellDetector = Objects.requireNonNull(cellDetector);
        this.positionFinder = Objects.requireNonNull(positionFinder);
    }

    public boolean isStack() {
        return stack;
    }

    public void setStack(boolean stack) {
        this.stack = stack;
    }

    @Override
    public VoltageLevelLayout create(Graph graph) {
        // detect cells
        cellDetector.detectCells(graph);

        // build blocks from cells
        new BlockOrganizer(positionFinder, stack).organize(graph);

        return new PositionVoltageLevelLayout(graph);
    }
}
