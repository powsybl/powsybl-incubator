/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class InternCell extends BusCell {

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
        refactorBlocks();
    }

    private void identifyIfFlat() {
        List<BusNode> buses = getBusNodes();
        if (buses.size() != 2) {
            return;
        }
        Position pos1 = buses.get(0).getStructuralPosition();
        Position pos2 = buses.get(1).getStructuralPosition();
        if (Math.abs(pos2.getH() - pos1.getH()) == 1 && pos2.getV() == pos1.getV()) {
            setDirection(Direction.FLAT);
            getRootBlock().setOrientation(Orientation.HORIZONTAL);
        }
    }

    /**
     * <pre>
     * the organisation of the block shall be
     *     a parallelBlock of - from left to right :
     *         1 serial block with:
     *             lowerBlock: 1 (parallelBlocks or one primaryBlock) LefttNode to busNodes on the right
     *             upperBlock: 1 central block (with startingNode = leftNode and endingNode = rightNode)
     *         blocks - one or more / whatever type from RightNode to busNodes
     * </pre>
     */
    public void rationalizeOrganization() {
        unRavelBlocks();
        refactorBlocks();
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
        if (centralBlock == null) {
            return;
        }
        SerialBlock bc = new SerialBlock(sideToBlock.get(Side.LEFT),
                centralBlock, this);
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
        Block swap = sideToBlock.get(Side.LEFT);
        sideToBlock.put(Side.LEFT, sideToBlock.get(Side.RIGHT));
        sideToBlock.put(Side.RIGHT, swap);
        refactorBlocks();
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

    public Block getSideToBlock(Side side) {
        return sideToBlock.get(side);
    }

    public List<BusNode> getSideBusNodes(Side side) {
        return sideToConnectedBlocks.get(side).stream()
                .map(PrimaryBlock::getBusNode)
                .collect(Collectors.toList());
    }

    public Block getCentralBlock() {
        return centralBlock;
    }
}
