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

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SubSections splits the horizontal organisation of the bus bars to cope with the case when parallelism is not respected
 * This solves the case of a busbar spanning over many busbars at another vertical structural position.
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class SubSections {

    private Graph graph;

    private static final Logger LOGGER = LoggerFactory.getLogger(SubSections.class);

    private Map<SubSectionIndexes, HorizontalSubSection> subsectionMap;

    private static final String STR_SIDE = "\t side ";

    SubSections(Graph graph) {
        this.graph = graph;
        subsectionMap = new TreeMap<>();
    }

    class ExternCellComparator implements Comparator<ExternalCell> {
        public int compare(ExternalCell extCell1, ExternalCell extCell2) {
            if (extCell1 == extCell2) {
                return 0;
            }
            if (extCell1.getOrder() == extCell2.getOrder()) {
                return Comparator.comparingInt(ExternalCell::getNumber).compare(extCell1, extCell2);
            }
            return Comparator.comparingInt(ExternalCell::getOrder).compare(extCell1, extCell2);
        }
    }

    class HorizontalSubSection {
        private Set<Cell> cells;
        private Set<ExternalCell> externalCells;
        private Set<InternalCell> internalCells;
        private Set<BusNode> busNodes;
        private Map<InternalCell, Side> cellToSideMap;
        private int order;

        HorizontalSubSection() {
            cells = new HashSet<>();
            externalCells = new TreeSet<>(new ExternCellComparator());

            internalCells = new HashSet<>();
            busNodes = new HashSet<>();
            cellToSideMap = new HashMap<>();
            order = -1;
        }

        void add(BusCell busCell) {
            cells.add(busCell);
            if (busCell.getType() == Cell.CellType.EXTERNAL) {
                externalCells.add((ExternalCell) busCell);
                if (order == -1) {
                    order = ((ExternalCell) busCell).getOrder();
                }
            }
            busNodes.addAll(busCell.getBusNodes());
        }

        void add(InternalCell cell, Side side) {
            cells.add(cell);
            internalCells.add(cell);
            if (cellToSideMap.containsKey(cell) || side == Side.UNDEFINED) {
                cellToSideMap.put(cell, Side.UNDEFINED); // vertical coupling
            } else {
                cellToSideMap.put(cell,
                        side == Side.RIGHT ? Side.LEFT : Side.RIGHT); //inversion, the left leg of an interncell, is on the right side of the subsection
            }
            busNodes.addAll(cell.getBusNodes());
        }

        void merge(HorizontalSubSection hss) {
            cells.addAll(hss.cells);
            externalCells.addAll(hss.externalCells);
            internalCells.addAll(hss.internalCells);
            busNodes.addAll(hss.busNodes);
            cellToSideMap.putAll(hss.cellToSideMap);
        }

        @Override
        public String toString() {
            StringBuilder strBd = new StringBuilder();

            Set<InternalCell> leftCells = getSideInternCells(Side.LEFT);
            if (!leftCells.isEmpty()) {
                strBd.append("internCells Left: ").append(leftCells.size()).append("\n");
                leftCells.forEach(cell -> strBd.append(STR_SIDE).append(" ").append(cell.toString()).append("\n"));
            }

            if (!externalCells.isEmpty()) {
                strBd.append("externCells: ").append(externalCells.size()).append("\n");
                externalCells.forEach(cell -> strBd.append("\t").append(cell.toString()).append("\n"));
            }

            Set<InternalCell> undefinedCells = getSideInternCells(Side.UNDEFINED);
            if (!undefinedCells.isEmpty()) {
                strBd.append("undefined internCells: ").append(undefinedCells.size()).append("\n");
                undefinedCells.forEach(cell -> strBd.append(STR_SIDE).append(" ").append(cell.toString()).append("\n"));
            }

            Set<InternalCell> rightCells = getSideInternCells(Side.RIGHT);
            if (!rightCells.isEmpty()) {
                strBd.append("internCells Right: ").append(rightCells.size()).append("\n");
                rightCells.forEach(cell -> strBd.append(STR_SIDE).append(" ").append(cell).append("\n"));
            }

            strBd.append("busNodes: ").append(busNodes.size()).append("\n");
            busNodes.forEach(node -> strBd.append("\t").append(node.toString()).append("\n"));

            return strBd.toString();
        }

        Set<BusNode> getBusNodes() {
            return new HashSet<>(busNodes);
        }

        Set<ExternalCell> getExternalCells() {
            TreeSet<ExternalCell> externalCellsCopy = new TreeSet<>(new ExternCellComparator());
            externalCellsCopy.addAll(this.externalCells);
            return externalCellsCopy;
        }

        Set<Cell> getCells() {
            return new HashSet<>(cells);
        }

        Set<InternalCell> getInternalCells() {
            return new HashSet<>(internalCells);
        }

        Set<InternalCell> getSideInternCells(Side side) {
            return internalCells.stream().filter(cell -> cellToSideMap.get(cell) == side)
                    .collect(Collectors.toSet());
        }

        Map<InternalCell, Side> getCellToSideMap() {
            return cellToSideMap;
        }

        void setSide(InternalCell c, Side side) {
            cellToSideMap.put(c, side);
        }

    }

    class SubSectionIndexes implements Comparable<SubSectionIndexes> {
        private int size;
        private int[] indexes;
        private int order;

        SubSectionIndexes(int size) {
            this.size = size;
            indexes = new int[size];
            order = -1;
        }

        void setIndexI(int i, int val) {
            indexes[i] = val;
        }

        void updateOrder(int order) {
            this.order = Math.max(this.order, order);
        }

        int[] getIndexes() {
            return indexes.clone();
        }

        boolean asSameNonZeroIndexes(SubSectionIndexes ssI) {
            for (int i = 0; i < size; i++) {
                int index = ssI.getIndexes()[i];
                if (index != 0 && index != indexes[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object ss) {
            if (ss instanceof SubSectionIndexes) {
                return Arrays.equals(((SubSectionIndexes) ss).getIndexes(), this.indexes);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return Arrays.toString(indexes);
        }

        @Override
        public int compareTo(@Nonnull SubSectionIndexes o) {
            boolean hasBoth0 = false;
            for (int i = 0; i < size; i++) {
                if (indexes[i] != 0 && o.getIndexes()[i] != 0) {
                    int index = o.getIndexes()[i];
                    if (indexes[i] != index) {
                        return indexes[i] - index;
                    }
                } else {
                    hasBoth0 = true;
                }
            }
            if (hasBoth0) {
                return notObviousComp(o.getIndexes());
            }
            return 0;
        }

        private int notObviousComp(int[] indexes2) {
            int compMax = Arrays.stream(indexes).max().orElse(0) - Arrays.stream(indexes2).max().orElse(0);
            if (compMax != 0) {
                return compMax;
            }
            for (int i = 0; i < size; i++) {
                int index = indexes2[i];
                if (indexes[i] != index) {
                    return indexes[i] - index;
                }
            }
            return 0;
        }

        SubSectionIndexes merge(SubSectionIndexes ssi) {
            SubSectionIndexes resSsi = new SubSectionIndexes(size);
            for (int i = 0; i < size; i++) {
                if (ssi.indexes[i] != 0 && indexes[i] != 0) {
                    if (ssi.indexes[i] != indexes[i]) {
                        return new SubSectionIndexes(0);
                    } else {
                        resSsi.indexes[i] = indexes[i];
                    }
                } else {
                    resSsi.indexes[i] = Math.max(ssi.indexes[i], indexes[i]);
                }
            }
            resSsi.order = Math.max(ssi.order, order);
            return resSsi;
        }

    }

    void handleSpanningBusBar() {
        buildSubSections();
        checkInternCellOrientation();
        if (!checkCellOrderConsistencyWithSubsSections()) {
            LOGGER.warn("*************** Cells order not consistent with Subsections order");
        }
    }

    private void checkInternCellOrientation() {
        Map<InternalCell, List<SubSectionIndexes>> cellToIndex = new HashMap<>();

        subsectionMap.forEach((ssI, ssh) -> {
            ssh.getInternalCells().stream()
                    .filter(c -> c.getCentralBlock() != null)
                    .forEach(c -> {
                        cellToIndex.putIfAbsent(c, new ArrayList<>());
                        cellToIndex.get(c).add(ssI);
                    });
        });

        cellToIndex.forEach((c, ssiList) -> {
            if (ssiList.size() == 2) {
                HorizontalSubSection leftHss = subsectionMap.get(ssiList.get(0));
                HorizontalSubSection rightHss = subsectionMap.get(ssiList.get(1));
                if (leftHss.getSideInternCells(Side.LEFT).contains(c)
                        && rightHss.getSideInternCells(Side.RIGHT).contains(c)) {
                    c.reverseCell();
                    leftHss.setSide(c, Side.RIGHT);
                    rightHss.setSide(c, Side.LEFT);
                }
            }
        });
    }

    private boolean verticalInternCell(InternalCell cell) {
        return cell.getBusNodes().stream()
                .map(bus -> bus.getStructuralPosition().getH())
                .distinct().count() == 1;
    }

    private void buildSubSections() {
        graph.getCells().stream().filter(cell -> cell.getType() == Cell.CellType.EXTERNAL)
                .map(ExternalCell.class::cast)
                .forEach(cell -> allocateCellToSubsection(cell, cell.getBusNodes(), Side.UNDEFINED));

        Set<InternalCell> internalCells = graph.getCells().stream()
                .filter(cell -> cell.getType() == Cell.CellType.INTERNAL)
                .map(InternalCell.class::cast)
                .collect(Collectors.toSet());

        Set<InternalCell> verticalInternalCells = internalCells.stream().filter(this::verticalInternCell)

                .collect(Collectors.toSet());
        verticalInternalCells.forEach(cell -> allocateCellToSubsection(cell, cell.getBusNodes(), Side.UNDEFINED));

        internalCells.removeAll(verticalInternalCells);

        internalCells.stream()
                .filter(cell -> cell.getType() == Cell.CellType.INTERNAL)
                .forEach(internCell -> {
                    allocateCellToSubsection(internCell, internCell.getSideBusNodes(Side.LEFT), Side.LEFT);
                    allocateCellToSubsection(internCell, internCell.getSideBusNodes(Side.RIGHT), Side.RIGHT);
                });
        mergeSimilarSubstations();
    }

    private void mergeSimilarSubstations() {
        boolean change = true;
        while (change) {
            change = false;
            List<SubSectionIndexes> ssiList = new ArrayList<>(subsectionMap.keySet());
            for (int i = 0; i < ssiList.size() && !change; i++) {
                SubSectionIndexes ssi1 = ssiList.get(i);
                for (int j = i + 1; j < ssiList.size() && !change; j++) {
                    SubSectionIndexes ssi2 = ssiList.get(j);
                    SubSectionIndexes newSSI = ssi1.merge(ssi2);
                    if (newSSI.size != 0) {
                        change = true;
                        HorizontalSubSection hss1 = subsectionMap.get(ssi1);
                        HorizontalSubSection hss2 = subsectionMap.get(ssi2);
                        subsectionMap.remove(ssi1);
                        subsectionMap.remove(ssi2);
                        hss1.merge(hss2);
                        if (subsectionMap.containsKey(newSSI)) {
                            hss1.merge(subsectionMap.get(newSSI));
                        }
                        subsectionMap.put(newSSI, hss1);
                    }
                }
            }
        }
    }

    private void allocateCellToSubsection(BusCell busCell, List<BusNode> busNodes, Side side) {
        SubSectionIndexes indexes = new SubSectionIndexes(graph.getMaxBusStructuralPosition().getV());
        busNodes.stream().map(BusNode::getStructuralPosition).collect(Collectors.toList())
                .forEach(position -> indexes.setIndexI(position.getV() - 1, position.getH()));

        if (side == Side.UNDEFINED) {
            subsectionMap.putIfAbsent(indexes, new HorizontalSubSection());
            if (busCell instanceof InternalCell) {
                subsectionMap.get(indexes).add((InternalCell) busCell, side);
            } else {
                subsectionMap.get(indexes).add(busCell);
                if (busCell.getType() == Cell.CellType.EXTERNAL) {
                    indexes.updateOrder(((ExternalCell) busCell).getOrder());
                }
            }
        } else {
            Set<SubSectionIndexes> candidateSubsectionIndexes;
            candidateSubsectionIndexes = subsectionMap.keySet().stream()
                    .filter(ssi -> ssi.asSameNonZeroIndexes(indexes))
                    .collect(Collectors.toSet());
            if (candidateSubsectionIndexes.isEmpty()) {
                subsectionMap.put(indexes, new HorizontalSubSection());
                candidateSubsectionIndexes.add(indexes);
            }
            SubSectionIndexes ssI;
            if (side == Side.LEFT) {
                ssI = Collections.max(candidateSubsectionIndexes);
            } else {
                ssI = Collections.min(candidateSubsectionIndexes);
            }
            subsectionMap.get(ssI).add((InternalCell) busCell, side);
        }
    }

    private boolean checkCellOrderConsistencyWithSubsSections() {
        int previousMax = 0;
        boolean checkOK = true;
        for (Map.Entry<SubSectionIndexes, HorizontalSubSection> entry : subsectionMap.entrySet()) {
            Set<ExternalCell> externalCells = entry.getValue().getExternalCells();
            int minOrder = externalCells.stream().mapToInt(ExternalCell::getOrder).max().orElse(previousMax);
            checkOK &= minOrder >= previousMax;
            previousMax = externalCells.stream().mapToInt(ExternalCell::getOrder).max().orElse(previousMax);
        }
        return checkOK;
    }

    @Override
    public String toString() {
        StringBuilder stBdr = new StringBuilder();
        getSubsectionMap().forEach((indexes, subsection) -> {
            stBdr.append(Arrays.toString(indexes.getIndexes())).append(":\n");
            stBdr.append(subsection.toString()).append("\n");
        });
        return stBdr.toString();
    }

    Map<SubSectionIndexes, HorizontalSubSection> getSubsectionMap() {
        return new TreeMap<>(subsectionMap);
    }
}
