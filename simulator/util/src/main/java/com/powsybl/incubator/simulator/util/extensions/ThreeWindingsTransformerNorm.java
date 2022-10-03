package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.ThreeWindingsTransformer;

public class ThreeWindingsTransformerNorm extends AbstractExtension<ThreeWindingsTransformer> {

    public class T3wLeg {

        private double legCoeffRoOverload;
        private double legCoeffXoOverload;
        private double ktR;
        private double ktX;
        private double ktRo;
        private double ktXo;

        public T3wLeg(double legCoeffRoOverload, double legCoeffXoOverload,
                      double ktR, double ktX, double ktRo, double ktXo) {
            this.legCoeffRoOverload = legCoeffRoOverload;
            this.legCoeffXoOverload = legCoeffXoOverload;
            this.ktR = ktR;
            this.ktX = ktX;
            this.ktRo = ktRo;
            this.ktXo = ktXo;
        }

        public double getKtR() {
            return ktR;
        }

        public double getKtRo() {
            return ktRo;
        }

        public double getKtX() {
            return ktX;
        }

        public double getKtXo() {
            return ktXo;
        }

        public double getLegCoeffRoOverload() {
            return legCoeffRoOverload;
        }

        public double getLegCoeffXoOverload() {
            return legCoeffXoOverload;
        }

        public void setKtR(double ktR) {
            this.ktR = ktR;
        }

        public void setKtX(double ktX) {
            this.ktX = ktX;
        }

        public void setKtRo(double ktRo) {
            this.ktRo = ktRo;
        }

        public void setKtXo(double ktXo) {
            this.ktXo = ktXo;
        }

        public void setLegCoeffRoOverload(double legCoeffRo) {
            this.legCoeffRoOverload = legCoeffRo;
        }

        public void setLegCoeffXoOverload(double legCoeffXo) {
            this.legCoeffXoOverload = legCoeffXo;
        }

    }

    public static final String NAME = "threeWindingsTransformerNorm";

    private ThreeWindingsTransformerNorm.T3wLeg leg1;
    private ThreeWindingsTransformerNorm.T3wLeg leg2;
    private ThreeWindingsTransformerNorm.T3wLeg leg3;
    private boolean isOverloadHomopolarCoefs;

    @Override
    public String getName() {
        return NAME;
    }

    public ThreeWindingsTransformerNorm(ThreeWindingsTransformer extendable,
                                        double leg1CoeffRo, double leg2CoeffRo, double leg3CoeffRo,
                                        double leg1CoeffXo, double leg2CoeffXo, double leg3CoeffXo,
                                        double kt1R, double kt1X, double kt2R, double kt2X, double kt3R, double kt3X,
                                        double kt1Ro, double kt1Xo, double kt2Ro, double kt2Xo, double kt3Ro, double kt3Xo, boolean isOverloadHomopolarCoefs) {
        super(extendable);

        this.leg1 = new ThreeWindingsTransformerNorm.T3wLeg(leg1CoeffRo, leg1CoeffXo,
                kt1R, kt1X, kt1Ro, kt1Xo);
        this.leg2 = new ThreeWindingsTransformerNorm.T3wLeg(leg2CoeffRo, leg2CoeffXo,
                kt2R, kt2X, kt2Ro, kt2Xo);
        this.leg3 = new ThreeWindingsTransformerNorm.T3wLeg(leg3CoeffRo, leg3CoeffXo,
                kt3R, kt3X, kt3Ro, kt3Xo);
        this.isOverloadHomopolarCoefs = isOverloadHomopolarCoefs;

    }

    public ThreeWindingsTransformerNorm.T3wLeg getLeg1() {
        return leg1;
    }

    public ThreeWindingsTransformerNorm.T3wLeg getLeg2() {
        return leg2;
    }

    public ThreeWindingsTransformerNorm.T3wLeg getLeg3() {
        return leg3;
    }

    public boolean isOverloadHomopolarCoefs() {
        return isOverloadHomopolarCoefs;
    }

    public void setOverloadHomopolarCoefs(boolean overloadHomopolarCoefs) {
        isOverloadHomopolarCoefs = overloadHomopolarCoefs;
    }
}
