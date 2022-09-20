/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.ThreeWindingsTransformer;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class ThreeWindingsTransformerShortCircuit extends AbstractExtension<ThreeWindingsTransformer> {

    public class T3wLeg {

        private double legCoeffRo;
        private double legCoeffXo;
        private boolean legFreeFluxes;
        private LegConnectionType legConnectionType;
        private double ktR;
        private double ktX;
        private double ktRo;
        private double ktXo;

        public T3wLeg(double legCoeffRo, double legCoeffXo, boolean legFreeFluxes, LegConnectionType legConnectionType,
                      double ktR, double ktX, double ktRo, double ktXo) {
            this.legCoeffRo = legCoeffRo;
            this.legCoeffXo = legCoeffXo;
            this.legFreeFluxes = legFreeFluxes;
            this.legConnectionType = Objects.requireNonNull(legConnectionType);
            this.ktR = ktR;
            this.ktX = ktX;
            this.ktRo = ktRo;
            this.ktXo = ktXo;
        }

        public LegConnectionType getLegConnectionType() {
            return legConnectionType;
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

        public boolean isLegFreeFluxes() {
            return legFreeFluxes;
        }

        public double getLegCoeffRo() {
            return legCoeffRo;
        }

        public double getLegCoeffXo() {
            return legCoeffXo;
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

        public void setLegCoeffRo(double legCoeffRo) {
            this.legCoeffRo = legCoeffRo;
        }

        public void setLegCoeffXo(double legCoeffXo) {
            this.legCoeffXo = legCoeffXo;
        }

        public void setLegConnectionType(LegConnectionType legConnectionType) {
            this.legConnectionType = legConnectionType;
        }
    }

    public static final String NAME = "threeWindingsTransformerShortCircuit";

    private T3wLeg leg1;
    private T3wLeg leg2;
    private T3wLeg leg3;

    @Override
    public String getName() {
        return NAME;
    }

    public ThreeWindingsTransformerShortCircuit(ThreeWindingsTransformer extendable,
                                                double leg1CoeffRo, double leg2CoeffRo, double leg3CoeffRo,
                                                double leg1CoeffXo, double leg2CoeffXo, double leg3CoeffXo,
                                                boolean leg1FreeFluxes, boolean leg2FreeFluxes, boolean leg3FreeFluxes,
                                                LegConnectionType leg1ConnectionType, LegConnectionType leg2ConnectionType, LegConnectionType leg3ConnectionType,
                                                double kt1R, double kt1X, double kt2R, double kt2X, double kt3R, double kt3X,
                                                double kt1Ro, double kt1Xo, double kt2Ro, double kt2Xo, double kt3Ro, double kt3Xo) {
        super(extendable);

        this.leg1 = new T3wLeg(leg1CoeffRo, leg1CoeffXo, leg1FreeFluxes, leg1ConnectionType,
        kt1R, kt1X, kt1Ro, kt1Xo);
        this.leg2 = new T3wLeg(leg2CoeffRo, leg2CoeffXo, leg2FreeFluxes, leg2ConnectionType,
                kt2R, kt2X, kt2Ro, kt2Xo);
        this.leg3 = new T3wLeg(leg3CoeffRo, leg3CoeffXo, leg3FreeFluxes, leg3ConnectionType,
                kt3R, kt3X, kt3Ro, kt3Xo);

    }

    public T3wLeg getLeg1() {
        return leg1;
    }

    public T3wLeg getLeg2() {
        return leg2;
    }

    public T3wLeg getLeg3() {
        return leg3;
    }

}
