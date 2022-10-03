package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.ThreeWindingsTransformer;

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;
import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.DEFAULT_COEFF_K;

public class ThreeWindingsTransformerNormAdder extends AbstractExtensionAdder<ThreeWindingsTransformer, ThreeWindingsTransformerNorm> {

    private boolean isOverloadHomopolarCoefs = false;
    private double leg1CoeffRoOverload = DEFAULT_COEFF_RO;
    private double leg2CoeffRoOverload = DEFAULT_COEFF_RO;
    private double leg3CoeffRoOverload = DEFAULT_COEFF_RO;
    private double leg1CoeffXoOverload = DEFAULT_COEFF_XO;
    private double leg2CoeffXoOverload = DEFAULT_COEFF_XO;
    private double leg3CoeffXoOverload = DEFAULT_COEFF_XO;
    private double kt1R = DEFAULT_COEFF_K;
    private double kt1X = DEFAULT_COEFF_K;
    private double kt2R = DEFAULT_COEFF_K;
    private double kt2X = DEFAULT_COEFF_K;
    private double kt3R = DEFAULT_COEFF_K;
    private double kt3X = DEFAULT_COEFF_K;
    private double kt1Ro = DEFAULT_COEFF_K;
    private double kt1Xo = DEFAULT_COEFF_K;
    private double kt2Ro = DEFAULT_COEFF_K;
    private double kt2Xo = DEFAULT_COEFF_K;
    private double kt3Ro = DEFAULT_COEFF_K;
    private double kt3Xo = DEFAULT_COEFF_K;

    public ThreeWindingsTransformerNormAdder(ThreeWindingsTransformer twt) {
        super(twt);
    }

    @Override
    public Class<? super ThreeWindingsTransformerNorm> getExtensionClass() {
        return ThreeWindingsTransformerNorm.class;
    }

    @Override
    protected ThreeWindingsTransformerNorm createExtension(ThreeWindingsTransformer twt) {
        return new ThreeWindingsTransformerNorm(twt,
                leg1CoeffRoOverload, leg2CoeffRoOverload, leg3CoeffRoOverload,
                leg1CoeffXoOverload, leg2CoeffXoOverload, leg3CoeffXoOverload,
                kt1R, kt1X, kt2R, kt2X, kt3R, kt3X,
                kt1Ro, kt1Xo, kt2Ro, kt2Xo, kt3Ro, kt3Xo, isOverloadHomopolarCoefs);
    }

    public ThreeWindingsTransformerNormAdder withLeg1CoeffRoOverload(double leg1CoeffRo) {
        this.leg1CoeffRoOverload = leg1CoeffRo;
        return this;
    }

    public ThreeWindingsTransformerNormAdder withLeg2CoeffRoOverload(double leg2CoeffRo) {
        this.leg2CoeffRoOverload = leg2CoeffRo;
        return this;
    }

    public ThreeWindingsTransformerNormAdder withLeg3CoeffRoOverload(double leg3CoeffRo) {
        this.leg3CoeffRoOverload = leg3CoeffRo;
        return this;
    }

    public ThreeWindingsTransformerNormAdder withLeg1CoeffXoOverload(double leg1CoeffXo) {
        this.leg1CoeffXoOverload = leg1CoeffXo;
        return this;
    }

    public ThreeWindingsTransformerNormAdder withLeg2CoeffXoOverload(double leg2CoeffXo) {
        this.leg2CoeffXoOverload = leg2CoeffXo;
        return this;
    }

    public ThreeWindingsTransformerNormAdder withLeg3CoeffXoOverload(double leg3CoeffXo) {
        this.leg3CoeffXoOverload = leg3CoeffXo;
        return this;
    }
}
