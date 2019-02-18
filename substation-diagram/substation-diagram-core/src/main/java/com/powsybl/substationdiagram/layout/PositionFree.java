/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.substationdiagram.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jeanson Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PositionFree implements PositionFinder {
    private Map<BusNode, Integer> nodeToNb;
    private Map<BusConnectionPattern, List<Cell>> bcpToCell;


    @JsonIgnore
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionFree.class);
    private static final Cell.Direction DEFAULTDIRECTION = Cell.Direction.TOP;

    public PositionFree() {
        nodeToNb = new HashMap<>();
        bcpToCell = new HashMap<>();
    }

    @Override
    public void buildLayout(Graph graph) {
        LOGGER.info("start BuildLayout");
//        initiateBusPosition(graph);
        initBusPatternConnectionToCell(graph);
        newStructuralPosition();
        initiateFeederPosition(graph);
        graph.setMaxBusPosition();

    }


    private void initiateBusPosition(Graph graph) {
        int i = 1;
        for (BusNode n : graph.getNodeBuses()
                .stream()
                .sorted(Comparator.comparing(BusNode::getId))
                .collect(Collectors.toList())) {
            nodeToNb.put(n, i);
            i++;
        }
    }

    private void initiateFeederPosition(Graph graph) {
        int i = 0;
        for (FeederNode feederNode : graph.getNodes().stream()
                .filter(node -> node.getType() == Node.NodeType.FEEDER)
                .map(FeederNode.class::cast)
                .sorted(Comparator.comparing(Node::getId))
                .collect(Collectors.toList())) {
            feederNode.getCell().setDirection(DEFAULTDIRECTION);
            feederNode.setOrder(12 * i);
            i++;
        }

        graph.getCells().forEach(Cell::orderFromFeederOrders);

    }

    private void initBusPatternConnectionToCell(Graph graph) {
        graph.getCells().stream()
                .filter(cell -> cell.getType() == Cell.CellType.EXTERN)
                .forEach(cell -> addBusNodeSet(cell.getBusbars(), cell));
    }

    private void addBusNodeSet(List<BusNode> busNodes, Cell cell) {
        BusConnectionPattern bcp = new BusConnectionPattern(busNodes);
        bcpToCell.putIfAbsent(bcp, new ArrayList<>());
        bcpToCell.get(bcp).add(cell);
    }

/*
    private identifyStructuringInternCell(Graph graph) {
        Set<BusConnectionPattern> bcpSet = new HashSet<>(bcpToCell.keySet());
        Set<CellIntern> structuringCellInterns
                = graph.getCells().stream()
                .filter(cell -> cell.getType() == Cell.CellType.INTERN)
                .map(CellIntern.class::cast)
                .collect(Collectors.toSet());

        Set<CellIntern> verticalCells = structuringCellInterns.stream()
                .filter(cellIntern -> new BusConnectionPattern(cellIntern.getBusbars())
                        .isIncludedIn(bcpSet))
                .collect(Collectors.toSet());
        structuringCellInterns.removeAll(verticalCells);

        Set<BusConnectionPattern> leftLegs = structuringCellInterns.stream()
                .map(c -> new BusConnectionPattern(c.getSideBusNode(Side.LEFT)))
                .filter(leftBcp -> !leftBcp.isIncludedIn(bcpSet))
                .collect(Collectors.toSet());
        bcpSet.addAll(leftLegs);
        Set<BusConnectionPattern> rightLegs = structuringCellInterns.stream()
                .map(c -> new BusConnectionPattern(c.getSideBusNode(Side.RIGHT)))
                .filter(rightBcp -> !rightBcp.isIncludedIn(bcpSet))
                .collect(Collectors.toSet());
        bcpSet.addAll(rightLegs);


    }
*/

    private void newStructuralPosition() {
        int i = 1;
        for (BusConnectionPattern bcp : bcpToCell.keySet()) {
            int j = 1;
            for (BusNode busNode : bcp.getBusNodeSet()) {
                if (busNode.getStructuralPosition() != null) {
                    busNode.setStructuralPosition(new Position(i, j));
                }
                j++;
            }
            i++;
        }
    }

    private class BusConnectionPattern {
        Set<BusNode> busNodeSet;

        BusConnectionPattern(List<BusNode> busNodees) {
            busNodeSet = new TreeSet<>(Comparator.comparingInt(n -> nodeToNb.get(n)));
            busNodeSet.addAll(busNodees);
        }

        boolean isIncludedIn(BusConnectionPattern bcp2) {
            Iterator<BusNode> it1 = busNodeSet.iterator();
            Iterator<BusNode> it2 = bcp2.getBusNodeSet().iterator();
            boolean match = true;
            while (it1.hasNext() && match) {
                BusNode n1 = it1.next();
                match = false;
                while (it2.hasNext() && !match) {
                    BusNode n2 = it2.next();
                    match = n2 == n1;
                }
            }
            return match;
        }

        BusConnectionPattern isIncludedIn(Set<BusConnectionPattern> busConnectionPatterns) {
            Iterator<BusConnectionPattern> it = bcpToCell.keySet().iterator();
            while (it.hasNext()) {
                BusConnectionPattern candidateBCPIncluser = it.next();
                if (isIncludedIn(candidateBCPIncluser)) {
                    return candidateBCPIncluser;
                }
            }
            return null;
        }


        Set<BusNode> getBusNodeSet() {
            return busNodeSet;
        }
    }
}



