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

    public LfDanglingLineBranch(DanglingLine danglingLine, LfBus bus1, LfBus bus2) {
        super(Objects.requireNonNull(bus1), Objects.requireNonNull(bus2));
        this.danglingLine = Objects.requireNonNull(danglingLine);
        r = danglingLine.getR();
        x = danglingLine.getX();
        z = Math.hypot(r, x);
        y = 1 / z;
        ksi = Math.atan2(r, x);
        g1 = danglingLine.getG() / 2;
        b1 = danglingLine.getB() / 2;
        g2 = danglingLine.getG() / 2;
        b2 = danglingLine.getB() / 2;
    }

    @Override
    public String getId() {
        return danglingLine.getId() + "_BRANCH";
    }

    @Override
    public void setP1(double p1) {
        danglingLine.getTerminal().setP(p1);
    }

    @Override
    public void setP2(double p2) {
        // nothing to do
    }

    @Override
    public void setQ1(double q1) {
        danglingLine.getTerminal().setQ(q1);
    }

    @Override
    public void setQ2(double q2) {
        // nothing to do
    }

    @Override
    public double r1() {
        return 1;
    }

    @Override
    public double r2() {
        return 1;
    }

    @Override
    public double a1() {
        return 0;
    }

    @Override
    public double a2() {
        return 0;
    }
}
