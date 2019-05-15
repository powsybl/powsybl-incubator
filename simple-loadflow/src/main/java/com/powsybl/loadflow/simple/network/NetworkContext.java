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

    private final static Logger LOGGER = LoggerFactory.getLogger(NetworkContext.class);

    private final Network network;

    private final List<Bus> buses;

    private final List<Branch> branches;

    private final List<ShuntCompensator> shuntCompensators = new ArrayList<>();

    private final Map<String, Bus> busesById;

    private final String mostMeshedBusId;

    private String slackBusId;

    private final Set<String> pvBusIds = new HashSet<>();

    private final Map<String, Double> busP = new HashMap<>();
    private final Map<String, Double> busQ = new HashMap<>();

    public NetworkContext(Network network, List<Bus> buses, Map<HvdcConverterStation, HvdcLine> hvdcLines) {
        this.network = Objects.requireNonNull(network);
        this.buses = Objects.requireNonNull(buses);
        Objects.requireNonNull(hvdcLines);

        busesById = buses.stream().collect(Collectors.toMap(Identifiable::getId, b -> b));

        Set<Branch> branchSet = new HashSet<>();
        Map<String, Integer> neighbors = new HashMap<>();
        for (Bus bus : buses) {
            bus.visitConnectedEquipments(new DefaultTopologyVisitor() {

                private void visitBranch(Branch branch) {
                    branchSet.add(branch);
                    neighbors.merge(bus.getId(), 1, Integer::sum);
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
                    busP.compute(bus.getId(), (id, value) -> value == null ? generator.getTargetP() : value + generator.getTargetP());
                    if (!generator.isVoltageRegulatorOn()) {
                        busQ.compute(bus.getId(), (id, value) -> value == null ? generator.getTargetQ() : value + generator.getTargetQ());
                    }
                    if (generator.isVoltageRegulatorOn()) {
                        pvBusIds.add(bus.getId());
                    }
                }

                @Override
                public void visitLoad(Load load) {
                    busP.compute(bus.getId(), (id, value) -> value == null ? -load.getP0() : value - load.getP0());
                    busQ.compute(bus.getId(), (id, value) -> value == null ? -load.getQ0() : value - load.getQ0());
                }

                @Override
                public void visitShuntCompensator(ShuntCompensator sc) {
                    shuntCompensators.add(sc);
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
                    busP.compute(bus.getId(), (id, value) -> value == null ? -p : value - p);
                }
            });
        }
        branches = new ArrayList<>(branchSet);

        mostMeshedBusId = neighbors.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue))
                .orElseThrow(AssertionError::new).getKey();

        slackBusId = buses.get(0).getId();
    }

    public static List<NetworkContext> of(Network network) {
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
                .map(e -> new NetworkContext(network, e.getValue(), hvdcLines))
                .collect(Collectors.toList());
    }

    public Network getNetwork() {
        return network;
    }

    public List<Branch> getBranches() {
        return branches;
    }

    public List<ShuntCompensator> getShuntCompensators() {
        return shuntCompensators;
    }

    public List<Bus> getBuses() {
        return buses;
    }

    public Bus getBus(String id) {
        Objects.requireNonNull(id);
        Bus bus = busesById.get(id);
        if (bus == null) {
            throw new IllegalStateException("Bus '" + id + "' not found");
        }
        return bus;
    }

    public double getBusP(String id) {
        return busP.getOrDefault(id, 0d);
    }

    public double getBusQ(String id) {
        return busQ.getOrDefault(id, 0d);
    }

    public boolean isPvBus(String id) {
        Objects.requireNonNull(id);
        return pvBusIds.contains(id);
    }

    public boolean isSlackBus(String id) {
        Objects.requireNonNull(id);
        return id.equals(slackBusId);
    }

    public Bus getSlackBus() {
        return busesById.get(slackBusId);
    }

    public void setMostMeshedBusAsSlack() {
        slackBusId = mostMeshedBusId;
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

