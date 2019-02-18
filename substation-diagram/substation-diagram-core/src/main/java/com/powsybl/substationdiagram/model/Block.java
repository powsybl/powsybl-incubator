/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.substationdiagram.layout.LayoutParameters;

/**
 * @author Jeanson Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface Block {
    enum Type {
        PRIMARY, PARALLEL, SERIAL
    }

    enum Extremity {
        START,
        END
    }


    Node getStartingNode();

    Node getEndingNode();

    void reverseBlock();

    boolean isEmbedingNodeType(Node.NodeType type);

    void setParentBlock(Block parentBlock);

    Position getPosition();

    void setXSpan(double xSpan);

    void setYSpan(double ySpan);

    void setX(double x);

    void setY(double y);


    /**
     * Calculate maximal pxWidth that layout.block can use in a cell without modifying
     * root pxWidth
     */
    void calculateDimensionAndInternPos();

    /**
     * Calculates all the blocks dimensions and find the order of the layout.block inside
     * the cell
     */
    void calculateCoord(LayoutParameters layoutParam);

    int getOrder();

    void coordVerticalCase(LayoutParameters layoutParam);

    void coordHorizontalCase(LayoutParameters layoutParam);

    int getCardinality(Node commonNode);

    int getCardinalityInverse(Node commonNode);

    void defineExtremity(Node node, AbstractBlock.Extremity ext);

    void setCell(Cell cell);

    BusNode getBusNode();

    void setBusNode(BusNode busNode);

    void setOrientation(Orientation orientation);

    Type getType();
}
