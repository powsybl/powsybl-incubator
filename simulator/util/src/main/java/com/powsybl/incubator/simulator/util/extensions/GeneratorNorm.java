package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Generator;

public class GeneratorNorm extends AbstractExtension<Generator>  {

    public static final String NAME = "generatorNorm";

    private double kG; // coeff related to the application of a norm, possibly modifying x"d or x'd

    public GeneratorNorm(Generator generator, double kG) {
        super(generator);
        this.kG = kG;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public double getkG() {
        return kG;
    }

    public void setkG(double kG) {
        this.kG = kG;
    }
}
