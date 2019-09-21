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
import java.util.*;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */

public interface Cell {
    public enum CellType {
        INTERN, EXTERN, SHUNT
    }

    public void addNodes(Collection<Node> nodesToAdd);

    public List<Node> getNodes();

    public void removeAllNodes(List<Node> nodeToRemove);

    public void setNodes(List<Node> nodes);

    public void setType(CellType type);

    public CellType getType();

    public Block getRootBlock();

    public void setRootBlock(Block rootBlock);

    public int getNumber();

    public void calculateCoord(LayoutParameters layoutParam);

    public void writeJson(JsonGenerator generator) throws IOException;

    public String getFullId();

    public Graph getGraph();
}
