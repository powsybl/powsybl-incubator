/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.substationdiagram.layout.LayoutParameters;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * A block group that cannot be correctly decomposed anymore.
 * All subBlocks are superposed.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class UndefinedBlock extends AbstractBlock {

    private final List<Block> subBlocks;

    public UndefinedBlock(List<Block> subBlocks) {
        super(Type.UNDEFINED);
        if (subBlocks.isEmpty()) {
            throw new IllegalArgumentException("Empty block list");
        }
        this.subBlocks = Objects.requireNonNull(subBlocks);
        for (Block block : subBlocks) {
            block.setParentBlock(this);
        }
    }

    @Override
    public Graph getGraph() {
        return subBlocks.get(0).getGraph();
    }

    @Override
    public Node getStartingNode() {
        return subBlocks.get(0).getStartingNode();
    }

    @Override
    public Node getEndingNode() {
        return subBlocks.get(0).getEndingNode();
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
    public int getOrder() {
        return subBlocks.get(0).getOrder();
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
    protected void writeJsonContent(JsonGenerator generator) throws IOException {
        generator.writeFieldName("blocks");
        generator.writeStartArray();
        for (Block subBlock : subBlocks) {
            subBlock.writeJson(generator);
        }
        generator.writeEndArray();
    }

    @Override
    public String toString() {
        return "UndefinedBlock(subBlocks=" + subBlocks + ")";
    }
}
