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
public class LfDanglingLineBranch implements LfBranch {

    private final DanglingLine danglingLine;

    private final LfBus bus1;

    private final LfBus bus2;

    private final double r;
    private final double x;
    private final double z;
    private final double y;
    private final double ksi;
    private final double g1;
    private final double b1;
    private final double g2;
    private final double b2;

    public LfDanglingLineBranch(DanglingLine danglingLine, LfBus bus1, LfBus bus2) {
        this.danglingLine = Objects.requireNonNull(danglingLine);
        this.bus1 = Objects.requireNonNull(bus1);
        this.bus2 = Objects.requireNonNull(bus2);
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
    public LfBus getBus1() {
        return bus1;
    }

    @Override
    public LfBus getBus2() {
        return bus2;
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
    public double r() {
        return r;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double z() {
        return z;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double ksi() {
        return ksi;
    }

    @Override
    public double g1() {
        return g1;
    }

    @Override
    public double g2() {
        return g2;
    }

    @Override
    public double b1() {
        return b1;
    }

    @Override
    public double b2() {
        return b2;
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
