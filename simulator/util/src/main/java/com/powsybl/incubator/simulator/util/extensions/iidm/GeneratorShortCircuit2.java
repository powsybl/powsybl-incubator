/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Generator;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class GeneratorShortCircuit2 extends AbstractExtension<Generator> {

    public static final String NAME = "generatorShortCircuit2";

    private final double transRd; // transient resistance
    private final double subTransRd; // sub-transient resistance
    private final boolean toGround;
    private final double coeffRo;
    private final double coeffXo;

    @Override
    public String getName() {
        return NAME;
    }

    public GeneratorShortCircuit2(Generator generator, double transRd, double subTransRd, boolean toGround, double coeffRo, double coeffXo) {
        super(generator);
        this.transRd = transRd;
        this.subTransRd = subTransRd;
        this.toGround = toGround;
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
    }

    public double getTransRd() {
        return transRd;
    }

    public double getSubTransRd() {
        return subTransRd;
    }

    public boolean isToGround() {
        return toGround;
    }

    public double getCoeffRo() {
        return coeffRo;
    }

    public double getCoeffXo() {
        return coeffXo;
    }
}
