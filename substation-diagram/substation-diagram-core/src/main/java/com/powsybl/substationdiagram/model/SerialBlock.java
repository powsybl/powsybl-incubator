/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.powsybl.substationdiagram.layout.LayoutParameters;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SerialBlock extends AbstractBlock {

    @JsonManagedReference
    private Block lowerBlock;
    @JsonManagedReference
    private Block upperBlock;
    @JsonIgnore
    private Block[] subBlocks;

    private boolean isH2V = false;

    /**
     * Constructor
     * A layout.block chain is oriented in order to have.
     * Lower - embedding BusNode if only one of both layout.block embed a BusNode
     * Upper - (as a consequence) can embed a BusNode only if Lower as one
     *
     * @param block1     one layout.block to chain
     * @param block2     the other layout.block to chain
     * @param commonNode the node that is common to block1 and block2
     */

    public SerialBlock(Block block1,
                       Block block2,
                       Node commonNode,
                       Cell cell) {
        this(block1, block2, commonNode);
        if (cell != null) {
            setCell(cell);
        }
    }

    public SerialBlock(Block block1,
                       Block block2,
                       Node commonNode) {
        type = Type.SERIAL;
        if (block1.isEmbedingNodeType(Node.NodeType.BUS) || block2.isEmbedingNodeType(Node.NodeType.FEEDER)) {
            upperBlock = block2;
            lowerBlock = block1;
        } else {
            upperBlock = block1;
            lowerBlock = block2;
        }

        upperBlock.getPosition().setHV(0, 1);
        lowerBlock.getPosition().setHV(0, 0);

        subBlocks = new Block[2];
        subBlocks[0] = this.lowerBlock;
        subBlocks[1] = this.upperBlock;

        for (Block child : subBlocks) {
            child.setParentBlock(this);
        }


        setCardinalityStart(lowerBlock.getCardinalityInverse(commonNode));

        setCardinalityEnd(upperBlock.getCardinalityInverse(commonNode));
        upperBlock.defineExtremity(commonNode, Extremity.START);
        lowerBlock.defineExtremity(commonNode, Extremity.END);
    }

    @Override
    public boolean isEmbedingNodeType(Node.NodeType type) {
        return lowerBlock.isEmbedingNodeType(type) || upperBlock.isEmbedingNodeType(type);
    }

    @Override
    public int getOrder() {
        return upperBlock.getOrder();
    }

    public Block getUpperBlock() {
        return upperBlock;
    }

    public Block getLowerBlock() {
        return lowerBlock;
    }

    @Override
    public void reverseBlock() {
        Block temp = lowerBlock;
        lowerBlock = upperBlock;
        upperBlock = temp;
        lowerBlock.reverseBlock();
        upperBlock.reverseBlock();
    }


    @Override
    public Node getStartingNode() {
        return lowerBlock.getStartingNode();
    }

    @Override
    public Node getEndingNode() {
        return upperBlock.getEndingNode();
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
        lowerBlock.calculateDimensionAndInternPos();
        upperBlock.calculateDimensionAndInternPos();
        Position lPosition = lowerBlock.getPosition();
        Position uPosition = upperBlock.getPosition();


        if (getPosition().getOrientation() == Orientation.VERTICAL) {
            getPosition().setHSpan(Math.max(uPosition.getHSpan(), lPosition.getHSpan()));
            getPosition().setVSpan(lPosition.getVSpan() + uPosition.getVSpan());
            lPosition.setHV(0, 0);
            uPosition.setHV(0, lPosition.getVSpan());
        } else {
            isH2V = lPosition.getOrientation() == Orientation.VERTICAL
                    && uPosition.getOrientation() == Orientation.HORIZONTAL;
            int h2vShift = isH2V ? 1 : 0;
            getPosition().setHSpan(uPosition.getHSpan() + lPosition.getHSpan() - h2vShift);
            getPosition().setVSpan(Math.max(uPosition.getVSpan(), lPosition.getVSpan()));
            lPosition.setHV(0, 0);
            uPosition.setHV(lPosition.getHSpan() - h2vShift, 0);
        }
    }


    @Override
    public void coordVerticalCase(LayoutParameters layoutParam) {
        double y0;
        double yPxStep;
        int sign = getCell().getDirection() == Cell.Direction.TOP ? 1 : -1;
        y0 = getCoord().getY() + sign * getCoord().getYSpan() / 2;
        yPxStep = -sign * getCoord().getYSpan() / getPosition().getVSpan();

        for (Block sub : subBlocks) {
            sub.setX(getCoord().getX());
            sub.setXSpan(getCoord().getXSpan());

            sub.setYSpan(
                    getCoord().getYSpan() * ((double) sub.getPosition().getVSpan() / getPosition().getVSpan()));
            sub.setY(y0 + yPxStep * (sub.getPosition().getV() + (double) sub.getPosition().getVSpan() / 2));

            sub.calculateCoord(layoutParam);
        }
    }

    @Override
    public void coordHorizontalCase(LayoutParameters layoutParam) {
        double x0 = getCoord().getX() - getCoord().getXSpan() / 2;
        double xPxStep = getCoord().getXSpan() / getPosition().getHSpan();
        double xTranslateInternalNonFlatCell = isH2V ? layoutParam.getCellWidth() / 2 : 0;

        for (Block sub : subBlocks) {
            sub.setX(x0 + (sub.getPosition().getH() + (double) sub.getPosition().getHSpan() / 2) * xPxStep
                             + ((sub == upperBlock) ? xTranslateInternalNonFlatCell : 0));
            sub.setXSpan(sub.getPosition().getHSpan() * xPxStep);
            sub.setY(getCoord().getY());
            sub.setYSpan(getCoord().getYSpan());
            sub.calculateCoord(layoutParam);
        }
    }
}
