/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public interface ShortCircuitNorm {

    double getCmaxVoltageFactor(double nominalVoltage);

    double getCminVoltageFactor(double nominalVoltage);

    double getKtT2W(TwoWindingsTransformer t2w);

    double getKtT3W(ThreeWindingsTransformer t3w, int numLeg);

    double getKg(Generator gen);

    String getNormType();
}
