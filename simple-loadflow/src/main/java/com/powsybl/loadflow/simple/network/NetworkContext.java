/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.google.common.base.Stopwatch;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NetworkContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkContext.class);

    private final Network network;

    private final List<LfBus> buses;

    private final List<LfBranch> branches;

    public NetworkContext(Network network, List<Bus> buses, SlackBusSelectionMode slackBusSelectionMode,
                          Map<HvdcConverterStation, HvdcLine> hvdcLines) {
        this.network = Objects.requireNonNull(network);
        Objects.requireNonNull(buses);
        if (buses.isEmpty()) {
            throw new IllegalArgumentException("Empty bus list");
        }
        Objects.requireNonNull(slackBusSelectionMode);
        Objects.requireNonNull(hvdcLines);

        Set<Branch> branchSet = new HashSet<>();
        List<DanglingLine> danglingLines = new ArrayList<>();
        Set<ThreeWindingsTransformer> t3wtSet = new HashSet<>();
        Map<String, Integer> busIdToNum = new HashMap<>();

        this.buses = createBuses(buses, hvdcLines, branchSet, danglingLines, t3wtSet, busIdToNum);
        branches = createBranches(branchSet, danglingLines, t3wtSet, this.buses, busIdToNum);

        selectSlackBus(slackBusSelectionMode);
    }

    private static List<LfBus> createBuses(List<Bus> buses, Map<HvdcConverterStation, HvdcLine> hvdcLines, Set<Branch> branchSet,
                                           List<DanglingLine> danglingLines, Set<ThreeWindingsTransformer> t3wtSet,
                                           Map<String, Integer> busIdToNum) {
        List<LfBus> lfBuses = new ArrayList<>(buses.size());
        int[] generatorCount = new int[1];

        for (Bus bus : buses) {
            LfBusImpl lfBus = addLfBus(bus, lfBuses, busIdToNum);

            bus.visitConnectedEquipments(new DefaultTopologyVisitor() {

                private void visitBranch(Branch branch) {
                    branchSet.add(branch);
                    lfBus.addNeighbor();
                }

                @Override
                public void visitLine(Line line, Line.Side side) {
                    visitBranch(line);
                }

                @Override
                public void visitTwoWindingsTransformer(TwoWindingsTransformer transformer, TwoWindingsTransformer.Side side) {
                    visitBranch(transformer);
                }

                @Override
                public void visitThreeWindingsTransformer(ThreeWindingsTransformer transformer, ThreeWindingsTransformer.Side side) {
                    t3wtSet.add(transformer);
                }

                @Override
                public void visitGenerator(Generator generator) {
                    lfBus.addGenerationTargetP(generator.getTargetP());
                    if (generator.isVoltageRegulatorOn()) {
                        lfBus.setTargetV(generator.getTargetV());
                        lfBus.setVoltageControl(true);
                    } else {
                        lfBus.addGenerationTargetQ(generator.getTargetQ());
                    }
                    generatorCount[0]++;
                }

                @Override
                public void visitLoad(Load load) {
                    lfBus.addLoadTargetP(load.getP0());
                    lfBus.addLoadTargetQ(load.getQ0());
                }

                @Override
                public void visitShuntCompensator(ShuntCompensator sc) {
                    lfBus.addShuntCompensator(sc);
                }

                @Override
                public void visitDanglingLine(DanglingLine danglingLine) {
                    danglingLines.add(danglingLine);
                }

                @Override
                public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
                    if (staticVarCompensator.getRegulationMode() == StaticVarCompensator.RegulationMode.VOLTAGE) {
                        lfBus.setTargetV(staticVarCompensator.getVoltageSetPoint());
                        lfBus.setVoltageControl(true);
                    } else if (staticVarCompensator.getRegulationMode() == StaticVarCompensator.RegulationMode.REACTIVE_POWER) {
                        throw new UnsupportedOperationException("SVC with reactive power regulation not supported");
                    }
                }

                @Override
                public void visitBattery(Battery battery) {
                    throw new UnsupportedOperationException("TODO: battery");
                }

                @Override
                public void visitHvdcConverterStation(HvdcConverterStation<?> converterStation) {
                    HvdcLine line = hvdcLines.get(converterStation);
                    double p = line.getConverterStation1() == converterStation && line.getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER
                            ? line.getActivePowerSetpoint()
                            : -line.getActivePowerSetpoint();
                    lfBus.addGenerationTargetP(p);
                    switch (converterStation.getHvdcType()) {
                        case VSC:
                            visitVscConverterStation((VscConverterStation) converterStation);
                            break;
                        case LCC:
                            throw new UnsupportedOperationException("TODO: LCC");
                        default:
                            throw new IllegalStateException("Unknown HVDC converter station type: " + converterStation.getHvdcType());
                    }
                }

                private void visitVscConverterStation(VscConverterStation vscCs) {
                    if (vscCs.isVoltageRegulatorOn()) {
                        lfBus.setTargetV(vscCs.getVoltageSetpoint());
                        lfBus.setVoltageControl(true);
                    } else {
                        lfBus.addGenerationTargetQ(vscCs.getReactivePowerSetpoint());
                    }
                }
            });
        }

        if (generatorCount[0] == 0) {
            throw new PowsyblException("Connected component without any regulating generator");
        }

        return lfBuses;
    }

    private static List<LfBranch> createBranches(Set<Branch> branchSet, List<DanglingLine> danglingLines,
                                                 Set<ThreeWindingsTransformer> t3wtSet, List<LfBus> lfBuses,
                                                 Map<String, Integer> busIdToNum) {
        List<LfBranch> lfBranches = new ArrayList<>();

        for (Branch branch : branchSet) {
            LfBus lfBus1 = getLfBus(branch.getTerminal1(), lfBuses, busIdToNum);
            LfBus lfBus2 = getLfBus(branch.getTerminal2(), lfBuses, busIdToNum);
            lfBranches.add(LfBranchImpl.create(branch, lfBus1, lfBus2));
        }

        for (DanglingLine danglingLine : danglingLines) {
            LfDanglingLineBus lfBus2 = addLfBus(danglingLine, lfBuses, busIdToNum);
            LfBus lfBus1 = getLfBus(danglingLine.getTerminal(), lfBuses, busIdToNum);
            lfBranches.add(LfDanglingLineBranch.create(danglingLine, lfBus1, lfBus2));
        }

        for (ThreeWindingsTransformer t3wt : t3wtSet) {
            LfStarBus lfBus0 = addLfBus(t3wt, lfBuses, busIdToNum);
            LfBus lfBus1 = getLfBus(t3wt.getLeg1().getTerminal(), lfBuses, busIdToNum);
            LfBus lfBus2 = getLfBus(t3wt.getLeg2().getTerminal(), lfBuses, busIdToNum);
            LfBus lfBus3 = getLfBus(t3wt.getLeg3().getTerminal(), lfBuses, busIdToNum);
            lfBranches.add(LfLeg1Branch.create(lfBus1, lfBus0, t3wt.getLeg1()));
            lfBranches.add(LfLeg2or3Branch.create(lfBus2, lfBus0, t3wt, t3wt.getLeg2()));
            lfBranches.add(LfLeg2or3Branch.create(lfBus3, lfBus0, t3wt, t3wt.getLeg3()));
        }

        return lfBranches;
    }

    private void selectSlackBus(SlackBusSelectionMode slackBusSelectionMode) {
        LfBus slackBus;
        switch (slackBusSelectionMode) {
            case FIRST:
                slackBus = this.buses.get(0);
                break;
            case MOST_MESHED:
                slackBus = this.buses.stream()
                        .max(Comparator.comparingInt(LfBus::getNeighbors))
                        .orElseThrow(AssertionError::new);
                break;
            default:
                throw new IllegalStateException("Slack bus selection mode unknown:" + slackBusSelectionMode);
        }
        slackBus.setSlack(true);
        LOGGER.debug("Selected slack bus (mode={}): {}", slackBusSelectionMode, slackBus.getId());
    }

    private static LfBus getLfBus(Terminal terminal, List<LfBus> lfBuses, Map<String, Integer> busIdToNum) {
        Bus bus = terminal.getBusView().getBus();
        if (bus != null) {
            int num = busIdToNum.get(bus.getId());
            return lfBuses.get(num);
        }
        return null;
    }

    private static LfBusImpl addLfBus(Bus bus, List<LfBus> lfBuses, Map<String, Integer> busIdToNum) {
        int busNum = lfBuses.size();
        LfBusImpl lfBus = new LfBusImpl(bus, busNum);
        busIdToNum.put(bus.getId(), busNum);
        lfBuses.add(lfBus);
        return lfBus;
    }

    private static LfDanglingLineBus addLfBus(DanglingLine danglingLine, List<LfBus> lfBuses, Map<String, Integer> busIdToNum) {
        int busNum = lfBuses.size();
        LfDanglingLineBus lfBus = new LfDanglingLineBus(danglingLine, busNum);
        busIdToNum.put(lfBus.getId(), busNum);
        lfBuses.add(lfBus);
        return lfBus;
    }

    private static LfStarBus addLfBus(ThreeWindingsTransformer t3wt, List<LfBus> lfBuses, Map<String, Integer> busIdToNum) {
        int busNum = lfBuses.size();
        LfStarBus lfBus = new LfStarBus(t3wt, busNum);
        busIdToNum.put(lfBus.getId(), busNum);
        lfBuses.add(lfBus);
        return lfBus;
    }

    public static List<NetworkContext> of(Network network, SlackBusSelectionMode slackBusSelectionMode) {
        Objects.requireNonNull(network);
        Map<Integer, List<Bus>> buseByCc = new TreeMap<>();
        for (Bus bus : network.getBusView().getBuses()) {
            Component cc = bus.getConnectedComponent();
            if (cc != null) {
                buseByCc.computeIfAbsent(cc.getNum(), k -> new ArrayList<>()).add(bus);
            }
        }

        // hack because there is no way to get HVDC line from converter station
        Map<HvdcConverterStation, HvdcLine> hvdcLines = new HashMap<>();
        for (HvdcLine line : network.getHvdcLines()) {
            hvdcLines.put(line.getConverterStation1(), line);
            hvdcLines.put(line.getConverterStation2(), line);
        }

        return buseByCc.entrySet().stream()
                .filter(e -> e.getKey() == ComponentConstants.MAIN_NUM)
                .map(e -> new NetworkContext(network, e.getValue(), slackBusSelectionMode, hvdcLines))
                .collect(Collectors.toList());
    }

    public List<LfBranch> getBranches() {
        return branches;
    }

    public List<LfBus> getBuses() {
        return buses;
    }

    public LfBus getBus(int num) {
        return buses.get(num);
    }

    public void resetState() {
        Stopwatch stopwatch = Stopwatch.createStarted();

        for (Bus b : network.getBusView().getBuses()) {
            b.setV(Double.NaN);
            b.setAngle(Double.NaN);
        }
        for (ShuntCompensator sc : network.getShuntCompensators()) {
            sc.getTerminal().setQ(Double.NaN);
        }
        for (Branch b : network.getBranches()) {
            b.getTerminal1().setP(Double.NaN);
            b.getTerminal1().setQ(Double.NaN);
            b.getTerminal2().setP(Double.NaN);
            b.getTerminal2().setQ(Double.NaN);
        }

        stopwatch.stop();
        LOGGER.debug("Network reset done in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}

