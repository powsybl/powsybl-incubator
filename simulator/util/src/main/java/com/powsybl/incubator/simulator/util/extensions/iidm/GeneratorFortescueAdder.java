package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.Generator;

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;

public class GeneratorFortescueAdder extends AbstractExtensionAdder<Generator, GeneratorFortescue> {

    private boolean toGround = DEFAULT_TO_GROUND;
    private double ro = 0.;
    private double xo = 0.;
    private double ri = 0.;
    private double xi = 0.;
    private double groundingR = DEFAULT_GROUNDING_R;
    private double groundingX = DEFAULT_GROUNDING_X;
    private GeneratorFortescue.GeneratorType generatorType = DEFAULT_GENERATOR_FORTESCUE_TYPE;

    public GeneratorFortescueAdder(Generator generator) {
        super(generator);
    }

    @Override
    public Class<? super GeneratorFortescue> getExtensionClass() {
        return GeneratorFortescue.class;
    }

    @Override
    protected GeneratorFortescue createExtension(Generator generator) {
        return new GeneratorFortescue(generator, toGround, ro, xo, ri, xi, groundingR, groundingX, generatorType);
    }

    public GeneratorFortescueAdder withToGround(boolean toGround) {
        this.toGround = toGround;
        return this;
    }

    public GeneratorFortescueAdder withRo(double ro) {
        this.ro = ro;
        return this;
    }

    public GeneratorFortescueAdder withXo(double xo) {
        this.xo = xo;
        return this;
    }

    public GeneratorFortescueAdder withRi(double ri) {
        this.ri = ri;
        return this;
    }

    public GeneratorFortescueAdder withXi(double xi) {
        this.xi = xi;
        return this;
    }

    public GeneratorFortescueAdder withGroundingR(double groundingR) {
        this.groundingR = groundingR;
        return this;
    }

    public GeneratorFortescueAdder withGroundingX(double groundingX) {
        this.groundingX = groundingX;
        return this;
    }

    public GeneratorFortescueAdder withGeneratorType(GeneratorFortescue.GeneratorType generatorType) {
        this.generatorType = generatorType;
        return this;
    }
}
