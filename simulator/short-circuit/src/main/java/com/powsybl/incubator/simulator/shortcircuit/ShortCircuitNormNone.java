/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNormNone implements ShortCircuitNorm {

    @Override
    public double getCmaxVoltageFactor(double nominalVoltage) {
        return 1.0;
    }

    @Override
    public double getCminVoltageFactor(double nominalVoltage) {
        return 1.0;
    }

    @Override
    public double getKtT2W(TwoWindingsTransformer t2w) {
        return 1.0;
    }

    @Override
    public String getNormType() {
        return "NONE";
    }

    @Override
    public double getKg(Generator gen) {
        return 1.;
    }
}
