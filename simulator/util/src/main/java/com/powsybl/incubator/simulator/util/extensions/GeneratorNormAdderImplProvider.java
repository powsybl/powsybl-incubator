package com.powsybl.incubator.simulator.util.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.Generator;

@AutoService(ExtensionAdderProvider.class)
public class GeneratorNormAdderImplProvider
        implements ExtensionAdderProvider<Generator, GeneratorNorm, GeneratorNormAdder> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public String getExtensionName() {
        return GeneratorNorm.NAME;
    }

    @Override
    public Class<GeneratorNormAdder> getAdderClass() {
        return GeneratorNormAdder.class;
    }

    @Override
    public GeneratorNormAdder newAdder(Generator generator) {
        return new GeneratorNormAdder(generator);
    }
}
