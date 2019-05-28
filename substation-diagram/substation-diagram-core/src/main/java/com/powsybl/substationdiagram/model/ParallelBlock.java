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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ParallelBlock extends AbstractBlock {

    private final List<Block> subBlocks = new ArrayList<>();

    public ParallelBlock(List<Block> subBlocks, AbstractCell cell, boolean allowMerge) {
        this(subBlocks, allowMerge);
        if (cell != null) {
            setCell(cell);
        }
    }

    public ParallelBlock(List<Block> subBlocks) {
        this(subBlocks, true);
    }

    public ParallelBlock(List<Block> subBlocks, boolean allowMerge) {
        super(Type.PARALLEL);
        if (subBlocks.isEmpty()) {
            throw new IllegalArgumentException("Empty block list");
        }
        subBlocks.forEach(child -> {
            if (child.getType() == Type.PARALLEL && allowMerge) {
                this.subBlocks.addAll(((ParallelBlock) child).getSubBlocks());
            } else {
                this.subBlocks.add(child);
            }
        });

        Node node0s = subBlocks.get(0).getStartingNode();
        Node node0e = subBlocks.get(0).getEndingNode();
        subBlocks.forEach(b -> {
            b.setParentBlock(this);
            if (b.getStartingNode() != node0s && b.getEndingNode() != node0e) {
                b.reverseBlock();
            }
        });

        setCardinalityStart(this.subBlocks.size());
        setCardinalityEnd(this.subBlocks.size());
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
        return getStartingNode().getType() == Node.NodeType.FEEDER ?
                ((FeederNode) getStartingNode()).getOrder() : 0;
    }

    @Override
    public Node getStartingNode() {
        return this.subBlocks.get(0).getStartingNode();
    }

    @Override
    public Node getEndingNode() {
        return this.subBlocks.get(0).getEndingNode();
    }

    @Override
    public void reverseBlock() {
        subBlocks.forEach(Block::reverseBlock);
    }

    @Override
    public void setOrientation(Orientation orientation) {
        super.setOrientation(orientation);
        for (Block sub : subBlocks) {
            sub.setOrientation(orientation);
        }
    }

    @Override
    public void calculateDimensionAndInternPos() {
        for (Block subBlock : subBlocks) {
            subBlock.calculateDimensionAndInternPos();
        }
        if (getPosition().getOrientation() == Orientation.VERTICAL) {
            getPosition().setVSpan(subBlocks.stream().mapToInt(b -> b.getPosition().getVSpan()).max().orElse(0));
            if (isEmbedingNodeType(Node.NodeType.BUS)) {
                handleStackingOnBuses();
            } else {
                orderSubBlocksByFeederOrder();
                getPosition().setHSpan(subBlocks.stream().mapToInt(b -> b.getPosition().getHSpan()).sum());
                int h = 0;
                for (Block block : subBlocks) {
                    block.getPosition().setHV(h, 0);
                    h += block.getPosition().getHSpan();
                }
            }
        } else {
            getPosition().setVSpan(subBlocks.stream().mapToInt(b -> b.getPosition().getVSpan()).sum());
            getPosition().setHSpan(subBlocks.stream().mapToInt(b -> b.getPosition().getHSpan()).max().orElse(0));
            int v = 0;
            for (Block subBlock : subBlocks) {
                subBlock.getPosition().setHV(0, v);
                v += subBlock.getPosition().getVSpan();
            }
        }
    }

    private void handleStackingOnBuses() {
        List<Block> subBlocksCopy = new ArrayList<>(subBlocks);
        int h = 0;
        Orientation previousOrientation = subBlocksCopy.get(0).getPosition().getOrientation();
        while (!subBlocksCopy.isEmpty()) {
            Block b = subBlocksCopy.get(0);
/*
            if (b.getPosition().getOrientation() == Orientation.HORIZONTAL
                    && previousOrientation == Orientation.VERTICAL) {
                h--;
            }
*/
            b.getPosition().setHV(h, 0);
            if (b instanceof PrimaryBlock && !((PrimaryBlock) b).getStackableBlocks().isEmpty()) {
                final int finalH = h;
                ((PrimaryBlock) b).getStackableBlocks().forEach(sb -> sb.getPosition().setHV(finalH, 0));
                h++;
                subBlocksCopy.removeAll(((PrimaryBlock) b).getStackableBlocks());
            } else {
                h += b.getPosition().getHSpan();
            }
            subBlocksCopy.remove(b);
            previousOrientation = b.getPosition().getOrientation();
        }
        getPosition().setHSpan(h);
    }

    private void orderSubBlocksByFeederOrder() {
        subBlocks.sort(Comparator.comparingInt(Block::getOrder));
    }

    @Override
    public void coordVerticalCase(LayoutParameters layoutParam) {
        double x0;
        double xPxStep;
        if (getPosition().getHSpan() != 1) {
            x0 = getCoord().getX() - getCoord().getXSpan() / 2;
            xPxStep = getCoord().getXSpan() / getPosition().getHSpan();
        } else {
            x0 = getCoord().getX();
            xPxStep = 0;
        }

        final double x0Final = x0;
        final double xPxStepFinal = xPxStep;
        subBlocks.forEach(sub -> {
            sub.setX(x0Final + (sub.getPosition().getH() + (double) sub.getPosition().getHSpan() / 2) * xPxStepFinal);
            sub.setXSpan(xPxStepFinal * sub.getPosition().getHSpan());
            sub.setY(getCoord().getY());
            sub.setYSpan(getCoord().getYSpan());
            sub.calculateCoord(layoutParam);
        });
    }

    @Override
    public void coordHorizontalCase(LayoutParameters layoutParam) {
        subBlocks.forEach(sub -> {
            sub.setX(getCoord().getX());
/*
            if (sub.getType() == Type.PRIMARY && sub.isEmbedingNodeType(Node.NodeType.BUS)) {
                sub.setXSpan(0);
            } else {
*/
            sub.setXSpan(getCoord().getXSpan());
//            }
            sub.setY(getCoord().getY());
            sub.setYSpan(getCoord().getYSpan());
            sub.calculateCoord(layoutParam);
        });
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
