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
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ThreeWindingsTransformerFortescue extends AbstractExtension<ThreeWindingsTransformer> {

    public class T3wLeg {

        private double legRo;
        private double legXo;
        private boolean legFreeFluxes;
        private LegConnectionType legConnectionType;

        public T3wLeg(double ro, double xo, boolean legFreeFluxes, LegConnectionType legConnectionType) {
            this.legRo = ro;
            this.legXo = xo;
            this.legFreeFluxes = legFreeFluxes;
            this.legConnectionType = Objects.requireNonNull(legConnectionType);
        }

        public LegConnectionType getLegConnectionType() {
            return legConnectionType;
        }

        public boolean isLegFreeFluxes() {
            return legFreeFluxes;
        }

        public double getLegRo() {
            return legRo;
        }

        public double getLegXo() {
            return legXo;
        }

        public void setLegRo(double legRo) {
            this.legRo = legRo;
        }

        public void setLegXo(double legXo) {
            this.legXo = legXo;
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

    public ThreeWindingsTransformerFortescue(ThreeWindingsTransformer extendable,
                                             double leg1Ro, double leg2Ro, double leg3Ro,
                                             double leg1Xo, double leg2Xo, double leg3Xo,
                                             boolean leg1FreeFluxes, boolean leg2FreeFluxes, boolean leg3FreeFluxes,
                                             LegConnectionType leg1ConnectionType, LegConnectionType leg2ConnectionType, LegConnectionType leg3ConnectionType) {
        super(extendable);

        this.leg1 = new T3wLeg(leg1Ro, leg1Xo, leg1FreeFluxes, leg1ConnectionType);
        this.leg2 = new T3wLeg(leg2Ro, leg2Xo, leg2FreeFluxes, leg2ConnectionType);
        this.leg3 = new T3wLeg(leg3Ro, leg3Xo, leg3FreeFluxes, leg3ConnectionType);

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
