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

    private double coeffRo; // Ro = Rd * CoeffRo
    private double coeffXo;

    @Override
    public String getName() {
        return NAME;
    }

    public LineShortCircuit(Line line, double coeffRo, double coeffXo) {
        super(line);
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
    }

    public double getCoeffRo() {
        return coeffRo;
    }

    public double getCoeffXo() {
        return coeffXo;
    }

    public void setCoeffRo(double coeffRo) {
        this.coeffRo = coeffRo;
    }

    public void setCoeffXo(double coeffXo) {
        this.coeffXo = coeffXo;
    }
}
