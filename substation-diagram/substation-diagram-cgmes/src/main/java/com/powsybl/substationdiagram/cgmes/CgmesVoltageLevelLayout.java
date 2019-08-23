/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.VoltageLevelLayout;
import com.powsybl.substationdiagram.model.Graph;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesVoltageLevelLayout extends AbstractCgmesLayout implements VoltageLevelLayout {

    private static final Logger LOG = LoggerFactory.getLogger(CgmesVoltageLevelLayout.class);

    private final Graph graph;

    public CgmesVoltageLevelLayout(Graph graph) {
        Objects.requireNonNull(graph);
        this.graph = removeFictitiousNodes(graph);
    }

    @Override
    public void run(LayoutParameters layoutParam) {
        LOG.info("Running CGMES Voltage Level Layout on voltage level {}", graph.getVoltageLevel().getId());
        setNodeCoordinates(graph);
        graph.getNodes().forEach(node -> shiftNodeCoordinates(node, layoutParam.getScaleFactor()));
        if (layoutParam.getScaleFactor() != 1) {
            graph.getNodes().forEach(node -> scaleNodeCoordinates(node, layoutParam.getScaleFactor()));
        }
    }

}
