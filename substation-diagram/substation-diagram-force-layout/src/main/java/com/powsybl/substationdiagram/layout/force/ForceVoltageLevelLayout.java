/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout.force;

import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.VoltageLevelLayout;
import com.powsybl.substationdiagram.model.BusNode;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.graph.impl.EdgeImpl;
import org.gephi.graph.impl.GraphModelImpl;
import org.gephi.graph.impl.NodeImpl;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;

import java.util.Objects;
import java.util.Random;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ForceVoltageLevelLayout implements VoltageLevelLayout {

    private final Random random = new Random();

    private final Graph graph;

    public ForceVoltageLevelLayout(Graph graph) {
        this.graph = Objects.requireNonNull(graph);
    }

    @Override
    public void run(LayoutParameters parameters) {
        ForceAtlas2 forceAtlas2 = new ForceAtlas2Builder()
                .buildLayout();
        GraphModel graphModel = new GraphModelImpl();
        UndirectedGraph undirectedGraph = graphModel.getUndirectedGraph();
        for (Node node : graph.getNodes()) {
            if (node instanceof BusNode) {
                ((BusNode) node).setPxWidth(50);
            }
            NodeImpl n = new NodeImpl(node.getId());
            n.setPosition(random.nextFloat() * 1000, random.nextFloat() * 1000);
            undirectedGraph.addNode(n);
        }
        for (Edge edge : graph.getEdges()) {
            NodeImpl node1 = (NodeImpl) undirectedGraph.getNode(edge.getNode1().getId());
            NodeImpl node2 = (NodeImpl) undirectedGraph.getNode(edge.getNode2().getId());
            undirectedGraph.addEdge(new EdgeImpl(edge.toString(), node1, node2, 0, 1, false));
        }
        forceAtlas2.setGraphModel(graphModel);
        forceAtlas2.resetPropertiesValues();
        forceAtlas2.setAdjustSizes(true);
        forceAtlas2.setOutboundAttractionDistribution(false);
        forceAtlas2.setEdgeWeightInfluence(1.5d);
        forceAtlas2.setGravity(10d);
        forceAtlas2.setJitterTolerance(.02);
        forceAtlas2.setScalingRatio(15.0);
        forceAtlas2.initAlgo();
        int maxSteps = 1000;
        for (int i = 0; i < maxSteps && forceAtlas2.canAlgo(); i++) {
            forceAtlas2.goAlgo();
        }
        forceAtlas2.endAlgo();
        for (Node node : graph.getNodes()) {
            org.gephi.graph.api.Node n = undirectedGraph.getNode(node.getId());
            node.setX(n.x());
            node.setY(n.y());
        }
    }
}
