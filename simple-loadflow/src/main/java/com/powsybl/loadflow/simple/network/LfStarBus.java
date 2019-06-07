/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.ThreeWindingsTransformer;

import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfStarBus extends AbstractLfBus {

    private final ThreeWindingsTransformer t3wt;

    private final int num;

    private double v = Double.NaN;

    private double angle = Double.NaN;

    public LfStarBus(ThreeWindingsTransformer t3wt, int num) {
        this.t3wt = t3wt;
        this.num = num;
    }

    @Override
    public String getId() {
        return t3wt.getId() + "_BUS0";
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public boolean hasVoltageControl() {
        return false;
    }

    @Override
    public double getLoadTargetP() {
        return 0;
    }

    @Override
    public double getLoadTargetQ() {
        return 0;
    }

    @Override
    public double getGenerationTargetP() {
        return 0;
    }

    @Override
    public double getGenerationTargetQ() {
        return 0;
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
        return t3wt.getLeg1().getTerminal().getVoltageLevel().getNominalV();
    }

    @Override
    public List<ShuntCompensator> getShuntCompensators() {
        return Collections.emptyList();
    }

    @Override
    public int getNeighbors() {
        return 3;
    }
}
