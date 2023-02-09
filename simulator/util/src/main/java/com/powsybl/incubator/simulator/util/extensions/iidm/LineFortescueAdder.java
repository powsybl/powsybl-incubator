/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.Line;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class LineFortescueAdder extends AbstractExtensionAdder<Line, LineFortescue> {

    private double ro = 0.;
    private double xo = 0.;

    public LineFortescueAdder(Line line) {
        super(line);
    }

    @Override
    public Class<? super LineFortescue> getExtensionClass() {
        return LineFortescue.class;
    }

    @Override
    protected LineFortescue createExtension(Line line) {
        return new LineFortescue(line, ro, xo);
    }

    public LineFortescueAdder withRo(double ro) {
        this.ro = ro;
        return this;
    }

    public LineFortescueAdder withXo(double xo) {
        this.xo = xo;
        return this;
    }
}
