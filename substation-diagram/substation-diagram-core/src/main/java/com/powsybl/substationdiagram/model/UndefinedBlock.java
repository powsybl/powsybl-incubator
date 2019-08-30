/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.substationdiagram.model;

import com.powsybl.substationdiagram.layout.LayoutParameters;

import java.util.List;
import java.util.Objects;

/**
 * A block group that cannot be correctly decomposed anymore.
 * All subBlocks are superposed.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class UndefinedBlock extends AbstractComposedBlock {

    public UndefinedBlock(List<Block> subBlocks) {
        super(Type.UNDEFINED, subBlocks);
        if (subBlocks.isEmpty()) {
            throw new IllegalArgumentException("Empty block list");
        }
        this.subBlocks = Objects.requireNonNull(subBlocks);
        for (Block block : subBlocks) {
            block.setParentBlock(this);
        }
    }

    @Override
    public void calculateDimensionAndInternPos() {
        for (Block block : subBlocks) {
            block.calculateDimensionAndInternPos();
        }
        if (getPosition().getOrientation() == Orientation.VERTICAL) {
            // TODO
        } else {
            throw new UnsupportedOperationException("Horizontal layout of undefined  block not supported");
        }
    }

    @Override
    public void coordVerticalCase(LayoutParameters layoutParam) {
        for (Block block : subBlocks) {
            block.setX(getCoord().getX());
            block.setY(getCoord().getY());
            block.setXSpan(getCoord().getXSpan());
            block.setYSpan(getCoord().getYSpan());
            block.coordVerticalCase(layoutParam);
        }
    }

    @Override
    public void coordHorizontalCase(LayoutParameters layoutParam) {
        throw new UnsupportedOperationException("Horizontal layout of undefined  block not supported");
    }

    @Override
    public String toString() {
        return "UndefinedBlock(subBlocks=" + subBlocks + ")";
    }
}
