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
public class FicticiousNode extends Node {

    public FicticiousNode(Graph graph, String id, boolean isFictitiousSwitch) {
        super(isFictitiousSwitch ? NodeType.FICTITIOUS_SWITCH : NodeType.FICTITIOUS, id, id, ComponentType.NODE, graph, null);
    }

    public FicticiousNode(Graph graph, String id) {
        this(graph, id, false);
    }

    public int getCardinality() {
        return this.getAdjacentNodes().size() - (getType() == NodeType.SHUNT ? 1 : 0);
    }
}
