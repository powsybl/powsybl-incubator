/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNorm {

    public enum NormType {
        NONE,
        IEC,
        RTE_COURCIRC;
    }

    ShortCircuitNorm() {

    }

    public double getCmaxVoltageFactor(double nominalVoltage) {
        return 1.0;
    }

    public double getCminVoltageFactor(double nominalVoltage) {
        return 1.0;
    }

    public double getKtT2W(TwoWindingsTransformer t2w) {
        return 1.0;
    }

    public double getKtT3W(ThreeWindingsTransformer t3w, int numLeg) {
        return 1.0;
    }

    public NormType getNormType() {
        return NormType.NONE;
    }

}
