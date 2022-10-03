package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.TwoWindingsTransformer;

public class TwoWindingsTransformerNorm extends AbstractExtension<TwoWindingsTransformer> {

    public static final String NAME = "twoWindingsTransformerNorm";

    private double kNorm; // coef used by the chosen norm to modify R, X, Ro and Xo

    @Override
    public String getName() {
        return NAME;
    }

    public TwoWindingsTransformerNorm(TwoWindingsTransformer twt, double kNorm) {
        super(twt);
        this.kNorm = kNorm;
    }

    public double getkNorm() {
        return kNorm;
    }

    public void setkNorm(double kNorm) {
        this.kNorm = kNorm;
    }
}
