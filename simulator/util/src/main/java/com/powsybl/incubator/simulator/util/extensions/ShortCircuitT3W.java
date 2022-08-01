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
public class ShortCircuitT3W {

    private final ShortCircuitTransformerLeg leg1;
    private final ShortCircuitTransformerLeg leg2;
    private final ShortCircuitTransformerLeg leg3;

    private final double kT1; //correction factor of the Two Windings Transformer
    private final double kT2;
    private final double kT3;

    ShortCircuitT3W(ShortCircuitTransformerLeg leg1, ShortCircuitTransformerLeg leg2, ShortCircuitTransformerLeg leg3) {
        this(leg1, leg2, leg3, 1d, 1d, 1d);
    }

    ShortCircuitT3W(ShortCircuitTransformerLeg leg1, ShortCircuitTransformerLeg leg2, ShortCircuitTransformerLeg leg3, double kT1, double kT2, double kT3) {
        this.leg1 = Objects.requireNonNull(leg1);
        this.leg2 = Objects.requireNonNull(leg2);
        this.leg3 = Objects.requireNonNull(leg3);
        this.kT1 = kT1;
        this.kT2 = kT2;
        this.kT3 = kT3;
    }

    public ShortCircuitTransformerLeg getLeg1() {
        return leg1;
    }

    public ShortCircuitTransformerLeg getLeg2() {
        return leg2;
    }

    public ShortCircuitTransformerLeg getLeg3() {
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
