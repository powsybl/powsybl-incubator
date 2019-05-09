/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.substationdiagram.library.ComponentType;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class FictitiousNode extends Node {

    private int shunts = 0;

    public FictitiousNode(Graph graph, String id) {
        super(NodeType.FICTITIOUS, id, id, ComponentType.NODE, true, graph);
    }

    public void addShunt() {
        shunts++;
    }

    @Override
    public boolean isShunt() {
        return shunts > 0;
    }

    public int getCardinality() {
        return this.getAdjacentNodes().size() - shunts;
    }
}
