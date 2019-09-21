/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import java.util.*;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface BusCell extends Cell {

    public enum Direction {
        TOP, BOTTOM, FLAT, UNDEFINED
    }

    public List<BusNode> getBusNodes();

    public void blocksSetting(Block rootBlock, List<LegPrimaryBlock> primaryBlocksConnectedToBus);

    public List<LegPrimaryBlock> getPrimaryLegBlocks();

    public void blockSizing();

    public int newHPosition(int hPosition);

    public Direction getDirection();

    public void setDirection(Direction direction);

    public Position getMaxBusPosition();

}
