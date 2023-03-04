package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Generator;

public class GeneratorFortescue extends AbstractExtension<Generator> {

    public static final String NAME = "generatorFortescue";

    public enum GeneratorType {
        UNKNOWN,
        ROTATING_MACHINE,
        FEEDER;
    }

    private boolean toGround;
    private final double ro;
    private final double xo;
    private final double ri;
    private final double xi;
    private final double groundingR;
    private final double groundingX;
    private final GeneratorType generatorType;

    @Override
    public String getName() {
        return NAME;
    }

    public GeneratorFortescue(Generator generator, boolean toGround, double ro, double xo, double ri, double xi, double groundingR, double groundingX, GeneratorType generatorType) {
        super(generator);
        this.toGround = toGround;
        this.ro = ro;
        this.xo = xo;
        this.ri = ri;
        this.xi = xi;
        this.groundingR = groundingR;
        this.groundingX = groundingX;
        this.generatorType = generatorType;

    }

    public boolean isToGround() {
        return toGround;
    }

    public double getGroundingR() {
        return groundingR;
    }

    public double getGroundingX() {
        return groundingX;
    }

    public double getRo() {
        return ro;
    }

    public double getXo() {
        return xo;
    }

    public double getRi() {
        return ri;
    }

    public double getXi() {
        return xi;
    }

    public GeneratorType getGeneratorType() {
        return generatorType;
    }

    public void setToGround(boolean toGround) {
        this.toGround = toGround;
    }
}
