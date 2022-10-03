package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.TwoWindingsTransformer;

public class TwoWindingsTransformerNormAdder extends AbstractExtensionAdder<TwoWindingsTransformer, TwoWindingsTransformerNorm> {

    private double kNorm = 1.;

    public TwoWindingsTransformerNormAdder(TwoWindingsTransformer twt) {
        super(twt);
    }

    @Override
    public Class<? super TwoWindingsTransformerNorm> getExtensionClass() {
        return TwoWindingsTransformerNorm.class;
    }

    @Override
    protected TwoWindingsTransformerNorm createExtension(TwoWindingsTransformer twt) {
        return new TwoWindingsTransformerNorm(twt, kNorm);
    }

    public TwoWindingsTransformerNormAdder withKnorm(double kNorm) {
        this.kNorm = kNorm;
        return this;
    }
}
