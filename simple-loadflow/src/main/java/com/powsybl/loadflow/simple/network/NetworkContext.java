/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.google.common.collect.ImmutableList;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NetworkContext {

    private final Network network;

    private final List<Bus> buses;

    private final List<Branch> branches = new ArrayList<>();

    private final Map<String, Bus> busesById;

    private final String slackBusId;

    private final Set<String> pvBusIds = new HashSet<>();

    private final Map<String, Double> busP = new HashMap<>();
    private final Map<String, Double> busQ = new HashMap<>();

    public NetworkContext(Network network) {
        this.network = Objects.requireNonNull(network);
        if (network.getDanglingLineCount() > 0) {
            throw new UnsupportedOperationException("TODO: dangling lines");
        }
        if (network.getThreeWindingsTransformerCount() > 0) {
            throw new UnsupportedOperationException("TODO: 3 windings transformers");
        }
        buses = network.getBusView().getBusStream().filter(Bus::isInMainConnectedComponent).collect(ImmutableList.toImmutableList());
        busesById = buses.stream().collect(Collectors.toMap(Identifiable::getId, b -> b));

        Map<String, Integer> neighbors = new HashMap<>();
        for (Branch branch : network.getBranches()) {
            Bus bus1 = branch.getTerminal1().getBusView().getBus();
            Bus bus2 = branch.getTerminal2().getBusView().getBus();
            boolean cc1 = bus1 != null && bus1.isInMainConnectedComponent();
            boolean cc2 = bus2 != null && bus2.isInMainConnectedComponent();
            if (cc1) {
                neighbors.merge(bus1.getId(), 1, Integer::sum);
            }
            if (cc2) {
                neighbors.merge(bus2.getId(), 1, Integer::sum);
            }
            if (cc1 || cc2) {
                branches.add(branch);
            }
        }

        for (Generator gen : network.getGenerators()) {
            Bus bus = gen.getTerminal().getBusView().getBus();
            if (bus == null || !bus.isInMainConnectedComponent()) {
                continue;
            }
            busP.compute(bus.getId(), (id, value) -> value == null ? gen.getTargetP() : value + gen.getTargetP());
            if (!gen.isVoltageRegulatorOn()) {
                busQ.compute(bus.getId(), (id, value) -> value == null ? gen.getTargetQ() : value + gen.getTargetQ());
            }
            if (gen.isVoltageRegulatorOn()) {
                pvBusIds.add(bus.getId());
            }
        }

        slackBusId = buses.get(0).getId();

        for (Load load : network.getLoads()) {
            Bus bus = load.getTerminal().getBusView().getBus();
            if (bus == null || !bus.isInMainConnectedComponent()) {
                continue;
            }
            busP.compute(bus.getId(), (id, value) -> value == null ? -load.getP0() : value - load.getP0());
            busQ.compute(bus.getId(), (id, value) -> value == null ? -load.getQ0() : value - load.getQ0());
        }

        for (ShuntCompensator sc : network.getShuntCompensators()) {
            throw new UnsupportedOperationException("TODO: shunt compensators");
        }

        for (HvdcLine line : network.getHvdcLines()) {
            Bus bus1 = line.getConverterStation1().getTerminal().getBusView().getBus();
            Bus bus2 = line.getConverterStation2().getTerminal().getBusView().getBus();
            double p = line.getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER
                    ? line.getActivePowerSetpoint()
                    : -line.getActivePowerSetpoint();
            if (bus1 != null && bus1.isInMainConnectedComponent()) {
                busP.compute(bus1.getId(), (id, value) -> value == null ? -p : value - p);
            }
            if (bus2 != null && bus2.isInMainConnectedComponent()) {
                busP.compute(bus2.getId(), (id, value) -> value == null ? p : value + p);
            }
        }
    }

    public static NetworkContext of(Network network) {
        return new NetworkContext(network);
    }

    public Network getNetwork() {
        return network;
    }

    public List<Branch> getBranches() {
        return branches;
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

    public void resetState() {
        for (Bus b : network.getBusView().getBuses()) {
            b.setV(Double.NaN);
            b.setAngle(Double.NaN);
        }
        for (Branch b : network.getBranches()) {
            b.getTerminal1().setP(Double.NaN);
            b.getTerminal1().setQ(Double.NaN);
            b.getTerminal2().setP(Double.NaN);
            b.getTerminal2().setQ(Double.NaN);
        }
    }
}

