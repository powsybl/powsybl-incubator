/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNetworkTransformerLeg {

    public enum LegConnectionType {
        Y,
        Y_GROUNDED,
        DELTA;
    }

    double rGround;
    double xGround;

    double coeffRo; // only used for now for 3 windings transformers
    double coeffXo;

    boolean isFreeFluxes; // only used for now for 3 windings transformers

    LegConnectionType legConnectionType;

    public ShortCircuitNetworkTransformerLeg(LegConnectionType legConnectionType) {
        this.legConnectionType = legConnectionType;
        this.rGround = 0.;
        this.xGround = 0.;
    }

    public ShortCircuitNetworkTransformerLeg(LegConnectionType legConnectionType, double coeffRo, double coeffXo) {
        this(legConnectionType);
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
    }

    public ShortCircuitNetworkTransformerLeg(LegConnectionType legConnectionType, double coeffRo, double coeffXo, boolean isFreeFluxes) {
        this(legConnectionType, coeffRo, coeffXo);
        this.isFreeFluxes = isFreeFluxes;
    }

    public ShortCircuitNetworkTransformerLeg(LegConnectionType legConnectionType, double coeffRo, double coeffXo, boolean isFreeFluxes, double rGround, double xGround) {
        this(legConnectionType, coeffRo, coeffXo, isFreeFluxes);
        this.rGround = rGround;
        this.xGround = xGround;
    }

    public void setLegConnectionType(LegConnectionType legConnectionType) {
        this.legConnectionType = legConnectionType;
    }

    public LegConnectionType getLegConnectionType() {
        return legConnectionType;
    }

    public boolean isFreeFluxes() {
        return isFreeFluxes;
    }
}
