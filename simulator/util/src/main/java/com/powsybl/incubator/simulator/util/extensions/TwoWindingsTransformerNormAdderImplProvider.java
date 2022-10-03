package com.powsybl.incubator.simulator.util.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.incubator.simulator.util.extensions.iidm.TwoWindingsTransformerShortCircuit;

@AutoService(ExtensionAdderProvider.class)
public class TwoWindingsTransformerNormAdderImplProvider
        implements ExtensionAdderProvider<TwoWindingsTransformer, TwoWindingsTransformerNorm, TwoWindingsTransformerNormAdder>  {
    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public String getExtensionName() {
        return TwoWindingsTransformerShortCircuit.NAME;
    }

    @Override
    public Class<TwoWindingsTransformerNormAdder> getAdderClass() {
        return TwoWindingsTransformerNormAdder.class;
    }

    @Override
    public TwoWindingsTransformerNormAdder newAdder(TwoWindingsTransformer twt) {
        return new TwoWindingsTransformerNormAdder(twt);
    }
}
