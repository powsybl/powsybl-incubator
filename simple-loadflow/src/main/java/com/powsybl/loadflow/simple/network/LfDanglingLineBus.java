/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.ShuntCompensator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfDanglingLineBus implements LfBus {

    private final DanglingLine danglingLine;

    private final int num;

    private double v = Double.NaN;

    private double angle = Double.NaN;

    private boolean slack = false;

    public LfDanglingLineBus(DanglingLine danglingLine, int num) {
        this.danglingLine = Objects.requireNonNull(danglingLine);
        this.num = num;
    }

    @Override
    public String getId() {
        return danglingLine.getId() + "_BUS";
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public boolean isSlack() {
        return slack;
    }

    @Override
    public void setSlack(boolean slack) {
        this.slack = slack;
    }

    @Override
    public boolean hasVoltageControl() {
        return false;
    }

    @Override
    public double getTargetP() {
        return -danglingLine.getP0();
    }

    @Override
    public double getTargetQ() {
        return -danglingLine.getQ0();
    }

    @Override
    public double getTargetV() {
        return Double.NaN;
    }

    @Override
    public double getV() {
        return v;
    }

    @Override
    public void setV(double v) {
        this.v = v;
    }

    @Override
    public double getAngle() {
        return angle;
    }

    @Override
    public void setAngle(double angle) {
        this.angle = angle;
    }

    @Override
    public double getNominalV() {
        return danglingLine.getTerminal().getVoltageLevel().getNominalV();
    }

    @Override
    public List<ShuntCompensator> getShuntCompensators() {
        return Collections.emptyList();
    }

    @Override
    public int getNeighbors() {
        return 1;
    }
}
