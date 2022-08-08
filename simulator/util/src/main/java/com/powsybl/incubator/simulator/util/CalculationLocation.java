/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import org.apache.commons.math3.util.Pair;

import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class CalculationLocation {

    private final String busLocation;

    private final String busLocationBiPhased;

    private final boolean voltageUpdate;

    private Pair<String, Integer > iidmBusInfo; // additional iidm info to make the correspondence between iidm info and lfNetwork info

    private Pair<String, Integer > iidmBus2Info; // additional iidm info to make the correspondence between iidm info and lfNetwork info in case of a biphased common support fault

    private String lfBusInfo; // additional info to have the correspondence between iidm and lfNetwork

    public CalculationLocation(String busLocation, boolean voltageUpdate) {
        this(busLocation, "", voltageUpdate);
    }

    public CalculationLocation(String busLocation, String busLocationBiPhased, boolean voltageUpdate) {
        this.busLocation = Objects.requireNonNull(busLocation);
        this.busLocationBiPhased = Objects.requireNonNull(busLocationBiPhased);
        this.voltageUpdate = voltageUpdate;
    }

    public String getBusLocation() {
        return busLocation;
    }

    public String getBusLocationBiPhased() {
        return busLocationBiPhased;
    }

    public void setIidmBusInfo(Pair<String, Integer> iidmBusInfo) {
        this.iidmBusInfo = iidmBusInfo;
    }

    public void setIidmBus2Info(Pair<String, Integer> iidmBus2Info) {
        this.iidmBus2Info = iidmBus2Info;
    }

    public Pair<String, Integer> getIidmBus2Info() {
        return iidmBus2Info;
    }

    public Pair<String, Integer> getIidmBusInfo() {
        return iidmBusInfo;
    }

    public void setLfBusInfo(String lfBusInfo) {
        this.lfBusInfo = lfBusInfo;
    }

    public String getLfBusInfo() {
        return lfBusInfo;
    }

}
