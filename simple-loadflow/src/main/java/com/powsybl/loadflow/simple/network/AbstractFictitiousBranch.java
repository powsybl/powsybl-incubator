/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractFictitiousBranch extends AbstractLfBranch {

    protected double p = Double.NaN;

    protected double q = Double.NaN;

    protected AbstractFictitiousBranch(LfBus bus1, LfBus bus2, PiModel piModel, double nominalV1, double nominalV2) {
        super(bus1, bus2, piModel, nominalV1, nominalV2);
    }

    @Override
    public void setP1(double p1) {
        this.p = p1 * PerUnit.SB;
    }

    @Override
    public void setP2(double p2) {
        // nothing to do
    }

    @Override
    public void setQ1(double q1) {
        this.q = q1 * PerUnit.SB;
    }

    @Override
    public void setQ2(double q2) {
        // nothing to do
    }
}
