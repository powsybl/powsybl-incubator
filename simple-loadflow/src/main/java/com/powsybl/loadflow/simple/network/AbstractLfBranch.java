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
public abstract class AbstractLfBranch implements LfBranch {

    private final LfBus bus1;

    private final LfBus bus2;

    protected final double r;
    protected final double x;
    protected final double g1;
    protected final double b1;
    protected final double g2;
    protected final double b2;
    protected final double r1;
    protected final double r2;
    protected final double a1;
    protected final double a2;

    protected final double y;
    protected final double ksi;

    protected AbstractLfBranch(LfBus bus1, LfBus bus2, double r, double x, double g1, double g2, double b1, double b2,
                               double r1, double r2, double a1, double a2) {
        this.bus1 = bus1;
        this.bus2 = bus2;
        this.r = r;
        this.x = x;
        this.g1 = g1;
        this.g2 = g2;
        this.b1 = b1;
        this.b2 = b2;
        this.r1 = r1;
        this.r2 = r2;
        this.a1 = a1;
        this.a2 = a2;

        double z = Math.hypot(r, x);
        y = 1 / z;
        ksi = Math.atan2(r, x);
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
    public double x() {
        return x;
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
        return r1;
    }

    @Override
    public double r2() {
        return r2;
    }

    @Override
    public double a1() {
        return a1;
    }

    @Override
    public double a2() {
        return a2;
    }
}
