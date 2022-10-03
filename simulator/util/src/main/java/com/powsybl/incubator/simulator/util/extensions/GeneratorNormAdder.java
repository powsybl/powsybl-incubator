package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.Generator;

public class GeneratorNormAdder extends AbstractExtensionAdder<Generator, GeneratorNorm> {

    private  double kG = 1.;

    public GeneratorNormAdder(Generator generator) {
        super(generator);
    }

    @Override
    public Class<? super GeneratorNorm> getExtensionClass() {
        return GeneratorNorm.class;
    }

    @Override
    protected GeneratorNorm createExtension(Generator generator) {
        return new GeneratorNorm(generator, kG);
    }

    public GeneratorNormAdder withKg(double kG) {
        this.kG = kG;
        return this;
    }

}
