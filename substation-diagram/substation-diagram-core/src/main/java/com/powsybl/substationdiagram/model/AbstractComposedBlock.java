/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractComposedBlock extends AbstractBlock {

    List<Block> subBlocks;

    AbstractComposedBlock(Type type, List<Block> subBlocks, Cell cell) {
        super(type);
        if (subBlocks.isEmpty()) {
            throw new IllegalArgumentException("Empty block list");
        }
        this.subBlocks = subBlocks;
        subBlocks.forEach(b -> {
            b.setParentBlock(this);
        });
        setCell(cell);
    }

    @Override
    public Graph getGraph() {
        return subBlocks.get(0).getGraph();
    }

    public List<Block> getSubBlocks() {
        return subBlocks;
    }

    @Override
    public boolean isEmbedingNodeType(Node.NodeType type) {
        return subBlocks.stream().anyMatch(b -> b.isEmbedingNodeType(type));
    }

    @Override
    public int getOrder() {
        return getEndingNode().getType() == Node.NodeType.FEEDER ?
                ((FeederNode) getEndingNode()).getOrder() : 0;
    }

    @Override
    public Node getStartingNode() {
        return subBlocks.get(0).getStartingNode();
    }

    @Override
    public Node getEndingNode() {
        return subBlocks.get(subBlocks.size() - 1).getEndingNode();
    }

    @Override
    public void reverseBlock() {
        Collections.reverse(subBlocks);
        subBlocks.forEach(Block::reverseBlock);
    }

    @Override
    public void setOrientation(Orientation orientation) {
        super.setOrientation(orientation);
        subBlocks.forEach(sub -> sub.setOrientation(orientation));
    }

    @Override
    protected void writeJsonContent(JsonGenerator generator) throws IOException {
        generator.writeFieldName("nodes");
        generator.writeStartArray();
        for (Block subBlock : subBlocks) {
            subBlock.writeJson(generator);
        }
        generator.writeEndArray();
    }

    @Override
    public String toString() {
        return "ParallelBlock(subBlocks=" + subBlocks + ")";
    }
}