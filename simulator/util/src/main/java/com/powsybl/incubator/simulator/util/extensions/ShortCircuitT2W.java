/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitT2W {

    private final ShortCircuitTransformerLeg leg1;
    private final ShortCircuitTransformerLeg leg2;

    private final double coeffXo; // Xo = Xd * coeffXo : value of the homopolar admittance (in ohms) expressed at the leg2 side
    private final double coeffRo; // Ro = Rd * coeffRo : value of the homopolar resistance (in ohms) expressed at the leg2 side

    private final boolean freeFluxes;

    private final double kT; //correction factor of the Two Windings Transformer

    ShortCircuitT2W(ShortCircuitTransformerLeg leg1, ShortCircuitTransformerLeg leg2, double coeffRo, double coeffXo, boolean freeFluxes) {
        this(leg1, leg2, coeffRo, coeffXo, freeFluxes, 1d);
    }

    ShortCircuitT2W(ShortCircuitTransformerLeg leg1, ShortCircuitTransformerLeg leg2, double coeffRo, double coeffXo, boolean freeFluxes, double kT) {
        this.leg1 = Objects.requireNonNull(leg1);
        this.leg2 = Objects.requireNonNull(leg2);
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
        this.freeFluxes = freeFluxes;
        this.kT = kT;
    }

    public ShortCircuitTransformerLeg getLeg1() {
        return leg1;
    }

    public ShortCircuitTransformerLeg getLeg2() {
        return leg2;
    }

    public double getCoeffXo() {
        return coeffXo;
    }

    public double getCoeffRo() {
        return coeffRo;
    }

    public boolean isFreeFluxes() {
        return freeFluxes;
    }

    public double getkT() {
        return kT;
    }
}
