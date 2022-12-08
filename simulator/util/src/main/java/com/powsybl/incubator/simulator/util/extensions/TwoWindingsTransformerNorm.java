package com.powsybl.incubator.simulator.util.extensions;

public class TwoWindingsTransformerNorm {

    public static final String NAME = "twoWindingsTransformerNorm";

    private double kNorm; // coef used by the chosen norm to modify R, X, Ro and Xo

    public TwoWindingsTransformerNorm(double kNorm) {
        this.kNorm = kNorm;
    }

    public double getkNorm() {
        return kNorm;
    }

    public void setkNorm(double kNorm) {
        this.kNorm = kNorm;
    }
}
