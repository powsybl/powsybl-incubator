/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.powsybl.cgmes.iidm.extensions.dl.NetworkDiagramData;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.SubstationLayout;
import com.powsybl.substationdiagram.model.BusCell.Direction;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Side;
import com.powsybl.substationdiagram.model.SubstationGraph;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesSubstationLayout extends AbstractCgmesLayout implements SubstationLayout {

    private static final Logger LOG = LoggerFactory.getLogger(CgmesSubstationLayout.class);

    private final SubstationGraph graph;

    public CgmesSubstationLayout(SubstationGraph graph) {
        Objects.requireNonNull(graph);
        for (Graph vlGraph : graph.getNodes()) {
            removeFictitiousNodes(vlGraph);
        }
        fixTransformersLabel = true;
        this.graph = graph;
    }

    @Override
    public void run(LayoutParameters layoutParam) {
        String diagramName = layoutParam.getDiagramName();
        if (diagramName == null) {
            LOG.warn("layout parameter diagramName not set: CGMES-DL layout will not be applied");
        } else {
            Network network = graph.getSubstation().getNetwork();
            if (NetworkDiagramData.containsDiagramName(network, diagramName)) {
                LOG.info("Applying CGMES-DL layout to network {}, substation {}, diagram name {}", network.getId(), graph.getSubstation().getId(), diagramName);
                for (Graph vlGraph : graph.getNodes()) {
                    setNodeCoordinates(vlGraph, diagramName);
                }
                for (Graph vlGraph : graph.getNodes()) {
                    vlGraph.getNodes().forEach(node -> shiftNodeCoordinates(node, layoutParam.getScaleFactor()));
                }
                if (layoutParam.getScaleFactor() != 1) {
                    for (Graph vlGraph : graph.getNodes()) {
                        vlGraph.getNodes().forEach(node -> scaleNodeCoordinates(node, layoutParam.getScaleFactor()));
                    }
                }
            } else {
                LOG.warn("diagram name {} not found in network: CGMES-DL layout will not be applied to network {}, substation {}", diagramName, network.getId(), graph.getSubstation().getId());
            }
        }
    }

    @Override
    public List<Double> calculatePolylineSnakeLine(LayoutParameters layoutParam, Edge edge,
            Map<Direction, Integer> nbSnakeLinesTopBottom, Map<Side, Integer> nbSnakeLinesLeftRight,
            Map<String, Integer> nbSnakeLinesBetween, Map<String, Integer> nbSnakeLinesBottomVL,
            Map<String, Integer> nbSnakeLinesTopVL) {
        return Collections.emptyList();
    }

}
