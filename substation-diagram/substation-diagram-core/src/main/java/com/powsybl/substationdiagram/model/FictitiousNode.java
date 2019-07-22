/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.substationdiagram.library.ComponentType;

/**
 *
 * A fictitious node represent a node in the graph which has no associated equipment.
 * Typically, a fictitious node will be created between a disconnector and a breaker
 * which are directly connected.
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class FictitiousNode extends Node {

    public FictitiousNode(Graph graph, String id) {
        super(NodeType.FICTITIOUS, id, id, ComponentType.NODE, true, graph);
    }

    public int getCardinality() {
        return this.getAdjacentNodes().size() - (getType() == NodeType.SHUNT ? 1 : 0);
    }
}
