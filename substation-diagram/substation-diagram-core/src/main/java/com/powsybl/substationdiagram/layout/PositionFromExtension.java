/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.substationdiagram.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * A {@link PositionFinder} which allocates positions according to position information defined
 * in feeder nodes.
 *
 * //TODO: rename ? actually it is not directly based on extensions, but on information contained in Nodes
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PositionFromExtension implements PositionFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionFromExtension.class);

    private static final BusCell.Direction DEFAULTDIRECTION = BusCell.Direction.TOP;

    /**
     * Builds the layout of the bus nodes, and organises cells (order and directions)
     */
    @Override
    public void buildLayout(Graph graph) {
        gatherLayoutExtensionInformation(graph);
        List<ExternalCell> problematicCells = graph.getCells().stream()
                .filter(cell -> cell.getType().equals(Cell.CellType.EXTERNAL))
                .map(ExternalCell.class::cast)
                .filter(cell -> cell.getOrder() == -1).collect(Collectors.toList());
        if (!problematicCells.isEmpty()) {
            LOGGER.info("Unable to build the layout only with Extension\nproblematic cells :");
            problematicCells.forEach(cell -> LOGGER
                    .info("Cell Nb : {}, Order : {}, Type : {}",
                            cell.getNumber(),
                            cell.getOrder(),
                            cell.getType()));
            return;
        }
        graph.setMaxBusPosition();
        forceSameOrientationForShuntedCell(graph);
    }

    private void gatherLayoutExtensionInformation(Graph graph) {
        graph.getNodes().stream()
                .filter(node -> node.getType() == Node.NodeType.FEEDER)
                .map(FeederNode.class::cast)
                .forEach(feederNode -> {
                    ExternalCell cell = (ExternalCell) feederNode.getCell();
                    cell.setDirection(
                            feederNode.getDirection() == BusCell.Direction.UNDEFINED ? DEFAULTDIRECTION : feederNode.getDirection());
                    cell.setOrder(feederNode.getOrder());
                });
        graph.getCells().stream().filter(cell -> cell.getType() == Cell.CellType.EXTERNAL).map(ExternalCell.class::cast)
                .forEach(ExternalCell::orderFromFeederOrders);
    }

    private void forceSameOrientationForShuntedCell(Graph graph) {
        for (Cell cell : graph.getCells().stream()
                .filter(c -> c.getType() == Cell.CellType.SHUNT).collect(Collectors.toList())) {
            List<Node> shNodes = cell.getNodes().stream()
                    .filter(node -> node.getType() == Node.NodeType.SHUNT).collect(Collectors.toList());
            ((ExternalCell) shNodes.get(1).getCell()).setDirection(
                    ((ExternalCell) shNodes.get(0).getCell()).getDirection());
        }
    }
}
