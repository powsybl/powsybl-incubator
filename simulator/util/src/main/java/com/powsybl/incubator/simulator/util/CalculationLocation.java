package com.powsybl.incubator.simulator.util;

import org.apache.commons.math3.util.Pair;

public class CalculationLocation {

    public CalculationLocation(String input, boolean voltageUpdate) {
        this.voltageUpdate = voltageUpdate;
        this.busLocation = input;
        this.busLocationBiPhased = "";
    }

    public CalculationLocation(String input, String input2, boolean voltageUpdate) {
        this(input, voltageUpdate);
        this.busLocationBiPhased = input2;
    }

    private String busLocation;
    private String busLocationBiPhased;

    private boolean voltageUpdate;

    private Pair<String, Integer > iidmBusInfo; // additional iidm info to make the correspondence between iidm info and lfNetwork info

    private Pair<String, Integer > iidmBus2Info; // additional iidm info to make the correspondence between iidm info and lfNetwork info in case of a biphased common support fault

    private String lfBusInfo; // additional info to have the correspondence between iidm and lfNetwork

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
