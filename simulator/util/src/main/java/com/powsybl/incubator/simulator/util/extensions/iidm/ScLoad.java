package com.powsybl.incubator.simulator.util.extensions.iidm;

public class ScLoad {

    private double bdEquivalent;
    private double gdEquivalent;

    public ScLoad(double gdEquivalent, double bdEquivalent) {
        this.bdEquivalent = bdEquivalent;
        this.gdEquivalent = gdEquivalent;
    }

    public double getBdEquivalent() {
        return bdEquivalent;
    }

    public double getGdEquivalent() {
        return gdEquivalent;
    }
}
