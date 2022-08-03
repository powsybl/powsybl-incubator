/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import org.apache.commons.math3.util.Pair;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitFault {

    public ShortCircuitFault(String input, double zfr, double zfi, ShortCircuitType type, boolean voltageUpdate) {
        this.zfr = zfr;
        this.zfi = zfi;
        this.voltageUpdate = voltageUpdate;
        this.shortCircuitVoltageLevelLocation = "";
        this.shortCircuitVoltageLevelLocationBiPhased = "";
        this.busLocation = input;
        this.busLocationBiPhased = "";
        this.type = type;
        this.inputByBus = false;
    }

    public ShortCircuitFault(String input, String input2, double zfr, double zfi, ShortCircuitType type, boolean voltageUpdate, ShortCircuitBiphasedType biphasedType) {
        this(input, zfr, zfi, type, voltageUpdate);
        this.busLocationBiPhased = input2;
        this.biphasedType = biphasedType;
    }


    public enum ShortCircuitType {
        TRIPHASED_GROUND,
        BIPHASED,
        BIPHASED_GROUND,
        BIPHASED_COMMON_SUPPORT,
        MONOPHASED;
    }

    public enum ShortCircuitBiphasedType {
        C1_C2,
        C1_B2,
        C1_A2;
    }

    // TODO : remove once input by bus is OK
    private String shortCircuitVoltageLevelLocation;
    private String shortCircuitVoltageLevelLocationBiPhased;

    private boolean inputByBus; // true if input given by bus

    private String busLocation;
    private String busLocationBiPhased;

    private double zfr; //real part of the short circuit impedance Zf
    private double zfi; //imaginary part of the short circuit impedance Zf

    private boolean voltageUpdate;

    private ShortCircuitType type;

    private ShortCircuitBiphasedType biphasedType;

    private Pair<String, Integer > iidmBusInfo; // additional iidm info to make the correspondence between iidm info and lfNetwork info

    private Pair<String, Integer > iidmBus2Info; // additional iidm info to make the correspondence between iidm info and lfNetwork info in case of a biphased common support fault

    private String lfBusInfo; // additional info to have the correspondence between iidm and lfNetwork

    public String getShortCircuitVoltageLevelLocation() {
        return shortCircuitVoltageLevelLocation;
    }

    public String getBusLocation() {
        return busLocation;
    }

    public String getBusLocationBiPhased() {
        return busLocationBiPhased;
    }

    public boolean isInputByBus() {
        return inputByBus;
    }

    public ShortCircuitType getType() {
        return type;
    }

    public double getZfr() {
        return zfr;
    }

    public double getZfi() {
        return zfi;
    }

    public ShortCircuitBiphasedType getBiphasedType() {
        return biphasedType;
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
