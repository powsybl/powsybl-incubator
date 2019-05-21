/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.ShuntCompensator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfBusImpl implements LfBus {

    private final Bus bus;

    private final int num;

    private boolean slack = false;

    private boolean voltageControl = false;

    private double targetP = 0;

    private double targetQ = 0;

    private double targetV = Double.NaN;

    private int neighbors = 0;

    private final List<ShuntCompensator> shuntCompensators = new ArrayList<>();

    public LfBusImpl(Bus bus, int num) {
        this.bus = Objects.requireNonNull(bus);
        this.num = num;
    }

    @Override
    public String getId() {
        return bus.getId();
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public boolean isSlack() {
        return slack;
    }

    public void setSlack(boolean slack) {
        this.slack = slack;
    }

    @Override
    public boolean hasVoltageControl() {
        return voltageControl;
    }

    public void setVoltageControl(boolean voltageControl) {
        this.voltageControl = voltageControl;
    }

    void addTargetP(double targetP) {
        this.targetP += targetP;
    }

    @Override
    public double getTargetP() {
        return targetP;
    }

    void addTargetQ(double targetQ) {
        this.targetQ += targetQ;
    }

    @Override
    public double getTargetQ() {
        return targetQ;
    }

    @Override
    public double getTargetV() {
        return targetV;
    }

    public void setTargetV(double targetV) {
        if (!Double.isNaN(this.targetV) && this.targetV != targetV) {
            throw new PowsyblException("Multiple generators connected to same bus with different target voltage");
        }
        this.targetV = targetV;
    }

    void addNeighbor() {
        neighbors++;
    }

    int getNeighbors() {
        return neighbors;
    }

    @Override
    public double getV() {
        return bus.getV();
    }

    @Override
    public void setV(double v) {
        bus.setV(v);
    }

    @Override
    public double getAngle() {
        return bus.getAngle();
    }

    @Override
    public void setAngle(double angle) {
        bus.setAngle(angle);
    }

    @Override
    public double getNominalV() {
        return bus.getVoltageLevel().getNominalV();
    }

    @Override
    public List<ShuntCompensator> getShuntCompensators() {
        return shuntCompensators;
    }

    void addShuntCompensator(ShuntCompensator sc) {
        shuntCompensators.add(sc);
    }
}
