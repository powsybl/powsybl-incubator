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

    protected double r;
    protected double x;
    protected double y;
    protected double ksi;
    protected double g1 = 0;
    protected double b1 = 0;
    protected double g2 = 0;
    protected double b2 = 0;
    protected double r1 = 1;
    protected double r2 = 1;
    protected double a1 = 0;
    protected double a2 = 0;

    protected AbstractLfBranch(LfBus bus1, LfBus bus2) {
        this.bus1 = bus1;
        this.bus2 = bus2;
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
