/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.commons.jaxb.JaxbUtil;
import com.powsybl.substationdiagram.model.*;
import generated.EdgePattern;
import generated.NodePattern;
import generated.Patterns;
import generated.Patterns.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PatternCellDetector implements CellDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatternCellDetector.class);

    private Patterns patterns;

    /**
     * Constructor using file name
     * @param patternFileName file name of the file describing patterns
     */
    public PatternCellDetector(String patternFileName) {
        this.patterns = JaxbUtil.unmarchallReader(Patterns.class,
                                             new InputStreamReader(
                                                     this.getClass().getResourceAsStream(patternFileName)));
    }

    /**
     * Constructor using patterns data
     * @param patterns patterns
     */
    public PatternCellDetector(Patterns patterns) {
        this.patterns = Objects.requireNonNull(patterns);
    }

    /**
     * Detect and create the cells of a graph using patterns
     * <p>
     * graph of the current substation
     */
    @Override
    public void detectCells(Graph graph) {
        LOGGER.info("Detecting cells from patterns...");

        List<Node> nodesHandled = new ArrayList<>();
        Map<Cell, Pattern> cellPattern = new HashMap<>();

        for (Patterns.Pattern p : patterns.getPattern()) {
            searchPatternGraph(p, graph, nodesHandled, cellPattern);
        }

        // Display the cells created and set corresponding cell for every nodes
        for (Cell cell : graph.getCells()) {
            Pattern pattern = cellPattern.get(cell);
            LOGGER.info("Cell with pattern #{}", pattern.getId());
            for (Node n : cell.getNodes()) {
                LOGGER.trace("    Node : {} ; {}", n.getId(), n.getComponentType());
                if (!pattern.getKind().equals("pontage") || !(n instanceof FicticiousNode)) {
                    n.setCell(cell);
                }
            }
            if (pattern.getKind().equals("pontage")) {
                List<Node> shuntNodes = cell.getNodes()
                        .stream()
                        .filter(node -> node.getListNodeAdjInCell(cell).count() == 1).collect(Collectors.toList());
                shuntNodes.forEach(node -> node.setType(Node.NodeType.SHUNT));
            }
        }
        graph.getCells().stream()
                .filter(cell -> cell.getType() == Cell.CellType.SHUNT)
                .forEach(Cell::setBridgingCellsFromShuntNodes);

        graph.logCellDetectionStatus();
    }

    private static void setKindFromPattern(Pattern pattern, Cell cell) {
        switch (pattern.getKind()) {
            case "intern":
                cell.setType((cell.getNodes().size() == 3) ? Cell.CellType.INTERNBOUND : Cell.CellType.INTERN);
                break;
            case "extern":
                cell.setType(Cell.CellType.EXTERN);
                break;
            case "pontage":
                cell.setType(Cell.CellType.SHUNT);
                break;
            default:
                cell.setType(Cell.CellType.UNDEFINED);
        }
    }

    /**
     * Search a pattern in a graph
     *
     * @param p            pattern we are searching
     * @param graph        graph we are using
     * @param nodesHandled list of nodes we already find in a precedent pattern
     */
    private void searchPatternGraph(Pattern p, Graph graph, List<Node> nodesHandled,
                                    Map<Cell, Pattern> cellPattern) {
        LOGGER.trace("Searching pattern : {}", p.getId());
        Map<String, NodePattern> mapNodes = listNodeToMap(p);
        List<Node> nodeToSearch = new ArrayList<>(graph.getNodes());
        nodeToSearch.removeAll(nodesHandled);
        bindNodeAdj(p, mapNodes);
        if (!p.getNode().isEmpty()) {
            for (Node nG : nodeToSearch) {
                List<Node> nodesCell = new ArrayList<>();
                if (check(p.getNode().get(0), nG, new ArrayList<>(), nodesHandled, nodesCell)) {
                    Cell c = p.getKind().equals("intern") ? new InternCell(graph) : new Cell(graph);
                    cellPattern.put(c, p);
                    setKindFromPattern(p, c);
                    c.setNodes(nodesCell);
                    graph.addCell(c);
                    p.setOccurence(p.getOccurence() + 1);
                }
            }
        }
    }

    /**
     * Recursive way of finding pattern starting from a node of the pattern and
     * a node of the graph
     *
     * @param nP            Current node of the pattern
     * @param nG            Current node of the pattern
     * @param nodesPHandled Nodes of the pattern already visited
     * @param nodesGHandled Nodes of the graph already visited and considered in the
     *                      pattern
     * @return true if we match a entire pattern, false otherwise
     */
    private boolean check(NodePattern nP, Node nG, List<NodePattern> nodesPHandled, List<Node> nodesGHandled,
                          List<Node> nodesCell) {
        LOGGER.trace("Check nodes : {} ; {}", nP.getKind(), nG.getComponentType());
        boolean checking = false;

        List<Node> nodesGParents = new ArrayList<>(nodesGHandled);
        List<NodePattern> nodesPParents = new ArrayList<>(nodesPHandled);

        if (nP.getKind().equals("Cell") && nodesGHandled.contains(nG) && !nodesCell.contains(nG)) {
            nodesCell.add(nG);
            return true;
        }

        if (!nP.isShuntPossible() && nP.getListNodeAdj().size() != nG.getAdjacentNodes().size()) {
            return false;
        } else {
            if ((nP.getKind().equals("EQ") && (nG instanceof FeederNode) && !(nG instanceof BusNode))
                    || ((nP.getKind().equals("Switch")) && (nG instanceof SwitchNode))
                    || (nP.getKind().equals("BusbarSection") && (nG instanceof BusNode))
                    || (nP.getKind().equals("Node") && (nG instanceof FicticiousNode))) {
                nodesCell.add(nG);
                // We reach the end of the branch and it matches
                if (nP.getKind().equals("BusbarSection")) {
                    return true;
                }
                // We processed this nodes for the pattern and graph
                nodesPHandled.add(nP);
                nodesGHandled.add(nG);

                // Create the possible combination between outgoing nodes for pattern and graph
                List<Integer> data = new ArrayList<>();
                for (int j = 0; j < nG.getAdjacentNodes().size(); j++) {
                    data.add(j);
                }
                List<List<Integer>> listComb = new ArrayList<>();
                combination(listComb, data, nP.getListNodeAdj().size(), 0, new int[nP.getListNodeAdj().size()]);
                LOGGER.trace("Nb combination a tester : {}", listComb.size());

                // Test all combinations and return true if at least one is working
                for (List<Integer> l : listComb) {
                    checking = true;
                    int nbOfRecursivCall = 0;
                    int nbNodeAlreadyVisited = 0;

                    for (int i = 0; i < nP.getListNodeAdj().size(); i++) {
                        // check potential nodes are'nt already processed
                        if (nodesPHandled.contains(nP.getListNodeAdj().get(i))) {
                            nbNodeAlreadyVisited++;
                        }
                        if (!(nodesPHandled.contains(nP.getListNodeAdj().get(i)))
                                && (nP.getListNodeAdj().get(i).getKind().equals("Cell")
                                || !(nodesGHandled.contains(nG.getAdjacentNodes().get(l.get(i)))))) {
                            checking = checking && check(nP.getListNodeAdj().get(i),
                                                         nG.getAdjacentNodes().get(l.get(i)),
                                                         nodesPHandled, nodesGHandled, nodesCell);
                            nbOfRecursivCall++;
                        }
                    }

                    if (nbOfRecursivCall != (nP.getListNodeAdj().size() - nbNodeAlreadyVisited)) {
                        // We did'nt validate the combination
                        checking = false;
                    }
                    if (checking) {
                        // If the current combination work, we don't need to test the others
                        break;
                    }
                }
                if (!checking) {
                    nodesCell.remove(nG);
                    nodesGHandled.clear();
                    nodesPHandled.clear();
                    nodesGHandled.addAll(nodesGParents);
                    nodesPHandled.addAll(nodesPParents);
                }
                return checking;
            }
        }
        return false;
    }

    private void combination(List<List<Integer>> listComb, List<Integer> data, int size, int index, int[] l) {
        if (index == size) {
            List<Integer> nL = new ArrayList<>();
            for (int j : l) {
                nL.add(j);
            }
            listComb.add(nL);
            return;
        }

        for (int i : data) {
            l[index] = i;
            List<Integer> remainingData = new ArrayList<>(data);
            remainingData.remove(Integer.valueOf(i));
            combination(listComb, remainingData, size, index + 1, l);
        }

    }

    /**
     * Allow to see connected nodes from the current one.
     */
    private void bindNodeAdj(Pattern p, Map<String, NodePattern> mapNodes) {
        for (NodePattern n : p.getNode()) {
            n.getListNodeAdj().clear();
        }
        for (EdgePattern e : p.getEdge()) {
            mapNodes.get(e.getSource()).getListNodeAdj().add(mapNodes.get(e.getTarget()));
            mapNodes.get(e.getTarget()).getListNodeAdj().add(mapNodes.get(e.getSource()));
        }
    }

    /**
     * Return a map of the meta data nodes <id,Node>
     */
    private Map<String, NodePattern> listNodeToMap(Pattern pat) {
        Map<String, NodePattern> mapNodes = new HashMap<>();
        for (NodePattern n : pat.getNode()) {
            mapNodes.put(n.getId(), n);
        }
        return mapNodes;
    }

    public void patternStats() {
        LOGGER.info("Statistiques des patterns :");
        for (Pattern p : patterns.getPattern()) {
            LOGGER.info("-Pattern {}", p.getId());
            LOGGER.info("-Occurence {}", p.getOccurence());
        }
    }
}
