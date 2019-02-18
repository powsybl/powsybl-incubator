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

/**
 * Contain function to dispose components of cells based on Hierarchical Layout
 *
 * @author Jeanson Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 */
public class CellBlockDecomposer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CellBlockDecomposer.class);

    /**
     * Search layout.block and build layout.block hierarchy by merging blocks together; also
     * list blocks connected to busbar
     *
     * @param cell cell we are working on
     */
    public void determineBlocks(Cell cell) {
        if (cell.getType() == Cell.CellType.INTERNBOUND || cell.getType() == Cell.CellType.SHUNT) {
            determineSingularCell(cell);
        } else {
            determineComplexCell(cell);
        }
    }

    private void determineSingularCell(Cell cell) {
        List<PrimaryBlock> blocksConnectedToBusbar = new ArrayList<>();
        PrimaryBlock bpy = new PrimaryBlock(cell.getNodes());
        bpy.setCell(cell);
        if (cell.getType() == Cell.CellType.INTERNBOUND) {
            blocksConnectedToBusbar.add(bpy);
        }
        cell.blocksSetting(bpy, blocksConnectedToBusbar);
    }

    private void determineComplexCell(Cell cell) {

        List<PrimaryBlock> blocksConnectedToBusbar = new ArrayList<>();
        List<Node> alreadyTreated = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        Node currentNode = cell.getBusbars().get(0);

        // Search all primary blocks
        currentNode.getListNodeAdjInCell(cell).forEach(n -> {
            if (!alreadyTreated.contains(n)) {
                List<Node> blockNodes = new ArrayList<>();
                blockNodes.add(currentNode);
                rElaboratePrimaryBlocks(cell, n, currentNode, alreadyTreated, blockNodes, blocks);
            }
        });

        // Search all blocks connected to a busbar inside the primary blocks list
        for (Block b : blocks) {
            b.setCell(cell);
            if (b.getStartingNode().getType() == Node.NodeType.BUS) {
                b.setBusNode((BusNode) b.getStartingNode());
                blocksConnectedToBusbar.add((PrimaryBlock) b);
            }
        }

        //put blocks with busNode at the end ==> this enables to foster chaining over parallelization (for internCell)
        List<Block> organisedBlocks = new ArrayList<>();
        blocks.forEach(b -> addBlockOrganised(organisedBlocks, b));

        // Merge blocks to obtain a hierarchy of blocks
        while (organisedBlocks.size() != 1) {
            searchParallelMerge(organisedBlocks, cell);
            searchChainMerge(organisedBlocks, cell);
        }
        cell.blocksSetting(organisedBlocks.get(0), blocksConnectedToBusbar);
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
    private void searchChainMerge(List<Block> blocks, Cell cell) {
        boolean chainEnded = false;
        int i = 0;
        while (i < blocks.size() && !chainEnded) {
            for (int j = i + 1; j < blocks.size(); j++) {
                Block b1 = blocks.get(i);
                Block b2 = blocks.get(j);
                LOGGER.trace(" Blocks compared : {} & {}", b1, b2);
                Node commonNode = compareBlockPath(b1, b2);
                LOGGER.trace(" Common node : {}", commonNode);
                if (commonNode != null
                        && ((FicticiousNode) commonNode).getCardinality()
                        == (b1.getCardinality(commonNode) + b2.getCardinality(commonNode))) {
                    SerialBlock b = new SerialBlock(b1, b2, commonNode);
                    b1.setParentBlock(b);
                    b2.setParentBlock(b);
                    blocks.remove(b1);
                    blocks.remove(b2);
                    b.setCell(cell);
                    blocks.add(b);
                    chainEnded = true;
                    break;
                }
            }
            i++;
        }
    }

    /**
     * Search possibility to merge some blocks into a parallel layout.block and do the merging
     *
     * @param blocks list of blocks we can merge
     * @param cell   current cell
     */
    private void searchParallelMerge(List<Block> blocks, Cell cell) {
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
            ParallelBlock bPar = new ParallelBlock(blocksBundle);
            bPar.setCell(cell);
            blocks.add(bPar);
        }
    }

    /**
     * Compare two blocks to see if they can be consecutive
     *
     * @param block1 layout.block
     * @param block2 layout.block
     * @return the common node between the 2 blocks, null otherwise
     */
    private Node compareBlockPath(Block block1, Block block2) {
        Node s1 = block1.getStartingNode();
        Node e1 = block1.getEndingNode();
        Node s2 = block2.getStartingNode();
        Node e2 = block2.getEndingNode();

        if ((s1.getType() == Node.NodeType.FICTITIOUS || s1.getType() == Node.NodeType.SHUNT) && (s1.equals(
                s2) || s1.equals(e2))) {
            return s1;
        }
        if ((e1.getType() == Node.NodeType.FICTITIOUS || e1.getType() == Node.NodeType.SHUNT) && (e1.equals(
                s2) || e1.equals(e2))) {
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
     * a primary layout.block is made of BUS|FICTICIOUS|FEEDER|SHUNT - n* SWITCH - BUS|FICTICIOUS|FEEDER|SHUNT
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
        while (currentNode2.getType() == Node.NodeType.SWITCH || currentNode2.getType() == Node.NodeType.FICTITIOUS_SWITCH) {
            Node nextNode = currentNode2.getAdjacentNodes().get(
                    currentNode2.getAdjacentNodes().get(0).equals(parentNode2) ? 1 : 0);
            parentNode2 = currentNode2;
            currentNode2 = nextNode;
            if (currentNode2.getType() != Node.NodeType.BUS) {
                alreadyTreated.add(currentNode2);
            }
            blockNodes.add(currentNode2);
        }
        PrimaryBlock b = new PrimaryBlock(blockNodes);
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
