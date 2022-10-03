package com.powsybl.incubator.simulator.util.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.incubator.simulator.util.extensions.iidm.ThreeWindingsTransformerShortCircuit;

@AutoService(ExtensionAdderProvider.class)
public class ThreeWindingsTransformerNormAdderImplProvider
        implements ExtensionAdderProvider<ThreeWindingsTransformer, ThreeWindingsTransformerNorm, ThreeWindingsTransformerNormAdder>  {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public String getExtensionName() {
        return ThreeWindingsTransformerShortCircuit.NAME;
    }

    @Override
    public Class<ThreeWindingsTransformerNormAdder> getAdderClass() {
        return ThreeWindingsTransformerNormAdder.class;
    }

    @Override
    public ThreeWindingsTransformerNormAdder newAdder(ThreeWindingsTransformer twt) {
        return new ThreeWindingsTransformerNormAdder(twt);
    }
}
