/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.Line;

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class LineShortCircuitAdder extends AbstractExtensionAdder<Line, LineShortCircuit> {

    private double coeffRo = DEFAULT_COEFF_RO;
    private double coeffXo = DEFAULT_COEFF_XO;

    public LineShortCircuitAdder(Line line) {
        super(line);
    }

    @Override
    public Class<? super LineShortCircuit> getExtensionClass() {
        return LineShortCircuit.class;
    }

    @Override
    protected LineShortCircuit createExtension(Line line) {
        return new LineShortCircuit(line, coeffRo, coeffXo);
    }

    public LineShortCircuitAdder withCoeffRo(double coeffRo) {
        this.coeffRo = coeffRo;
        return this;
    }

    public LineShortCircuitAdder withCoeffXo(double coeffXo) {
        this.coeffXo = coeffXo;
        return this;
    }
}
