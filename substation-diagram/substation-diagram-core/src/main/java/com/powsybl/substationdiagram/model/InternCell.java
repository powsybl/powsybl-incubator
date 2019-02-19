/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private Map<Side, Block> sideToBlock;
    @JsonIgnore
    private Map<Side, Node> sideToCentralNode;
    @JsonIgnore
    private Map<Side, List<PrimaryBlock>> sideToConnectedBlocks;

    public InternCell(Graph graph) {
        super(graph, CellType.INTERN);
        centralBlock = null;
        sideToCentralNode = new EnumMap<>(Side.class);
        sideToConnectedBlocks = new EnumMap<>(Side.class);
        sideToConnectedBlocks.put(Side.RIGHT, new ArrayList<>());
        sideToConnectedBlocks.put(Side.LEFT, new ArrayList<>());
        sideToBlock = new EnumMap<>(Side.class);
        setDirection(Direction.TOP);
    }

    public void postPositioningSettings() {
        centralBlock = null;
        identifyIfFlat();
        if (getType() == CellType.INTERNBOUND) {
            if (getDirection() == Direction.FLAT) {
                centralBlock = getRootBlock();
            } else {
                handleNonFlatInterbound();
            }
        } else {
            unRavelBlocks();
            if (centralBlock != null) {
                refactorBlocks();
            }
        }
    }

    private void identifyIfFlat() {
        List<BusNode> buses = getBusbars();
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
        if (block0.getBusNode().getStructuralPosition().getH() < block1.getBusNode().getStructuralPosition().getH()) {
            sideToBlock.put(Side.LEFT, block0);
            sideToBlock.put(Side.RIGHT, block1);
        } else {
            sideToBlock.put(Side.LEFT, block1);
            sideToBlock.put(Side.RIGHT, block0);
        }
        centralBlock = new PrimaryBlock(
                Arrays.asList(new Node[]{block0.getEndingNode(), ns, block1.getEndingNode()}), this);
        setType(CellType.INTERN);
        refactorBlocks();
    }

    private Block createAdjacentPrimaryBlock(SwitchNode ns, int id) {
        FicticiousNode nf = (FicticiousNode) ns.getAdjacentNodes().get(id);
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

    private void unRavelBlocks() {
        Block legBlock1 = null;
        Block legBlock2;
        List<Block> blocksToParalellize = new ArrayList<>();
        for (Block block : ((ParallelBlock) getRootBlock()).getSubBlocks()) {
            if (block instanceof SerialBlock) {
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

        if (centralBlock == null) {
            sideToBlock.put(Side.LEFT, legBlock2);
        } else if (legBlock1 != null) {
            if (getOneNodeBusHPod(legBlock1) > getOneNodeBusHPod(legBlock2)) {
                sideToBlock.put(Side.LEFT, legBlock2);
                sideToBlock.put(Side.RIGHT, legBlock1);
            } else {
                sideToBlock.put(Side.LEFT, legBlock1);
                sideToBlock.put(Side.RIGHT, legBlock2);
            }
        }
        LOGGER.warn("Intern cell structure not handled for {}", getFullId());
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
    private void refactorBlocks() {
        SerialBlock bc = new SerialBlock(sideToBlock.get(Side.LEFT),
                                       centralBlock,
                                       sideToBlock.get(Side.LEFT).getEndingNode(), this);
        List<Block> blocks = new ArrayList<>();
        blocks.add(bc);
        blocks.add(sideToBlock.get(Side.RIGHT));

        ParallelBlock bp = new ParallelBlock(blocks, this, false);
        if (getDirection() == Direction.FLAT) {
            bp.setOrientation(Orientation.HORIZONTAL);
        } else {
            bp.setOrientation(Orientation.VERTICAL);
            bc.setOrientation(Orientation.HORIZONTAL);
            bc.getLowerBlock().setOrientation(Orientation.VERTICAL);
        }

        blocksSetting(bp, getPrimaryBlocksConnectedToBus());
        identifyExtremities();
        identifyConnectedBlocks();
    }

    private int getOneNodeBusHPod(Block b) {
        return ((BusNode) b.getStartingNode()).getStructuralPosition().getH();
    }

    private void identifyExtremities() {
        sideToCentralNode.put(Side.LEFT, centralBlock.getStartingNode());
        sideToCentralNode.put(Side.RIGHT, centralBlock.getEndingNode());
    }

    private void identifyConnectedBlocks() {
        getPrimaryBlocksConnectedToBus().forEach(block -> {
            if (block.getNodes().contains(sideToCentralNode.get(Side.LEFT))) {
                sideToConnectedBlocks.get(Side.LEFT).add(block);
            } else if (block.getNodes().contains(sideToCentralNode.get(Side.RIGHT))) {
                sideToConnectedBlocks.get(Side.RIGHT).add(block);
            }
        });
    }

    public void reverseCell() {
        if (getDirection() == Direction.FLAT) {
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

    public List<BusNode> getSideNodeBus(Side side) {
        if (getType() == CellType.INTERNBOUND) {
            List<BusNode> busNodes = getBusbars()
                    .stream()
                    .sorted(Comparator.comparingInt(n -> n.getStructuralPosition().getH()))
                    .collect(Collectors.toList());
            if (side == Side.LEFT) {
                return new ArrayList<>(Collections.singletonList(busNodes.get(0)));
            } else {
                return new ArrayList<>(Collections.singletonList(busNodes.get(1)));
            }
        }
        return sideToConnectedBlocks.get(side).stream()
                .map(PrimaryBlock::getBusNode)
                .collect(Collectors.toList());
    }

    public Block getCentralBlock() {
        return centralBlock;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
