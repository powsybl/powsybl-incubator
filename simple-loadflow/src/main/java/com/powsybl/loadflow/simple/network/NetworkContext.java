/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.google.common.base.Stopwatch;
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
        this.buses = new ArrayList<>(buses.size());

        Set<Branch> branchSet = new HashSet<>();
        Map<String, Integer> busIdToNum = new HashMap<>();

        for (Bus bus : buses) {
            int busNum = this.buses.size();
            LfBusImpl lfBus = new LfBusImpl(bus, busNum);
            busIdToNum.put(bus.getId(), busNum);
            this.buses.add(lfBus);

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
                    throw new UnsupportedOperationException("TODO: 3 windings transformers");
                }

                @Override
                public void visitGenerator(Generator generator) {
                    lfBus.addTargetP(generator.getTargetP());
                    if (generator.isVoltageRegulatorOn()) {
                        lfBus.setTargetV(generator.getTargetV());
                        lfBus.setType(LfBusType.PV);
                    } else {
                        lfBus.addTargetQ(generator.getTargetQ());
                    }
                }

                @Override
                public void visitLoad(Load load) {
                    lfBus.addTargetP(-load.getP0());
                    lfBus.addTargetQ(-load.getQ0());
                }

                @Override
                public void visitShuntCompensator(ShuntCompensator sc) {
                    lfBus.addShuntCompensator(sc);
                }

                @Override
                public void visitDanglingLine(DanglingLine danglingLine) {
                    throw new UnsupportedOperationException("TODO: dangling lines");
                }

                @Override
                public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
                    //throw new UnsupportedOperationException("TODO: static var compensator");
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
                    lfBus.addTargetP(-p);
                }
            });
        }

        branches = branchSet.stream().map(branch -> {
            Bus bus1 = branch.getTerminal1().getBusView().getBus();
            Bus bus2 = branch.getTerminal2().getBusView().getBus();
            LfBus lfBus1 = null;
            if (bus1 != null) {
                int num1 = busIdToNum.get(bus1.getId());
                lfBus1 = NetworkContext.this.buses.get(num1);
            }
            LfBus lfBus2 = null;
            if (bus2 != null) {
                int num2 = busIdToNum.get(bus2.getId());
                lfBus2 = NetworkContext.this.buses.get(num2);
            }
            return new LfBranchImpl(branch, lfBus1, lfBus2);
        }).collect(Collectors.toList());

        switch (slackBusSelectionMode) {
            case FIRST:
                ((LfBusImpl) this.buses.get(0)).setType(LfBusType.SLACK);
                break;
            case MOST_MESHED:
                this.buses.stream().map(bus -> (LfBusImpl) bus)
                        .max(Comparator.comparingInt(LfBusImpl::getNeighbors))
                        .orElseThrow(AssertionError::new)
                        .setType(LfBusType.SLACK);
                break;
            default:
                throw new IllegalStateException("Slack bus selection mode unknown:" + slackBusSelectionMode);
        }
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

        // FIXME hack because there is no way to get HVDC line from converter station
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

    public Network getNetwork() {
        return network;
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

