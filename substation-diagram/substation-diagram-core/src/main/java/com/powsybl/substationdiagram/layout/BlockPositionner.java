package com.powsybl.substationdiagram.layout;

import com.powsybl.substationdiagram.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class BlockPositionner {

    void determineBlockPositions(Graph graph, SubSections subSections) {
        int hPos = 0;
        int prevHPos = 0;
        int hSpace = 0;
        int maxV = graph.getMaxBusStructuralPosition().getV();
        List<InternCell> nonFlatCellsToClose = new ArrayList<>();
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
            hPos = placeExternCell(hPos, hSs.getExternCells());

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

        manageInternCellOverlaps(graph);
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

    private int placeExternCell(int hPos, Set<ExternCell> externCells) {
        int hPos2 = hPos;
        for (Cell cell : externCells) {
            Position rootPosition = cell.getRootBlock().getPosition();
            rootPosition.setHV(hPos2, 0);
            hPos2 += rootPosition.getHSpan();

        }
        return hPos2;
    }

    private int placeVerticalCoupling(int hPos, SubSections.HorizontalSubSection hSs) {
        int hPos2 = hPos;
        for (InternCell cell : hSs.getSideInternCells(Side.UNDEFINED)) {
            cell.getRootBlock().getPosition().setH(hPos2);
            hPos2 += cell.getRootBlock().getPosition().getHSpan();
        }
        return hPos2;
    }

    private PosNWidth placeHorizontalInternCell(int hPos,
                                                int prevHPos,
                                                SubSections.HorizontalSubSection hSs,
                                                Side side, List<InternCell> nonFlatCellsToClose) {
        int hPosRes = hPos;
        Set<InternCell> cells = hSs.getSideInternCells(side);
        List<InternCell> nonFlatCells = cells.stream()
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
        for (InternCell cell : cells) {
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

    private int openHorizontalNonFlatCell(int hPos, List<InternCell> cells) {
        int hPosRes = hPos;
        for (InternCell cell : cells) {
            Position rootPosition = cell.getRootBlock().getPosition();
            rootPosition.setHV(hPosRes, 0);
            hPosRes += rootPosition.getHSpan() - cell.getSideToBlock(Side.RIGHT).getPosition().getHSpan();
        }
        return hPosRes;
    }

    private int closeHorizontalNonFlatCell(int hPos, List<InternCell> cells) {
        int hPosRes = hPos;
        for (InternCell cell : cells) {
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

    public void manageInternCellOverlaps(Graph graph) {
        List<InternCell> cellsToHandle = graph.getCells().stream()
                .filter(cell -> cell.getType() == Cell.CellType.INTERN)
                .map(InternCell.class::cast)
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
        HashMap<InternCell, ArrayList<InternCell>> incompatibilities;
        Lane nextLane;
        List<Lane> lanes;

        Lane(List<InternCell> cells) {
            lanes = new ArrayList<>();
            lanes.add(this);
            incompatibilities = new HashMap<>();
            cells.forEach(this::addCell);
        }

        Lane(InternCell cell, List<Lane> lanes) {
            this.lanes = lanes;
            lanes.add(this);
            incompatibilities = new HashMap<>();
            addCell(cell);
        }

        void run() {
            bundleToCompatibleLanes();
            arrangeLanes();
        }

        private void addCell(InternCell cell) {
            incompatibilities.put(cell, new ArrayList<>());
        }

        private void bundleToCompatibleLanes() {
            while (identifyIncompatibilities()) {
                shiftIncompatibilities();
            }
        }

        private boolean identifyIncompatibilities() {
            boolean hasIncompatibility = false;
            for (Map.Entry<InternCell, ArrayList<InternCell>> entry : incompatibilities.entrySet()) {
                InternCell internCellA = entry.getKey();
                entry.getValue().clear();
                int hAmin = internCellA.getSideHPos(Side.LEFT);
                int hAmax = internCellA.getSideHPos(Side.RIGHT);

                for (InternCell internCellB : incompatibilities.keySet()) {
                    if (!internCellA.equals(internCellB)) {
                        int hBmin = internCellB.getSideHPos(Side.LEFT);
                        int hBmax = internCellB.getSideHPos(Side.RIGHT);
                        if (hAmax > hBmin && hBmax > hAmin) {
                            entry.getValue().add(internCellB);
                            hasIncompatibility = true;
                        }
                    }
                }
            }
            return hasIncompatibility;
        }

        private void shiftIncompatibilities() {
            Map.Entry<InternCell, ArrayList<InternCell>> entry = incompatibilities.entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .max(Comparator.comparingInt(e -> e.getValue().size())).orElse(null);

            if (entry != null) {
                InternCell cell = entry.getKey();
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
