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
public class ScTransfo3W {

    public static class Leg {

        private LegConnectionType legConnectionType;

        private final double coeffRo; // only used for now for 3 windings transformers
        private final double coeffXo;

        private final boolean freeFluxes; // only used for now for 3 windings transformers

        private final double rGround = 0;
        private final double xGround = 0;

        public Leg(LegConnectionType legConnectionType) {
            this(legConnectionType, 0, 0, false);
        }

        public Leg(LegConnectionType legConnectionType, double coeffRo, double coeffXo, boolean freeFluxes) {
            this.legConnectionType = legConnectionType;
            this.coeffRo = coeffRo;
            this.coeffXo = coeffXo;
            this.freeFluxes = freeFluxes;
        }

        public LegConnectionType getLegConnectionType() {
            return legConnectionType;
        }

        public void setLegConnectionType(LegConnectionType legConnectionType) {
            this.legConnectionType = Objects.requireNonNull(legConnectionType);
        }

        public double getCoeffRo() {
            return coeffRo;
        }

        public double getCoeffXo() {
            return coeffXo;
        }

        public boolean isFreeFluxes() {
            return freeFluxes;
        }

        public double getrGround() {
            return rGround;
        }

        public double getxGround() {
            return xGround;
        }
    }

    private final Leg leg1;
    private final Leg leg2;
    private final Leg leg3;

    private final double kT1; //correction factor of the Two Windings Transformer
    private final double kT2;
    private final double kT3;

    ScTransfo3W(Leg leg1, Leg leg2, Leg leg3) {
        this(leg1, leg2, leg3, 1d, 1d, 1d);
    }

    ScTransfo3W(Leg leg1, Leg leg2, Leg leg3, double kT1, double kT2, double kT3) {
        this.leg1 = Objects.requireNonNull(leg1);
        this.leg2 = Objects.requireNonNull(leg2);
        this.leg3 = Objects.requireNonNull(leg3);
        this.kT1 = kT1;
        this.kT2 = kT2;
        this.kT3 = kT3;
    }

    public Leg getLeg1() {
        return leg1;
    }

    public Leg getLeg2() {
        return leg2;
    }

    public Leg getLeg3() {
        return leg3;
    }

    public double getkT1() {
        return kT1;
    }

    public double getkT2() {
        return kT2;
    }

    public double getkT3() {
        return kT3;
    }
}
