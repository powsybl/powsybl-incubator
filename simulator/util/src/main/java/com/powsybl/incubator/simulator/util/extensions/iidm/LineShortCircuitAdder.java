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

    private double ro = 0.;
    private double xo = 0.;

    public LineShortCircuitAdder(Line line) {
        super(line);
    }

    @Override
    public Class<? super LineShortCircuit> getExtensionClass() {
        return LineShortCircuit.class;
    }

    @Override
    protected LineShortCircuit createExtension(Line line) {
        return new LineShortCircuit(line, ro, xo);
    }

    public LineShortCircuitAdder withRo(double ro) {
        this.ro = ro;
        return this;
    }

    public LineShortCircuitAdder withXo(double xo) {
        this.xo = xo;
        return this;
    }
}
