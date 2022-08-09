/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.Generator;

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class GeneratorShortCircuitAdder2 extends AbstractExtensionAdder<Generator, GeneratorShortCircuit2> {

    private double transRd = DEFAULT_TRANS_RD;
    private double subTransRd = DEFAULT_SUB_TRANS_RD;
    private boolean toGround = DEFAULT_TO_GROUND;
    private double coeffRo = DEFAULT_COEFF_RO;
    private double coeffXo = DEFAULT_COEFF_XO;

    public GeneratorShortCircuitAdder2(Generator generator) {
        super(generator);
    }

    @Override
    public Class<? super GeneratorShortCircuit2> getExtensionClass() {
        return GeneratorShortCircuit2.class;
    }

    @Override
    protected GeneratorShortCircuit2 createExtension(Generator generator) {
        return new GeneratorShortCircuit2(generator, transRd, subTransRd, toGround, coeffRo, coeffXo);
    }

    public GeneratorShortCircuitAdder2 withTransRd(double transRd) {
        this.transRd = transRd;
        return this;
    }

    public GeneratorShortCircuitAdder2 withSubTransRd(double subTransRd) {
        this.subTransRd = subTransRd;
        return this;
    }

    public GeneratorShortCircuitAdder2 withToGround(boolean toGround) {
        this.toGround = toGround;
        return this;
    }

    public GeneratorShortCircuitAdder2 withCoeffRo(double coeffRo) {
        this.coeffRo = coeffRo;
        return this;
    }

    public GeneratorShortCircuitAdder2 withCoeffXo(double coeffXo) {
        this.coeffXo = coeffXo;
        return this;
    }
}
