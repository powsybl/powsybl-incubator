/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.substationdiagram.library.ComponentType;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class Fictitious3WTNode extends FictitiousNode {

    private final ThreeWindingsTransformer transformer;

    public Fictitious3WTNode(Graph graph, String id, ThreeWindingsTransformer transformer) {
        super(graph, id, ComponentType.THREE_WINDINGS_TRANSFORMER);
        this.transformer = transformer;
    }

    public ThreeWindingsTransformer getTransformer() {
        return transformer;
    }
}
