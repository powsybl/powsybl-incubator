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
public class LfLeg2or3Branch extends AbstractLfBranch {

    private final ThreeWindingsTransformer.Leg2or3 leg2or3;

    private double p = Double.NaN;

    private double q = Double.NaN;

    protected LfLeg2or3Branch(LfBus bus2or3, LfBus bus0, ThreeWindingsTransformer t3wt, ThreeWindingsTransformer.Leg2or3 leg2or3) {
        super(bus2or3, bus0, new PiModel(leg2or3.getR(), leg2or3.getX())
                                .setR1(Transformers.getRatio2or3(t3wt, leg2or3)),
                leg2or3.getTerminal().getVoltageLevel().getNominalV(),
                t3wt.getLeg1().getTerminal().getVoltageLevel().getNominalV());
        this.leg2or3 = leg2or3;
    }

    public static LfLeg2or3Branch create(LfBus bus2or3, LfBus bus0, ThreeWindingsTransformer t3wt, ThreeWindingsTransformer.Leg2or3 leg2or3) {
        Objects.requireNonNull(bus2or3);
        Objects.requireNonNull(bus0);
        Objects.requireNonNull(t3wt);
        Objects.requireNonNull(leg2or3);
        return new LfLeg2or3Branch(bus2or3, bus0, t3wt, leg2or3);
    }

    @Override
    public void setP1(double p1) {
        this.p = p1 * PerUnit.SB;
    }

    @Override
    public void setP2(double p2) {
        // nothing to update on star side
    }

    @Override
    public void setQ1(double q1) {
        this.q = q1 * PerUnit.SB;
    }

    @Override
    public void setQ2(double q2) {
        // nothing to update on star side
    }

    @Override
    public void updateState() {
        leg2or3.getTerminal().setP(p);
        leg2or3.getTerminal().setQ(q);
    }
}
