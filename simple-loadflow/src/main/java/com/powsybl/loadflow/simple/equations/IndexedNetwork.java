/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.IntStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class IndexedNetwork {

    private final Network network;

    private final List<Bus> buses;

    private final Map<Bus, Integer> index;

    static class Neighbor {

        final Branch branch;
        final Branch.Side side;

        Neighbor(Branch branch, Branch.Side side) {
            this.branch = branch;
            this.side = side;
        }

        @Override
        public String toString() {
            return "Neighbor(branchId='" + branch.getId() + "', side=" + side + ")";
        }
    }

    private final List<Branch> branches = new ArrayList<>();

    private final SortedMap<Integer, SortedMap<Integer, List<Neighbor>>> neighbors = new TreeMap<>();

    public IndexedNetwork(Network network) {
        this.network = Objects.requireNonNull(network);
        buses = network.getBusView().getBusStream().filter(Bus::isInMainConnectedComponent).collect(ImmutableList.toImmutableList());
        index = IntStream.range(0, buses.size()).boxed().collect(ImmutableMap.toImmutableMap(i -> buses.get((int) i), i -> i));
        for (Branch br : network.getBranches()) {
            Bus bus1 = br.getTerminal1().getBusView().getBus();
            Bus bus2 = br.getTerminal2().getBusView().getBus();
            if (bus1 != null && bus1.isInMainConnectedComponent() && bus2 != null && bus2.isInMainConnectedComponent()) {
                branches.add(br);

                int busNum1 = index.get(bus1);
                int busNum2 = index.get(bus2);

                neighbors.computeIfAbsent(busNum1, k -> new TreeMap<>())
                        .computeIfAbsent(busNum1, k -> new ArrayList<>())
                        .add(new Neighbor(br, Branch.Side.ONE));
                neighbors.computeIfAbsent(busNum1, k -> new TreeMap<>())
                        .computeIfAbsent(busNum2, k -> new ArrayList<>())
                        .add(new Neighbor(br, Branch.Side.ONE));

                neighbors.computeIfAbsent(busNum2, k -> new TreeMap<>())
                        .computeIfAbsent(busNum2, k -> new ArrayList<>())
                        .add(new Neighbor(br, Branch.Side.TWO));
                neighbors.computeIfAbsent(busNum2, k -> new TreeMap<>())
                        .computeIfAbsent(busNum1, k -> new ArrayList<>())
                        .add(new Neighbor(br, Branch.Side.TWO));
            }
        }
    }

    public static IndexedNetwork of(Network network) {
        return new IndexedNetwork(network);
    }

    public Network get() {
        return network;
    }

    public int getBusCount() {
        return buses.size();
    }

    public List<Bus> getBuses() {
        return buses;
    }

    public Bus getBus(int index) {
        return buses.get(index);
    }

    public Integer getIndex(Bus bus) {
        return index.get(bus);
    }

    public List<Branch> getBranches() {
        return branches;
    }

    interface BranchHandler {

        void onBranch(int row, int column, Branch branch, Branch.Side side);
    }

    public void forEachBranchInCscOrder(BranchHandler branchHandler) {
        for (SortedMap.Entry<Integer, SortedMap<Integer, List<Neighbor>>> e1 : neighbors.entrySet()) {
            int busNum2 = e1.getKey();
            for (SortedMap.Entry<Integer, List<Neighbor>> e2 : e1.getValue().entrySet()) {
                int busNum1 = e2.getKey();
                for (Neighbor neighbor : e2.getValue()) {
                    branchHandler.onBranch(busNum1, busNum2, neighbor.branch, neighbor.side);
                }
            }
        }
    }
}

