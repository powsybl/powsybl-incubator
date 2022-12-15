/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Line;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class LineShortCircuit extends AbstractExtension<Line> {

    public static final String NAME = "lineShortCircuit";

    private double ro;
    private double xo;

    @Override
    public String getName() {
        return NAME;
    }

    public LineShortCircuit(Line line, double ro, double xo) {
        super(line);
        this.ro = ro;
        this.xo = xo;
    }

    public double getRo() {
        return ro;
    }

    public double getXo() {
        return xo;
    }

    public void setRo(double ro) {
        this.ro = ro;
    }

    public void setXo(double xo) {
        this.xo = xo;
    }
}
