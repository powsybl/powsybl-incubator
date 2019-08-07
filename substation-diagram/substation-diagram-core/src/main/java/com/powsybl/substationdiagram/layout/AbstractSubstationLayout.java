/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import java.util.Objects;

import com.powsybl.commons.PowsyblException;
import com.powsybl.substationdiagram.model.BusCell;
import com.powsybl.substationdiagram.model.Coord;
import com.powsybl.substationdiagram.model.ExternCell;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.SubstationGraph;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public abstract class AbstractSubstationLayout implements SubstationLayout {

    protected SubstationGraph graph;
    protected VoltageLevelLayoutFactory vLayoutFactory;

    public AbstractSubstationLayout(SubstationGraph graph, VoltageLevelLayoutFactory vLayoutFactory) {
        this.graph = Objects.requireNonNull(graph);
        this.vLayoutFactory = Objects.requireNonNull(vLayoutFactory);
    }

    @Override
    public void run(LayoutParameters layoutParameters) {
        double graphX = layoutParameters.getHorizontalSubstationPadding();
        double graphY = layoutParameters.getVerticalSubstationPadding();

        for (Graph vlGraph : graph.getNodes()) {
            vlGraph.setX(graphX);
            vlGraph.setY(graphY);

            // Calculate the objects coordinates inside the voltageLevel graph
            VoltageLevelLayout vLayout = vLayoutFactory.create(vlGraph);
            vLayout.run(layoutParameters);

            // Calculate the coordinate of the voltageLevel graph inside the substation graph
            Coord posVLGraph = calculateCoordVoltageLevel(layoutParameters, vlGraph);

            graphX += posVLGraph.getX() + getHorizontalSubstationPadding(layoutParameters);
            graphY += posVLGraph.getY() + getVerticalSubstationPadding(layoutParameters);
        }
    }

    protected abstract Coord calculateCoordVoltageLevel(LayoutParameters layoutParameters, Graph vlGraph);

    protected abstract double getHorizontalSubstationPadding(LayoutParameters layoutParameters);

    protected abstract double getVerticalSubstationPadding(LayoutParameters layoutParameters);

    protected BusCell.Direction getNodeDirection(Node node, int nb) {
        if (node.getType() != Node.NodeType.FEEDER) {
            throw new PowsyblException("Node " + nb + " is not a feeder node");
        }
        BusCell.Direction dNode = node.getCell() != null ? ((ExternCell) node.getCell()).getDirection() : BusCell.Direction.TOP;
        if (dNode != BusCell.Direction.TOP && dNode != BusCell.Direction.BOTTOM) {
            throw new PowsyblException("Node " + nb + " cell direction not TOP or BOTTOM");
        }
        return dNode;
    }

}
