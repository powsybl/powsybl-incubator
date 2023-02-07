package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.Generator;

@AutoService(ExtensionAdderProvider.class)
public class GeneratorFortescueAdderImplProvider implements ExtensionAdderProvider<Generator, GeneratorFortescue, GeneratorFortescueAdder> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public String getExtensionName() {
        return GeneratorFortescue.NAME;
    }

    @Override
    public Class<GeneratorFortescueAdder> getAdderClass() {
        return GeneratorFortescueAdder.class;
    }

    @Override
    public GeneratorFortescueAdder newAdder(Generator generator) {
        return new GeneratorFortescueAdder(generator);
    }
}
