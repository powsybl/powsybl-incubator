/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

import com.powsybl.incubator.simulator.util.extensions.iidm.LegConnectionType;

import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitT2W {

    private final LegConnectionType leg1ConnectionType;
    private final LegConnectionType leg2ConnectionType;

    private final double coeffXo; // Xo = Xd * coeffXo : value of the homopolar admittance (in ohms) expressed at the leg2 side
    private final double coeffRo; // Ro = Rd * coeffRo : value of the homopolar resistance (in ohms) expressed at the leg2 side

    private final boolean freeFluxes;

    private final double kT; //correction factor of the Two Windings Transformer

    ShortCircuitT2W(LegConnectionType leg1ConnectionType, LegConnectionType leg2ConnectionType, double coeffRo, double coeffXo, boolean freeFluxes) {
        this(leg1ConnectionType, leg2ConnectionType, coeffRo, coeffXo, freeFluxes, 1d);
    }

    ShortCircuitT2W(LegConnectionType leg1ConnectionType, LegConnectionType leg2ConnectionType, double coeffRo, double coeffXo, boolean freeFluxes, double kT) {
        this.leg1ConnectionType = Objects.requireNonNull(leg1ConnectionType);
        this.leg2ConnectionType = Objects.requireNonNull(leg2ConnectionType);
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
        this.freeFluxes = freeFluxes;
        this.kT = kT;
    }

    public LegConnectionType getLeg1ConnectionType() {
        return leg1ConnectionType;
    }

    public LegConnectionType getLeg2ConnectionType() {
        return leg2ConnectionType;
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
