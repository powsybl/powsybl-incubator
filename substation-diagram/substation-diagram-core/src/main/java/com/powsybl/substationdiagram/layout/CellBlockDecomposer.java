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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contain function to dispose components of cells based on Hierarchical Layout
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CellBlockDecomposer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CellBlockDecomposer.class);

    /**
     * Search layout.block and build layout.block hierarchy by merging blocks together; also
     * list blocks connected to busbar
     *
     * @param cell Cell we are working on
     */
    public void determineBlocks(Cell cell) {
        if (cell.getType() == Cell.CellType.SHUNT) {
            determineShuntCellBlocks((ShuntCell) cell);
        } else {
            determineBusCellBlocks((BusCell) cell);
        }
    }

    private void determineBusCellBlocks(BusCell busCell) {
        if (busCell.getType() == Cell.CellType.INTERN && busCell.getNodes().size() == 3) {
            SwitchNode switchNode = (SwitchNode) busCell.getNodes().get(1);
            busCell.getGraph().extendSwitchBetweenBus(switchNode);
            List<Node> adj = switchNode.getAdjacentNodes();
            busCell.addNodes(adj);
            busCell.addNodes(adj.stream()
                    .flatMap(node -> node.getAdjacentNodes().stream())
                    .filter(node -> node != switchNode)
                    .collect(Collectors.toList()));
        }
        determineComplexCell(busCell);
    }

    private void determineShuntCellBlocks(ShuntCell shuntCell) {
        PrimaryBlock bpy = new PrimaryBlock(shuntCell.getNodes(), shuntCell);
        shuntCell.setRootBlock(bpy);
    }

    private void determineComplexCell(BusCell busCell) {
        List<Block> blocks = createPrimaryBlock(busCell);
        mergeBlocks(busCell, blocks);
    }

    private List<Block> createPrimaryBlock(BusCell busCell) {
        List<Node> alreadyTreated = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        Node currentNode = busCell.getBusNodes().get(0);

        // Search all primary blocks
        currentNode.getListNodeAdjInCell(busCell).forEach(n -> {
            if (!alreadyTreated.contains(n)) {
                List<Node> blockNodes = new ArrayList<>();
                blockNodes.add(currentNode);
                rElaboratePrimaryBlocks(busCell, n, currentNode, alreadyTreated, blockNodes, blocks);
            }
        });
        return blocks;
    }

    private void mergeBlocks(BusCell busCell, List<Block> blocks) {
        // Search all blocks connected to a busbar inside the primary blocks list
        List<PrimaryBlock> blocksConnectedToBusbar = blocks.stream()
                .filter(b -> b.getStartingNode().getType() == Node.NodeType.BUS)
                .map(PrimaryBlock.class::cast)
                .collect(Collectors.toList());

        //put blocks with busNode at the end ==> this enables to foster chaining over parallelization (for internCell)
        List<Block> organisedBlocks = new ArrayList<>();
        blocks.forEach(b -> addBlockOrganised(organisedBlocks, b));

        // Merge blocks to obtain a hierarchy of blocks
        while (organisedBlocks.size() != 1) {
            boolean merged = searchParallelMerge(organisedBlocks, busCell);
            merged |= searchSerialMerge(organisedBlocks, busCell);
            if (!merged) {
                LOGGER.warn("{} busCell, cannot merge any additional blocks, {} blocks remains", busCell.getType(), organisedBlocks.size());
                Block undefinedBlock = new UndefinedBlock(new ArrayList<>(organisedBlocks), busCell);
                organisedBlocks.clear();
                organisedBlocks.add(undefinedBlock);
                break;
            }
        }
        busCell.blocksSetting(organisedBlocks.get(0), blocksConnectedToBusbar);
    }

    private void addBlockOrganised(List<Block> blocks, Block b) {
        if (b.isEmbedingNodeType(Node.NodeType.BUS)) {
            blocks.add(b);
        } else {
            blocks.add(0, b);
        }
    }

    /**
     * Search possibility to merge two blocks into a chain layout.block and do the merging
     *
     * @param blocks list of blocks we can merge
     * @param cell   current cell
     */
    private boolean searchSerialMerge(List<Block> blocks, Cell cell) {
        boolean chainEnded = false;
        int i = 0;
        while (i < blocks.size() && !chainEnded) {
            for (int j = i + 1; j < blocks.size(); j++) {
                Block b1 = blocks.get(i);
                Block b2 = blocks.get(j);
                LOGGER.trace(" Blocks compared : {} & {}", b1, b2);
                Node commonNode = identifyCommonNode(b1, b2);
                LOGGER.trace(" Common node : {}", commonNode);
                if (commonNode != null
                        && ((FictitiousNode) commonNode).getCardinality()
                        == (b1.getCardinality(commonNode) + b2.getCardinality(commonNode))) {
                    SerialBlock b = new SerialBlock(b1, b2, cell);
                    blocks.remove(b1);
                    blocks.remove(b2);
                    blocks.add(b);
                    chainEnded = true;
                    break;
                }
            }
            i++;
        }
        return chainEnded;
    }

    /**
     * Search possibility to merge some blocks into a parallel layout.block and do the merging
     *
     * @param blocks list of blocks we can merge
     * @param cell   current cell
     */
    private boolean searchParallelMerge(List<Block> blocks, Cell cell) {
        List<List<Block>> blocksBundlesToMerge = new ArrayList<>();
        Node commonNode;
        int i = 0;
        while (i < blocks.size()) {
            List<Block> blocksBundle = new ArrayList<>();
            for (int j = i + 1; j < blocks.size(); j++) {
                commonNode = compareBlockParallel(blocks.get(i), blocks.get(j));
                if (commonNode != null) {
                    blocksBundle.add(blocks.get(j));
                }
            }
            if (blocksBundle.isEmpty()) {
                i++;
            } else {
                blocksBundle.add(blocks.get(i));
                blocks.removeAll(blocksBundle);
                blocksBundlesToMerge.add(blocksBundle);
            }
        }
        for (List<Block> blocksBundle : blocksBundlesToMerge) {
            ParallelBlock bPar = new ParallelBlock(blocksBundle, cell, true);
            blocks.add(bPar);
        }
        return !blocksBundlesToMerge.isEmpty();
    }

    /**
     * Compare two blocks to see if they can be consecutive
     *
     * @param block1 layout.block
     * @param block2 layout.block
     * @return the common node between the 2 blocks, null otherwise
     */
    private Node identifyCommonNode(Block block1, Block block2) {
        Node s1 = block1.getStartingNode();
        Node e1 = block1.getEndingNode();
        Node s2 = block2.getStartingNode();
        Node e2 = block2.getEndingNode();

        if ((s1.getType() == Node.NodeType.FICTITIOUS || s1.getType() == Node.NodeType.SHUNT)
                && (s1.equals(s2) || s1.equals(e2))) {
            return s1;
        }
        if ((e1.getType() == Node.NodeType.FICTITIOUS || e1.getType() == Node.NodeType.SHUNT)
                && (e1.equals(s2) || e1.equals(e2))) {
            return e1;
        }
        return null;
    }

    /**
     * Compare two blocks to see if they are parallel
     *
     * @param block1 layout.block
     * @param block2 layout.block
     * @return true if the two blocks are similar : same start and end
     */
    private Node compareBlockParallel(Block block1, Block block2) {
        Node s1 = block1.getStartingNode();
        Node e1 = block1.getEndingNode();
        Node s2 = block2.getStartingNode();
        Node e2 = block2.getEndingNode();

        if ((s1.checkNodeSimilarity(s2) && e1.checkNodeSimilarity(e2))
                || (s1.checkNodeSimilarity(e2) && e1.checkNodeSimilarity(s2))) {
            if (s1.equals(s2) || s1.equals(e2)) {
                return s1;
            } else {
                return e1;
            }
        } else {
            return null;
        }
    }

    /**
     * Search for primary layout.block
     * a primary layout.block shall have the following pattern :
     * BUS|FICTICIOUS|FEEDER|SHUNT - n * SWITCH - BUS|FICTICIOUS|FEEDER|SHUNT
     *
     * @param cell           cell
     * @param currentNode    currentnode
     * @param alreadyTreated alreadyTreated
     * @param blockNodes     blockNodes
     * @param blocks         blocks
     */
    private void rElaboratePrimaryBlocks(Cell cell, Node currentNode, Node parentNode,
                                         List<Node> alreadyTreated,
                                         List<Node> blockNodes,
                                         List<Block> blocks) {
        Node currentNode2 = currentNode;
        Node parentNode2 = parentNode;

        alreadyTreated.add(currentNode2);
        blockNodes.add(currentNode2);
        while (currentNode2.getType() == Node.NodeType.SWITCH) {
            Node nextNode = currentNode2.getAdjacentNodes().get(
                    currentNode2.getAdjacentNodes().get(0).equals(parentNode2) ? 1 : 0);
            parentNode2 = currentNode2;
            currentNode2 = nextNode;
            if (currentNode2.getType() != Node.NodeType.BUS) {
                alreadyTreated.add(currentNode2);
            }
            blockNodes.add(currentNode2);
        }
        PrimaryBlock b = new PrimaryBlock(blockNodes, cell);
        blocks.add(b);
        // If we did'nt reach a Busbar, continue to search for other
        // blocks
        if (currentNode2.getType() != Node.NodeType.BUS) {
            Node finalCurrentNode = currentNode2;
            currentNode2.getListNodeAdjInCell(cell)
                    .filter(node -> !alreadyTreated.contains(node) && !blockNodes.contains(node))
                    .forEach(node -> {
                        List<Node> blockNode = new ArrayList<>();
                        blockNode.add(finalCurrentNode);
                        rElaboratePrimaryBlocks(cell, node, finalCurrentNode, alreadyTreated, blockNode, blocks);
                    });
        }
    }
}
