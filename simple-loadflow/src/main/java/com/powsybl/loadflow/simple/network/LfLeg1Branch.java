/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.iidm.network.ThreeWindingsTransformer;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfLeg1Branch extends AbstractLfBranch {

    private final ThreeWindingsTransformer.Leg1 leg1;

    public LfLeg1Branch(LfBus bus1, LfBus bus0, ThreeWindingsTransformer.Leg1 leg1) {
        super(bus1, bus0);
        this.leg1 = Objects.requireNonNull(leg1);
        r = leg1.getR();
        x = leg1.getX();
        double z = Math.hypot(r, x);
        y = 1 / z;
        ksi = Math.atan2(r, x);
        g2 = leg1.getG();
        b2 = leg1.getB();
    }

    @Override
    public void setP1(double p1) {
        leg1.getTerminal().setP(p1);
    }

    @Override
    public void setP2(double p2) {
        // nothing to update on star side
    }

    @Override
    public void setQ1(double q1) {
        leg1.getTerminal().setQ(q1);
    }

    @Override
    public void setQ2(double q2) {
        // nothing to update on star side
    }
}
