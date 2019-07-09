/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.iidm.network.DanglingLine;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfDanglingLineBranch extends AbstractLfBranch {

    private final DanglingLine danglingLine;

    private double p = Double.NaN;

    private double q = Double.NaN;

    protected LfDanglingLineBranch(DanglingLine danglingLine, LfBus bus1, LfBus bus2) {
        super(bus1, bus2, new PiModel(danglingLine.getR(), danglingLine.getX())
                            .setG1(danglingLine.getG() / 2)
                            .setG2(danglingLine.getG() / 2)
                            .setB1(danglingLine.getB() / 2)
                            .setB2(danglingLine.getB() / 2),
                danglingLine.getTerminal().getVoltageLevel().getNominalV(),
                danglingLine.getTerminal().getVoltageLevel().getNominalV());
        this.danglingLine = danglingLine;
    }

    public static LfDanglingLineBranch create(DanglingLine danglingLine, LfBus bus1, LfBus bus2) {
        Objects.requireNonNull(danglingLine);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);
        return new LfDanglingLineBranch(danglingLine, bus1, bus2);
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

    @Override
    public void updateState() {
        danglingLine.getTerminal().setP(p);
        danglingLine.getTerminal().setQ(q);
    }
}
