package com.powsybl.incubator.simulator.util.extensions;

public class GeneratorNorm  {

    public static final String NAME = "generatorNorm";

    private double kG; // coeff related to the application of a norm, possibly modifying x"d or x'd

    public GeneratorNorm(double kG) {
        this.kG = kG;
    }

    public double getkG() {
        return kG;
    }

    public void setkG(double kG) {
        this.kG = kG;
    }
}
