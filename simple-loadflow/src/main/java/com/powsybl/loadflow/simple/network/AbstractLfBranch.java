/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractLfBranch implements LfBranch {

    private final LfBus bus1;

    private final LfBus bus2;

    protected final PiModel piModel;

    protected final double y;
    protected final double ksi;

    protected AbstractLfBranch(LfBus bus1, LfBus bus2, PiModel piModel) {
        this.bus1 = bus1;
        this.bus2 = bus2;
        this.piModel = Objects.requireNonNull(piModel);

        double z = Math.hypot(piModel.getR(), piModel.getX());
        y = 1 / z;
        ksi = Math.atan2(piModel.getR(), piModel.getX());
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
        return piModel.getX();
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
        return piModel.getG1();
    }

    @Override
    public double g2() {
        return piModel.getG2();
    }

    @Override
    public double b1() {
        return piModel.getB1();
    }

    @Override
    public double b2() {
        return piModel.getB2();
    }

    @Override
    public double r1() {
        return piModel.getR1();
    }

    @Override
    public double r2() {
        return piModel.getR2();
    }

    @Override
    public double a1() {
        return piModel.getA1();
    }

    @Override
    public double a2() {
        return piModel.getA2();
    }
}
