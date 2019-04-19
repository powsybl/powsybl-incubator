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
 * All blocks are superposed.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class UndefinedBlock extends AbstractBlock {

    private final List<Block> blocks;

    public UndefinedBlock(List<Block> blocks) {
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("Empty block list");
        }
        this.blocks = Objects.requireNonNull(blocks);
        for (Block block : blocks) {
            block.setParentBlock(this);
        }
    }

    @Override
    public Node getStartingNode() {
        return blocks.get(0).getStartingNode();
    }

    @Override
    public Node getEndingNode() {
        return blocks.get(0).getEndingNode();
    }

    @Override
    public void reverseBlock() {
        // nothing to do
    }

    @Override
    public boolean isEmbedingNodeType(Node.NodeType type) {
        return false;
    }

    @Override
    public void calculateDimensionAndInternPos() {
        for (Block block : blocks) {
            block.calculateDimensionAndInternPos();
        }
    }

    @Override
    public int getOrder() {
        return blocks.get(0).getOrder();
    }

    @Override
    public void coordVerticalCase(LayoutParameters layoutParam) {
        for (Block block : blocks) {
            block.setX(getCoord().getX());
            block.setY(getCoord().getY());
            block.setXSpan(getCoord().getXSpan());
            block.setYSpan(getCoord().getYSpan());
            block.coordVerticalCase(layoutParam);
        }
    }

    @Override
    public void coordHorizontalCase(LayoutParameters layoutParam) {
        for (Block block : blocks) {
            block.setX(getCoord().getX());
            block.setY(getCoord().getY());
            block.setXSpan(getCoord().getXSpan());
            block.setYSpan(getCoord().getYSpan());
            block.coordHorizontalCase(layoutParam);
        }
    }
}
