/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.substationdiagram.layout.LayoutParameters;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractBlock implements Block {

    protected Type type;

    @JsonIgnore
    private int cardinalityStart;
    @JsonIgnore
    private int cardinalityEnd;
    @JsonBackReference
    private Block parentBlock;
    @JsonIgnore
    private BusNode busNode;

    @JsonIgnore
    private Cell cell;

    private Position position;
    private Coord coord;

    /**
     * Constructor for primary layout.block with the list of nodes corresponding to the
     * layout.block
     */
    AbstractBlock() {
        position = new Position(-1, -1);
        coord = new Coord(-1, -1);
        this.parentBlock = null;
        this.busNode = null;
    }

    AbstractBlock(Cell cell) {
        this();
        if (cell != null) {
            setCell(cell);
        }
    }

    @Override
    public int getCardinalityInverse(Node commonNode) {
        if (commonNode.equals(getStartingNode())) {
            return cardinalityEnd;
        }
        if (commonNode.equals(getEndingNode())) {
            return cardinalityStart;
        }
        return 0;
    }

    @Override
    public int getCardinality(Node commonNode) {
        if (commonNode.equals(getStartingNode())) {
            return cardinalityStart;
        }
        if (commonNode.equals(getEndingNode())) {
            return cardinalityEnd;
        }
        return 0;
    }

    void setCardinalityStart(int cardinalityStart) {
        this.cardinalityStart = cardinalityStart;
    }

    void setCardinalityEnd(int cardinalityEnd) {
        this.cardinalityEnd = cardinalityEnd;
    }

    public Block getParentBlock() {
        return parentBlock;
    }

    @Override
    public void setParentBlock(Block parentBlock) {
        this.parentBlock = parentBlock;
    }

    @Override
    public BusNode getBusNode() {
        return this.busNode;
    }

    @Override
    public void setBusNode(BusNode busNode) {
        this.busNode = busNode;
    }

    @Override
    public void defineExtremity(Node node, Extremity ext) {
        if (!node.equals(getExtremityNode(ext))) {
            reverseBlock();
        }
    }

    private Node getExtremityNode(Extremity ext) {
        if (ext == Extremity.START) {
            return getStartingNode();
        } else {
            return getEndingNode();
        }
    }

    public Cell getCell() {
        return cell;
    }

    @Override
    public void setCell(Cell cell) {
        this.cell = cell;
        if (cell.getType() == Cell.CellType.INTERNBOUND
                || cell.getType() == Cell.CellType.SHUNT) {
            setOrientation(Orientation.HORIZONTAL);
        } else {
            setOrientation(Orientation.VERTICAL);
        }
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public void setOrientation(Orientation orientation) {
        getPosition().setOrientation(orientation);
    }

    public Coord getCoord() {
        return coord;
    }

    @Override
    public void setXSpan(double xSpan) {
        getCoord().setXSpan(xSpan);
    }

    @Override
    public void setYSpan(double ySpan) {
        getCoord().setYSpan(ySpan);
    }

    @Override
    public void setX(double x) {
        getCoord().setX(x);
    }

    @Override
    public void setY(double y) {
        getCoord().setY(y);
    }


    @Override
    public void calculateCoord(LayoutParameters layoutParam) {
        if (cell.getType() == Cell.CellType.SHUNT) {
            ((PrimaryBlock) this).coordShuntCase();
        } else {
            if (getParentBlock() == null || getPosition().isAbsolute()) {
                calculateRootCoord(layoutParam);
            }
            if (getPosition().getOrientation() == Orientation.VERTICAL) {
                coordVerticalCase(layoutParam);
            } else {
                coordHorizontalCase(layoutParam);
            }
        }
    }

    private void calculateRootCoord(LayoutParameters layoutParam) {
        double dyToBus = 0;
        coord.setXSpan((double) position.getHSpan() * layoutParam.getCellWidth());
        if (cell.getType() == Cell.CellType.INTERN || cell.getType() == Cell.CellType.INTERNBOUND) {
            coord.setYSpan(0);
            if (cell.getDirection() != Cell.Direction.FLAT) {
                dyToBus = layoutParam.getInternCellHeight() * position.getV();
            }
        } else {
            coord.setYSpan(layoutParam.getExternCellHeight());
            dyToBus = layoutParam.getExternCellHeight() / 2 + layoutParam.getStackHeight();
        }

        coord.setX(layoutParam.getInitialXBus()
                           + layoutParam.getCellWidth() * position.getH()
                           + coord.getXSpan() / 2);

        switch (cell.getDirection()) {
            case BOTTOM:
                coord.setY(layoutParam.getInitialYBus()
                                   + (cell.getMaxBusPosition().getV() - 1) * layoutParam.getVerticalSpaceBus()
                                   + dyToBus);
                break;
            case TOP:
                coord.setY(layoutParam.getInitialYBus()
                                   - dyToBus);
                break;
            case FLAT:
                coord.setY(
                        layoutParam.getInitialYBus() + (getPosition().getV() - 1) * layoutParam.getVerticalSpaceBus());
                break;
            default:
        }
    }

    @Override
    public Block.Type getType() {
        return this.type;
    }
}
