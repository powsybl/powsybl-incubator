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
public class ShortCircuitNetworkT3W {

    ShortCircuitNetworkTransformerLeg leg1;
    ShortCircuitNetworkTransformerLeg leg2;
    ShortCircuitNetworkTransformerLeg leg3;

    double kT1; //correction factor of the Two Windings Transformer
    double kT2;
    double kT3;

    ShortCircuitNetworkT3W(ShortCircuitNetworkTransformerLeg leg1, ShortCircuitNetworkTransformerLeg leg2, ShortCircuitNetworkTransformerLeg leg3) {
        this.leg1 = leg1;
        this.leg2 = leg2;
        this.leg3 = leg3;
        double kT1 = 1.0;
        double kT2 = 1.0;
        double kT3 = 1.0;
    }

    ShortCircuitNetworkT3W(ShortCircuitNetworkTransformerLeg leg1, ShortCircuitNetworkTransformerLeg leg2, ShortCircuitNetworkTransformerLeg leg3, double kT1, double kT2, double kT3) {
        this(leg1, leg2, leg3);
        this.kT1 = kT1;
        this.kT2 = kT2;
        this.kT3 = kT3;
    }

}
