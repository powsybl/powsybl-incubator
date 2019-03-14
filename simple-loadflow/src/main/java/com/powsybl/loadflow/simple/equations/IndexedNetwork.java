/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class IndexedNetwork {

    private final Network network;

    private final List<Bus> buses;

    private final Map<Bus, Integer> index;

    public IndexedNetwork(Network network) {
        this.network = Objects.requireNonNull(network);
        buses = network.getBusView().getBusStream().collect(ImmutableList.toImmutableList());
        index = IntStream.range(0, buses.size()).boxed().collect(ImmutableMap.toImmutableMap(i -> buses.get((int) i), i -> i));
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
}

