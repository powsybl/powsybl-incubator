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
public class LfBusImpl extends AbstractLfBus {

    private final Bus bus;

    private final int num;

    private boolean voltageControl = false;

    private double loadTargetP = 0;

    private double loadTargetQ = 0;

    private double generationTargetP = 0;

    private double generationTargetQ = 0;

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
    public boolean hasVoltageControl() {
        return voltageControl;
    }

    public void setVoltageControl(boolean voltageControl) {
        this.voltageControl = voltageControl;
    }

    void addLoadTargetP(double loadTargetP) {
        this.loadTargetP += loadTargetP;
    }

    void addGenerationTargetP(double generationTargetP) {
        this.generationTargetP += generationTargetP;
    }

    void addLoadTargetQ(double loadTargetQ) {
        this.loadTargetQ += loadTargetQ;
    }

    void addGenerationTargetQ(double generationTargetQ) {
        this.generationTargetQ += generationTargetQ;
    }

    @Override
    public double getGenerationTargetP() {
        return generationTargetP;
    }

    @Override
    public double getGenerationTargetQ() {
        return generationTargetQ;
    }

    @Override
    public double getLoadTargetP() {
        return loadTargetP;
    }

    @Override
    public double getLoadTargetQ() {
        return loadTargetQ;
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

    @Override
    public int getNeighbors() {
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
