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
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BlockOrganizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockOrganizer.class);

    private final PositionFinder positionFinder;

    private final boolean stack;

    public BlockOrganizer() {
        this(new PositionFromExtension(), true);
    }

    public BlockOrganizer(boolean stack) {
        this(new PositionFree(), stack);
    }

    public BlockOrganizer(PositionFinder positionFinder) {
        this(positionFinder, true);
    }

    public BlockOrganizer(PositionFinder positionFinder, boolean stack) {
        this.positionFinder = Objects.requireNonNull(positionFinder);
        this.stack = stack;
    }

    /**
     * Organize cells into blocks and call the layout resolvers
     *
     * @return true if the Extension enabled to place the whole graph
     */
    public boolean organize(Graph graph) {
        LOGGER.info("Organizing graph cells into blocks");
        graph.getCells().stream()
                .filter(cell -> cell.getType().equals(Cell.CellType.EXTERNAL)
                        || cell.getType().equals(Cell.CellType.INTERNAL))
                .forEach(cell -> {
                    new CellBlockDecomposer().determineBlocks(cell);
                    if (cell.getType() == Cell.CellType.INTERNAL) {
                        ((InternalCell) cell).rationalizeOrganization();
                    }
                });
        graph.getCells().stream()
                .filter(cell -> cell.getType() == Cell.CellType.SHUNT)
                .forEach(cell -> new CellBlockDecomposer().determineBlocks(cell));

        if (stack) {
            determinePreliminaryStackableBlocks(graph);
        }
        positionFinder.buildLayout(graph);

        graph.getCells().stream()
                .filter(c -> c.getType() == Cell.CellType.INTERNAL)
                .forEach(c -> ((InternalCell) c).postPositioningSettings());

        SubSections subSections = new SubSections(graph);
        subSections.handleSpanningBusBar();
        LOGGER.debug("Subsections {}", subSections);

        graph.getCells().stream()
                .filter(cell -> cell instanceof BusCell)
                .forEach(cell -> cell.getRootBlock().calculateDimensionAndInternPos());
        determineBlockPositions(graph, subSections);

        manageInternCellOverlaps(graph);

        return true;
    }

    /**
     * Determines blocks connected to busbar that are stackable
     */
    private void determinePreliminaryStackableBlocks(Graph graph) {
        LOGGER.info("Determining stackable Blocks");
        graph.getBusCells().forEach(cell -> {
            List<PrimaryBlock> blocks = cell.getPrimaryBlocksConnectedToBus();
            for (int i = 0; i < blocks.size(); i++) {
                PrimaryBlock block1 = blocks.get(i);
                if (block1.getNodes().size() == 3) {
                    for (int j = i + 1; j < blocks.size(); j++) {
                        PrimaryBlock block2 = blocks.get(j);
                        if (block2.getNodes().size() == 3
                                && block1.getEndingNode().equals(block2.getEndingNode())
                                && !block1.getStartingNode().equals(block2.getStartingNode())) {
                            block1.addStackableBlock(block2);
                            block2.addStackableBlock(block1);
                        }
                    }
                }
            }
        });
    }

    private void determineBlockPositions(Graph graph, SubSections subSections) {
        int hPos = 0;
        int prevHPos = 0;
        int hSpace = 0;
        int maxV = graph.getMaxBusStructuralPosition().getV();
        List<InternalCell> nonFlatCellsToClose = new ArrayList<>();
        PosNWidth posNWidth;

        int[] previousIndexes = new int[maxV];
        graph.getNodeBuses().forEach(nodeBus -> nodeBus.getPosition().setV(nodeBus.getStructuralPosition().getV()));

        for (Map.Entry<SubSections.SubSectionIndexes, SubSections.HorizontalSubSection> entry :
                subSections.getSubsectionMap().entrySet()) {
            int[] ssIndexes = entry.getKey().getIndexes();
            SubSections.HorizontalSubSection hSs = entry.getValue();
            for (int vPos = 0; vPos < maxV; vPos++) {
                if (ssIndexes[vPos] != previousIndexes[vPos]) {
                    updateNodeBusPos(graph, vPos, hPos, hSpace, previousIndexes, Side.LEFT);
                    updateNodeBusPos(graph, vPos, hPos, 0, ssIndexes, Side.RIGHT);
                }
            }
            hPos = placeHorizontalInternCell(hPos, prevHPos, hSs, Side.LEFT, nonFlatCellsToClose).pos;
            hPos = placeVerticalCoupling(hPos, hSs);
            hPos = placeExternCell(hPos, hSs.getExternalCells());

            posNWidth = placeHorizontalInternCell(hPos, prevHPos, hSs, Side.RIGHT, nonFlatCellsToClose);
            hPos = posNWidth.pos;
            hSpace = posNWidth.width;

            if (hPos == prevHPos) {
                hPos++;
            }

            prevHPos = hPos;
            previousIndexes = ssIndexes;

        }
        for (int vPos = 0; vPos < maxV; vPos++) {
            updateNodeBusPos(graph, vPos, hPos, hSpace, previousIndexes, Side.LEFT);
        }
    }

    private void updateNodeBusPos(Graph graph, int vPos, int hPos, int hSpace, int[] indexes, Side side) {
        if (indexes[vPos] != 0) {
            Position p = graph.getVHNodeBus(vPos + 1, indexes[vPos]).getPosition();
            if (side == Side.LEFT) {
                p.setHSpan(hPos - Math.max(p.getH(), 0) - hSpace);
            } else if (side == Side.RIGHT && (p.getH() == -1 || hPos == 0)) {
                p.setH(hPos);
            }
        }
    }

    private int placeExternCell(int hPos, Set<ExternalCell> externalCells) {
        int hPos2 = hPos;
        for (Cell cell : externalCells) {
            Position rootPosition = cell.getRootBlock().getPosition();
            rootPosition.setHV(hPos2, 0);
            hPos2 += rootPosition.getHSpan();

        }
        return hPos2;
    }

    private int placeVerticalCoupling(int hPos, SubSections.HorizontalSubSection hSs) {
        int hPos2 = hPos;
        for (InternalCell cell : hSs.getSideInternCells(Side.UNDEFINED)) {
            cell.getRootBlock().getPosition().setH(hPos2);
            hPos2 += cell.getRootBlock().getPosition().getHSpan();
//            if (cell.getCentralBlock() != null) {
//                hPos2 += cell.getCentralBlock().getPosition().getHSpan();
//            }
//            hPos2++;
        }
        return hPos2;
    }

    private PosNWidth placeHorizontalInternCell(int hPos,
                                                int prevHPos,
                                                SubSections.HorizontalSubSection hSs,
                                                Side side, List<InternalCell> nonFlatCellsToClose) {
        int hPosRes = hPos;
        Set<InternalCell> cells = hSs.getSideInternCells(side);
        List<InternalCell> nonFlatCells = cells.stream()
                .filter(cellIntern -> cellIntern.getDirection() != BusCell.Direction.FLAT).distinct()
                .sorted(Comparator.comparingInt(c -> -nonFlatCellsToClose.indexOf(c)))
                .collect(Collectors.toList());
        if (side == Side.RIGHT) {
            hPosRes = openHorizontalNonFlatCell(hPosRes, nonFlatCells);
            nonFlatCellsToClose.addAll(nonFlatCells);
        } else {
            hPosRes = closeHorizontalNonFlatCell(hPosRes, nonFlatCells);
            nonFlatCellsToClose.removeAll(nonFlatCells);
        }
        cells.removeAll(nonFlatCells);
        int shift = 0;
        for (InternalCell cell : cells) {
            if (side == Side.RIGHT) {
                if (prevHPos == hPosRes) {
                    hPosRes++;
                }
                Position position = cell.getRootBlock().getPosition();
                position.setHV(hPosRes, cell.getBusNodes().get(0).getStructuralPosition().getV());
                shift = Math.max(position.getHSpan(), shift);
            }
        }

        return new PosNWidth(hPosRes + shift, shift);
    }

    private int openHorizontalNonFlatCell(int hPos, List<InternalCell> cells) {
        int hPosRes = hPos;
        for (InternalCell cell : cells) {
            Position rootPosition = cell.getRootBlock().getPosition();
            rootPosition.setHV(hPosRes, 0);
            hPosRes += rootPosition.getHSpan() - cell.getSideToBlock(Side.RIGHT).getPosition().getHSpan();
        }
        return hPosRes;
    }

    private int closeHorizontalNonFlatCell(int hPos, List<InternalCell> cells) {
        int hPosRes = hPos;
        for (InternalCell cell : cells) {
            Block rightBlock = cell.getSideToBlock(Side.RIGHT);
            if (rightBlock != null) {
                rightBlock.getPosition().setH(hPosRes);
                rightBlock.getPosition().setAbsolute(true);
                hPosRes += rightBlock.getPosition().getHSpan();
            }
        }
        return hPosRes;
    }

    class PosNWidth {
        int pos;
        int width;

        PosNWidth(int pos, int width) {
            this.pos = pos;
            this.width = width;
        }
    }

    private void manageInternCellOverlaps(Graph graph) {
        List<InternalCell> cellsToHandle = graph.getCells().stream()
                .filter(cell -> cell.getType() == Cell.CellType.INTERNAL)
                .map(InternalCell.class::cast)
                .filter(internCell -> internCell.getDirection() != BusCell.Direction.FLAT
                        && internCell.getCentralBlock() != null)
                .collect(Collectors.toList());
        Lane lane = new Lane(cellsToHandle);
        lane.run();
    }

    /**
     * The class lane manages the overlaps of internCells.
     * After bundleToCompatibleLanes each lane contents non overlapping cells
     * arrangeLane at this stage balance the lanes on TOP and BOTTOM this could be improved by having various VPos per lane
     */
    private class Lane {
        HashMap<InternalCell, ArrayList<InternalCell>> incompatibilities;
        Lane nextLane;
        List<Lane> lanes;

        Lane(List<InternalCell> cells) {
            lanes = new ArrayList<>();
            lanes.add(this);
            incompatibilities = new HashMap<>();
            cells.forEach(this::addCell);
        }

        Lane(InternalCell cell, List<Lane> lanes) {
            this.lanes = lanes;
            lanes.add(this);
            incompatibilities = new HashMap<>();
            addCell(cell);
        }

        void run() {
            bundleToCompatibleLanes();
            arrangeLanes();
        }

        private void addCell(InternalCell cell) {
            incompatibilities.put(cell, new ArrayList<>());
        }

        private void bundleToCompatibleLanes() {
            while (identifyIncompatibilities()) {
                shiftIncompatibilities();
            }
        }

        private boolean identifyIncompatibilities() {
            boolean hasIncompatibility = false;
            for (Map.Entry<InternalCell, ArrayList<InternalCell>> entry : incompatibilities.entrySet()) {
                InternalCell internalCellA = entry.getKey();
                entry.getValue().clear();
                int hAmin = internalCellA.getSideHPos(Side.LEFT);
                int hAmax = internalCellA.getSideHPos(Side.RIGHT);

                for (InternalCell internalCellB : incompatibilities.keySet()) {
                    if (!internalCellA.equals(internalCellB)) {
                        int hBmin = internalCellB.getSideHPos(Side.LEFT);
                        int hBmax = internalCellB.getSideHPos(Side.RIGHT);
                        if (hAmax > hBmin && hBmax > hAmin) {
                            entry.getValue().add(internalCellB);
                            hasIncompatibility = true;
                        }
                    }
                }
            }
            return hasIncompatibility;
        }

        private void shiftIncompatibilities() {
            Map.Entry<InternalCell, ArrayList<InternalCell>> entry = incompatibilities.entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .max(Comparator.comparingInt(e -> e.getValue().size())).orElse(null);

            if (entry != null) {
                InternalCell cell = entry.getKey();
                incompatibilities.remove(cell);
                if (nextLane == null) {
                    nextLane = new Lane(cell, lanes);
                } else {
                    nextLane.addCell(cell);
                    nextLane.bundleToCompatibleLanes();
                }
            }
        }

        private void arrangeLanes() {
            int i = 0;
            for (Lane lane : lanes) {
                final int j = i % 2;
                final int newV = 1 + i / 2;
                lane.incompatibilities.keySet()
                        .forEach(c -> {
                            c.setDirection(j == 0 ? BusCell.Direction.TOP : BusCell.Direction.BOTTOM);
                            c.getRootPosition().setV(newV);
                        });
                i++;
            }
        }

        public String toString() {
            StringBuilder str = new StringBuilder(incompatibilities.toString() + "\n\n");
            if (nextLane != null) {
                str.append(nextLane.toString());
            }
            return new String(str);
        }

        public int size() {
            return incompatibilities.size();
        }

    }
}
