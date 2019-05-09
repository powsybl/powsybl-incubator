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

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ImplicitCellDetector implements CellDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImplicitCellDetector.class);

    private static final Predicate<Node> SHUNT_EXPLORE_STOP_CRITERIA
            = n -> n.getType() == Node.NodeType.BUS || n.getType() == Node.NodeType.FEEDER || n.isShunt();

    private boolean removeUnnecessaryFictitiousNodes;
    private boolean substituteSingularFictitiousByFeederNode;

    public ImplicitCellDetector(boolean removeUnnecessaryFictitiousNodes, boolean substituteSingularFictitiousByFeederNode) {
        this.removeUnnecessaryFictitiousNodes = removeUnnecessaryFictitiousNodes;
        this.substituteSingularFictitiousByFeederNode = substituteSingularFictitiousByFeederNode;
    }

    public ImplicitCellDetector() {
        this(true, true);
    }


    /**
     * internCell detection : an internal cell is composed of nodes connecting BUSes without connecting Feeder.
     * genericDetectCell is used to detect cells exploring the graph and scanning exclusionTypes and stopTypes
     * <p>
     * *************INTERN CELL*******************
     * exclusion types = {FEEDER} : if a FEEDER type is reached it is not an INTERN CELL
     * stop the visit if reach a Bus : stopTypes = {BUS}
     * ***************EXTERN AND SHUNT CELLS******
     * detection : nodes connecting buses and departures without being in an internCell (previously allocated nodes)
     * exclusion types = {}
     * stop the visit if reach a Bus : stopTypes = {BUS,FEEDER} * @param graph g
     */
    @Override
    public void detectCells(Graph graph) {
        cleaning(graph);
        LOGGER.info("Detecting cells...");

        List<Node> allocatedNodes = new ArrayList<>();
        // **************INTERN CELL*******************
        Set<Node.NodeType> exclusionTypes = EnumSet.of(Node.NodeType.FEEDER);
        Set<Node.NodeType> stopTypes = EnumSet.of(Node.NodeType.BUS);
        genericDetectCell(graph, stopTypes, exclusionTypes, true, allocatedNodes);

        // ****************EXTERN AND SHUNT CELLS******
        stopTypes.add(Node.NodeType.FEEDER);
        genericDetectCell(graph, stopTypes, Collections.emptySet(), false, allocatedNodes);
        for (Cell cell : graph.getCells().stream()
                .filter(cell -> cell.getType() == Cell.CellType.UNDEFINED)
                .collect(Collectors.toList())) {

            //*****************EXTERN CELL
            if (!typeExternCellReturnFalseIfShunt(graph, cell)) {
                //*****************SHUNT CELL
                //in that case the cell is splitted into 2 EXTERN Cells and 1 SHUNT CELL
                detectAndTypeShunt(graph, cell);
            }
        }
        graph.getCells().forEach(Cell::getFullId);

        graph.logCellDetectionStatus();
    }

    private void cleaning(Graph graph) {
        graph.substituteFictitiousNodesMirroringBusNodes();
        if (removeUnnecessaryFictitiousNodes) {
            graph.removeUnnecessaryFictitiousNodes();
        }
        graph.extendFeederWithMultipleSwitches();
        graph.extendFirstOutsideNode();
        if (substituteSingularFictitiousByFeederNode) {
            graph.substituteSingularFictitiousByFeederNode();
        }
        graph.extendBreakerConnectedToBus();
        graph.extendFeederConnectedToBus();
    }

    /**
     * @param typeStops      is the types of node that stops the exploration
     * @param exclusionTypes is the types when reached considers the exploration unsuccessful
     * @param isCellIntern   when the exploration is for the identification of internCell enables to instanciate InternCell class instead of Cell
     * @param allocatedNodes is the list of nodes already allocated to a cell.
     **/
    private void genericDetectCell(Graph graph,
                                   Set<Node.NodeType> typeStops,
                                   Set<Node.NodeType> exclusionTypes,
                                   boolean isCellIntern,
                                   List<Node> allocatedNodes) {
        graph.getNodeBuses().forEach(bus -> {
            List<BusNode> visitedBus = new ArrayList<>();
            visitedBus.add(bus);
            bus.getAdjacentNodes().forEach(adj -> {
                List<Node> cellNodes = new ArrayList<>();
                List<Node> visitedNodes = new ArrayList<>(allocatedNodes);
                visitedNodes.addAll(visitedBus);
                boolean searchOK = rDelimitedExploration(adj, node -> typeStops.contains(node.getType()), exclusionTypes, cellNodes, visitedNodes);
                if (searchOK && !cellNodes.isEmpty()) {
                    cellNodes.add(adj);
                    cellNodes.add(bus);
                    Cell cell = isCellIntern ? new InternCell(graph) : new Cell(graph);
                    cell.setNodes(cellNodes);
                    allocatedNodes.addAll(cellNodes);
                    // remove the BusNodes from allocatedNode for a BusNode can be part of many cells
                    allocatedNodes.removeAll(
                            cellNodes.stream()
                                    .filter(node -> node.getType() == Node.NodeType.BUS)
                                    .collect(Collectors.toList()));
                }
            });
        });
    }

    /**
     * @param node           the starting point for the exploration
     * @param stopCriteria   stops exploration criteria
     * @param exclusionTypes is the types when reached considers the exploration unsuccessful
     * @param nodesResult    the resulting list of nodes
     * @param exploredNodes  nodes already visited
     * @return true if no exclusionType found
     **/
    private boolean rDelimitedExploration(Node node,
                                          Predicate<Node> stopCriteria,
                                          Set<Node.NodeType> exclusionTypes,
                                          List<Node> nodesResult,
                                          List<Node> exploredNodes) {

        if (exploredNodes.contains(node)) {
            return true;
        }
        exploredNodes.add(node);
        // the node match the pattern if all the branches from its adjacent nodes reaches a typeStop node without reaching an exclusionTypes node
        List<Node> nodesToVisit = new ArrayList<>(node.getAdjacentNodes());
        nodesToVisit.removeAll(exploredNodes);
        if (nodesToVisit.isEmpty()) {
            return true;
        }
        for (Node n : nodesToVisit) {
            if (exclusionTypes.contains(n.getType())) {
                return false;
            } else if (stopCriteria.test(n)) {
                nodesResult.add(n);
                exploredNodes.add(n);
            } else if (rDelimitedExploration(n, stopCriteria, exclusionTypes, nodesResult,
                    exploredNodes)) {
                nodesResult.add(n);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * set Cell.type to Extern if the cell analysed is an external one, and return true in that case, else false (suspected shunt)
     *
     * @param cell : the cell to analyse
     **/
    private boolean typeExternCellReturnFalseIfShunt(Graph graph, Cell cell) {
        /*Explore the graph of the candidate cell. Remove successively one node, assess if it splits the graph into n>1 branches
        if so, then check if each component is exclusively reaching FEEDER or exclusively reaching BUS
        And verify you have at least one of them
        Return true in that case else false meaning there is one shunt
        */
        for (Node n : cell.getNodes()) {
            List<Node> nodes = new ArrayList<>(cell.getNodes());
            nodes.remove(n);
            List<List<Node>> connexComponents = graph.getConnexComponents(nodes);
            if (checkExternComponents(connexComponents)) {
                cell.setType(Cell.CellType.EXTERN);
                return true;
            }
        }
        return false;
    }

    /**
     * @param connexComponents components partition to analyse
     * @return true if this partition reflects an extern cell
     */
    private boolean checkExternComponents(List<List<Node>> connexComponents) {
        if (connexComponents.size() > 1) {
            boolean hasDepartBranch = false;
            boolean hasBusBranch = false;
            boolean hasMixBranch = false;
            for (List<Node> nodesConnex : connexComponents) {
                List<Node.NodeType> types = nodesConnex.stream()
                        .map(Node::getType)
                        .distinct().filter(t -> t == Node.NodeType.FEEDER || t == Node.NodeType.BUS)
                        .collect(Collectors.toList());
                if (types.size() == 2) {
                    hasMixBranch = true;
                } else if (types.isEmpty()) {
                    return false;
                } else if (types.get(0).equals(Node.NodeType.FEEDER)) {
                    hasDepartBranch = true;
                } else {
                    hasBusBranch = true;
                }
            }
            return hasBusBranch && hasDepartBranch && !hasMixBranch;
        }
        return false;
    }

    /**
     * @param cell the nodes of a cell that is suppected to be a shunt
     **/
    private void detectAndTypeShunt(Graph graph, Cell cell) {

        List<Node> externalNodes = graph.getNodes()
                .stream()
                .filter(node -> !cell.getNodes().contains(node))
                .collect(Collectors.toList());

        for (Node n : cell.getNodes().stream()
                .filter(n -> n.getAdjacentNodes().size() > 2).collect(Collectors.toList())) {
            // optimisation : a Shunt node has necessarily 3 ore more adjacent nodes

            List<Node> cellNodesExtern1 = checkCandidateShuntNode(n, externalNodes);
            if (cellNodesExtern1 != null) {
                LOGGER.info("Found shunt at {}", n);

                // create the 1st new external cell
                cell.removeAllNodes(cellNodesExtern1.stream()
                        .filter(m -> !m.getType().equals(Node.NodeType.BUS))
                        .collect(Collectors.toList()));
                ((FictitiousNode) n).addShunt();
                Cell newExternCell1 = new Cell(graph);
                newExternCell1.setType(Cell.CellType.EXTERN);
                cellNodesExtern1.add(n);
                newExternCell1.setNodes(cellNodesExtern1);

                //create the shunt cell

                Cell shuntCell = createShuntCell(graph, n, cellNodesExtern1);

                // create the 2nd external cell
                List<Node> cellNodesExtern2 = cell.getNodes().stream()
                        .filter(node -> (!cellNodesExtern1.contains(node) || node.getType() == Node.NodeType.BUS)
                                && (!shuntCell.getNodes().contains(node) || node.isShunt()))
                        .collect(Collectors.toList());

                cellNodesExtern2.removeAll(cellNodesExtern2.stream()
                        .filter(node -> node.getType() == Node.NodeType.BUS
                                && node.getAdjacentNodes().stream().noneMatch(
                                cellNodesExtern2::contains))
                        .collect(Collectors.toList()));

                Cell newExternCell2 = new Cell(graph);
                newExternCell2.setType(Cell.CellType.EXTERN);
                newExternCell2.setNodes(cellNodesExtern2);

                graph.removeCell(cell);
                shuntCell.setBridgingCellsFromShuntNodes();

                detectAndTypeShunt(graph, newExternCell1);
                detectAndTypeShunt(graph, newExternCell2);

                break;
            }
        }
    }

    private List<Node> checkCandidateShuntNode(Node n, List<Node> externalNodes) {

        /*
        the node n is candidate to be a SHUNT node if there is
        (i) at least one branch exclusively reaching BUSes
        (ii) at least one branch exclusively reaching DEPARTs
        (iii) at least one branch reaching BUSes and DEPARTs (this branch would be a Shunt)
        In that case, the BUSes branches and DEPARTs Branches constitute an EXTERN Cell,
        and returned in the cellNodesExtern
         */

        List<Node> visitedNodes = new ArrayList<>(externalNodes);
        visitedNodes.add(n); //removal of the node to explore branches from it

        List<Node> cellNodesExtern = new ArrayList<>();
        boolean hasFeederBranch = false;
        boolean hasBusBranch = false;
        boolean hasMixBranch = false;

        List<Node> adjList = new ArrayList<>(n.getAdjacentNodes());
        adjList.removeAll(visitedNodes);
        for (Node adj : adjList) {
            if (!visitedNodes.contains(adj)) {
                List<Node> resultNodes = new ArrayList<>();
                rDelimitedExploration(adj,
                        SHUNT_EXPLORE_STOP_CRITERIA,
                        Collections.emptySet(),
                        resultNodes,
                        visitedNodes);
                resultNodes.add(adj);

                Set<Node.NodeType> types = resultNodes.stream() // what are the types of terminal node of the branch
                        .filter(SHUNT_EXPLORE_STOP_CRITERIA)
                        .map(Node::getType)
                        .collect(Collectors.toSet());

                if (!types.isEmpty()) {
                    if (types.size() > 1) {
                        hasMixBranch = true;
                    } else {
                        Node.NodeType type = types.iterator().next();
                        hasBusBranch |= type.equals(Node.NodeType.BUS);
                        hasFeederBranch |= type.equals(Node.NodeType.FEEDER);

                        if (type.equals(Node.NodeType.BUS) || type.equals(Node.NodeType.FEEDER)) {
                            cellNodesExtern.addAll(resultNodes);
                        }
                    }
                }
                visitedNodes.removeAll(resultNodes
                        .stream()
                        .filter(m -> m.getType().equals(Node.NodeType.BUS))
                        .collect(Collectors.toList()));

            }
        }
        return (hasBusBranch && hasFeederBranch && hasMixBranch) ? cellNodesExtern : null;
    }

    private Cell createShuntCell(Graph graph, Node n, List<Node> cellNodesExtern1) {
        List<Node> shuntCellNodes = new ArrayList<>();
        shuntCellNodes.add(n);
        Node currentNode = n.getAdjacentNodes().stream()
                .filter(node -> !cellNodesExtern1.contains(node))
                .findAny().orElse(null);
        if (currentNode != null) {
            while (currentNode.getAdjacentNodes().size() == 2) {
                shuntCellNodes.add(currentNode);
                currentNode = shuntCellNodes.contains(currentNode.getAdjacentNodes().get(0))
                        ? currentNode.getAdjacentNodes().get(1) : currentNode.getAdjacentNodes().get(0);
            }
            shuntCellNodes.add(currentNode);
            ((FictitiousNode) currentNode).addShunt();
        }
        Cell shuntCell = new Cell(graph); // the shunt branch is made of the remaining cells + the actual node n
        shuntCell.setType(Cell.CellType.SHUNT);
        shuntCell.setNodes(shuntCellNodes);
        return shuntCell;
    }
}

