/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class InternCell extends Cell {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternCell.class);

    private Block centralBlock;

    private Map<Side, Block> sideToBlock;

    private Map<Side, List<PrimaryBlock>> sideToConnectedBlocks;

    public InternCell(Graph graph) {
        super(graph, CellType.INTERN);
        centralBlock = null;
        sideToConnectedBlocks = new EnumMap<>(Side.class);
        sideToConnectedBlocks.put(Side.RIGHT, new ArrayList<>());
        sideToConnectedBlocks.put(Side.LEFT, new ArrayList<>());
        sideToBlock = new EnumMap<>(Side.class);
        setDirection(Direction.TOP);
    }

    public void postPositioningSettings() {
        identifyIfFlat();
        if (getType() == CellType.INTERNBOUND) {
            if (getDirection() == Direction.FLAT) {
                centralBlock = getRootBlock();
            } else {
                handleNonFlatInterbound();
            }
        } else {
            if (centralBlock != null) {
                refactorBlocks();
            }
        }
    }

    private void identifyIfFlat() {
        List<BusNode> buses = getBusNodes();
        if (buses.size() < 2) {
            return;
        }
        Position pos1 = buses.get(0).getStructuralPosition();
        Position pos2 = buses.get(1).getStructuralPosition();
        if (buses.size() == 2 && Math.abs(pos2.getH() - pos1.getH()) == 1
                && pos2.getV() == pos1.getV()) {
            setDirection(Direction.FLAT);
            getRootBlock().setOrientation(Orientation.HORIZONTAL);
        }
    }

    private void handleNonFlatInterbound() {
        PrimaryBlock initialRootBlock = (PrimaryBlock) getRootBlock();
        SwitchNode ns = (SwitchNode) (initialRootBlock.getNodes().get(1));
        graph.extendSwitchBetweenBus(ns);
        getPrimaryBlocksConnectedToBus().clear();
        Block block0 = createAdjacentPrimaryBlock(ns, 0);
        Block block1 = createAdjacentPrimaryBlock(ns, 1);
        fillInSideToBlock(block0, block1);
///*
//        if (block0.getBusNode().getStructuralPosition().getH() < block1.getBusNode().getStructuralPosition().getH()) {
//            sideToBlock.put(Side.LEFT, block0);
//            sideToBlock.put(Side.RIGHT, block1);
//        } else {
//            sideToBlock.put(Side.LEFT, block1);
//            sideToBlock.put(Side.RIGHT, block0);
//        }
//*/
        centralBlock = new PrimaryBlock(Arrays.asList(block0.getEndingNode(), ns, block1.getEndingNode()), this);
        setType(CellType.INTERN);
        refactorBlocks();
    }

    private Block createAdjacentPrimaryBlock(SwitchNode ns, int id) {
        FictitiousNode nf = (FictitiousNode) ns.getAdjacentNodes().get(id);
        Node fictSwitch = otherNodeFromAdj(nf, ns);
        BusNode bus = (BusNode) otherNodeFromAdj(fictSwitch, nf);
        PrimaryBlock bpy = new PrimaryBlock(Arrays.asList(new Node[]{bus, fictSwitch, nf}), this);
        bpy.setBusNode(bus);
        getPrimaryBlocksConnectedToBus().add(bpy);
        return bpy;
    }

    private Node otherNodeFromAdj(Node nodeOrigin, Node toBeOther) {
        List<Node> adj = nodeOrigin.getAdjacentNodes();
        return adj.get(0) == toBeOther ? adj.get(1) : adj.get(0);
    }

    /**
     * <pre>
     * the organisation of the block shall be
     *     a parallelBlock of - from left to right :
     *         1 chain block with:
     *             lowerBlock: 1 (parallelBlocks or one primaryBlock) LefttNode to busNodes on the right
     *             upperBlock: 1 central block (with startingNode = leftNode and endingNode = rightNode)
     *         blocks - one or more / whatever type from RightNode to busNodes
     * </pre>
     */
    public void rationalizeOrganization() {
        unRavelBlocks();
        if (centralBlock != null) {
            refactorBlocks();
        }
    }

    private void unRavelBlocks() {
        Block legBlock1 = null;
        Block legBlock2;
        List<Block> blocksToParalellize = new ArrayList<>();
/*
         Usually the rootBlock becomes a parallel block in an internCell considering
         - a BusNode is always becoming a startingNode
         - the merging process
         then the block that is a serial one is the one having the central block, the others constituting another "leg"

         One exception : it can be a serial block in case of a "one leg" intern cell on a single bus

*/
        if (getRootBlock().getType() == Block.Type.PARALLEL) {
            for (Block block : ((ParallelBlock) getRootBlock()).getSubBlocks()) {
                if (block instanceof SerialBlock && centralBlock == null) {
                    SerialBlock bc = (SerialBlock) block;
                    centralBlock = bc.getUpperBlock();
                    legBlock1 = bc.getLowerBlock();
                } else {
                    blocksToParalellize.add(block);
                }
            }
            if (blocksToParalellize.size() == 1) {
                legBlock2 = blocksToParalellize.get(0);
            } else {
                legBlock2 = new ParallelBlock(blocksToParalellize, this, true);
            }
        } else {
            legBlock2 = getRootBlock();
        }

        if (centralBlock == null) {
            // case of a one leg internCell.
            sideToBlock.put(Side.LEFT, legBlock2);
        } else if (legBlock1 != null) {
            fillInSideToBlock(legBlock1, legBlock2);
        }
    }

    private void fillInSideToBlock(Block block1, Block block2) {
        if (getOneNodeBusHPod(block1) > getOneNodeBusHPod(block2)) {
            sideToBlock.put(Side.LEFT, block2);
            sideToBlock.put(Side.RIGHT, block1);
        } else {
            sideToBlock.put(Side.LEFT, block1);
            sideToBlock.put(Side.RIGHT, block2);
        }
    }

    private void refactorBlocks() {
        SerialBlock bc = new SerialBlock(sideToBlock.get(Side.LEFT),
                centralBlock,
                sideToBlock.get(Side.LEFT).getEndingNode(), this);
        ParallelBlock bp = new ParallelBlock(Arrays.asList(bc, sideToBlock.get(Side.RIGHT)), this, false);
        if (getDirection() == Direction.FLAT) {
            bp.setOrientation(Orientation.HORIZONTAL);
        } else {
            bp.setOrientation(Orientation.VERTICAL);
            bc.setOrientation(Orientation.HORIZONTAL);
            bc.getLowerBlock().setOrientation(Orientation.VERTICAL);
        }

        blocksSetting(bp, getPrimaryBlocksConnectedToBus());
        identifyConnectedBlocks();
    }

    private int getOneNodeBusHPod(Block b) {
        Position structuralPos = ((BusNode) b.getStartingNode()).getStructuralPosition();
        return structuralPos == null ? -1 : structuralPos.getH();
    }

    private void identifyConnectedBlocks() {
        getPrimaryBlocksConnectedToBus().forEach(block -> {
            if (block.getNodes().contains(centralBlock.getStartingNode())) {
                sideToConnectedBlocks.get(Side.LEFT).add(block);
            } else if (block.getNodes().contains(centralBlock.getEndingNode())) {
                sideToConnectedBlocks.get(Side.RIGHT).add(block);
            }
        });
    }

    public void reverseCell() {
        if (getType() != CellType.INTERNBOUND) {
            Block swap = sideToBlock.get(Side.LEFT);
            sideToBlock.put(Side.LEFT, sideToBlock.get(Side.RIGHT));
            sideToBlock.put(Side.RIGHT, swap);
            refactorBlocks();
        }
    }

    @Override
    public void setNodes(List<Node> nodes) {
        super.setNodes(nodes);
        if (getNodes().size() == 3) {
            setType(CellType.INTERNBOUND);
        }
    }

    public int getSideHPos(Side side) {
        if (side == Side.LEFT) {
            return getRootBlock().getPosition().getH();
        }
        Position rightBlockPos = getSideToBlock(Side.RIGHT).getPosition();
        if (rightBlockPos.isAbsolute()) {
            return rightBlockPos.getH();
        }
        return getRootBlock().getPosition().getH() + rightBlockPos.getH();
    }

    public List<PrimaryBlock> getSideConnectedBlocks(Side side) {
        return sideToConnectedBlocks.get(side);
    }

    public Block getSideToBlock(Side side) {
        return sideToBlock.get(side);
    }

    public List<BusNode> getSideBusNodes(Side side) {
        if (getType() == CellType.INTERNBOUND) {
            Position pos0 = getBusNodes().get(0).getStructuralPosition();
            Position pos1 = getBusNodes().get(1).getStructuralPosition();
            if (pos0 == null || pos1 == null || pos0.getH() < pos1.getH()) {
                if (side == Side.LEFT) {
                    return new ArrayList<>(Collections.singletonList(getBusNodes().get(0)));
                } else {
                    return new ArrayList<>(Collections.singletonList(getBusNodes().get(1)));
                }
            }
            if (side == Side.LEFT) {
                return new ArrayList<>(Collections.singletonList(getBusNodes().get(1)));
            } else {
                return new ArrayList<>(Collections.singletonList(getBusNodes().get(0)));
            }
        }
        return sideToConnectedBlocks.get(side).stream()
                .map(PrimaryBlock::getBusNode)
                .collect(Collectors.toList());
    }

    public Block getCentralBlock() {
        return centralBlock;
    }
}
