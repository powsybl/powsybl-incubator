/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.substationdiagram.postprocessor;

import com.google.auto.service.AutoService;
import com.powsybl.substationdiagram.model.Graph;

import java.util.Objects;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@AutoService(GraphBuildPostProcessor.class)
public class GraphBuildPostProcessorStub implements GraphBuildPostProcessor {
    private static final String ID = "PostProcessorStub";

    public String getId() {
        return ID;
    }

    public void addNode(Graph graph) {
        Objects.requireNonNull(graph);

        // this stub does nothing
    }
}
