/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNetworkT2W {

    ShortCircuitNetworkTransformerLeg leg1;
    ShortCircuitNetworkTransformerLeg leg2;

    double coeffXo; // Xo = Xd * coeffXo : value of the homopolar admittance (in ohms) expressed at the leg2 side
    double coeffRo; // Ro = Rd * coeffRo : value of the homopolar resistance (in ohms) expressed at the leg2 side

    double kT; //correction factor of the Two Windings Transformer

    boolean isFreeFluxes;

    ShortCircuitNetworkT2W(ShortCircuitNetworkTransformerLeg leg1, ShortCircuitNetworkTransformerLeg leg2, double coeffRo, double coeffXo, boolean isFreeFluxes) {
        this.leg1 = leg1;
        this.leg2 = leg2;
        double kT = 1.0;
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
        this.isFreeFluxes = isFreeFluxes;
    }

    ShortCircuitNetworkT2W(ShortCircuitNetworkTransformerLeg leg1, ShortCircuitNetworkTransformerLeg leg2, double coeffRo, double coeffXo, boolean isFreeFluxes, double kT) {
        this(leg1, leg2, coeffRo, coeffXo, isFreeFluxes);
        this.kT = kT;
    }

    public double getCoeffXo() {
        return coeffXo;
    }

    public double getCoeffRo() {
        return coeffRo;
    }

    public ShortCircuitNetworkTransformerLeg getLeg1() {
        return leg1;
    }

    public ShortCircuitNetworkTransformerLeg getLeg2() {
        return leg2;
    }
}
