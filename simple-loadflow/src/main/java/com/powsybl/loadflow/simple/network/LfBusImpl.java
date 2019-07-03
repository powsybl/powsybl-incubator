/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfBusImpl extends AbstractLfBus {

    private final Bus bus;

    private final double nominalV;

    private boolean voltageControl = false;

    private double loadTargetP = 0;

    private double loadTargetQ = 0;

    private double generationTargetP = 0;

    private double generationTargetQ = 0;

    private double minP = Double.NaN;

    private double maxP = Double.NaN;

    private double targetV = Double.NaN;

    private int neighbors = 0;

    private final List<LfShunt> shunts = new ArrayList<>();

    public LfBusImpl(Bus bus, int num, double v, double angle) {
        super(num, v, angle);
        this.bus = bus;
        nominalV = bus.getVoltageLevel().getNominalV();
    }

    public static LfBusImpl create(Bus bus, int num) {
        Objects.requireNonNull(bus);
        return new LfBusImpl(bus, num, bus.getV(), bus.getAngle());
    }

    @Override
    public String getId() {
        return bus.getId();
    }

    @Override
    public boolean hasVoltageControl() {
        return voltageControl;
    }

    private void checkTargetV(double targetV) {
        if (!Double.isNaN(this.targetV) && this.targetV != targetV) {
            throw new PowsyblException("Multiple generators connected to same bus with different target voltage");
        }
    }

    private void setActivePowerLimits(double minP, double maxP) {
        if (Double.isNaN(this.minP)) {
            this.minP = minP;
        } else {
            this.minP = Math.max(this.minP, minP);
        }
        if (Double.isNaN(this.maxP)) {
            this.maxP = maxP;
        } else {
            this.maxP = Math.min(this.maxP, maxP);
        }
    }

    void addLoad(Load load) {
        this.loadTargetP += load.getP0();
        this.loadTargetQ += load.getQ0();
    }

    void addBattery(Battery battery) {
        this.loadTargetP += battery.getP0();
        this.loadTargetQ += battery.getQ0();
        setActivePowerLimits(battery.getMinP(), battery.getMaxP());
    }

    void addGenerator(Generator generator) {
        this.generationTargetP += generator.getTargetP();
        if (generator.isVoltageRegulatorOn()) {
            checkTargetV(generator.getTargetV());
            targetV = generator.getTargetV();
            voltageControl = true;
        } else {
            this.generationTargetQ += generator.getTargetQ();
        }
        setActivePowerLimits(generator.getMinP(), generator.getMaxP());
    }

    void addStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
        if (staticVarCompensator.getRegulationMode() == StaticVarCompensator.RegulationMode.VOLTAGE) {
            checkTargetV(staticVarCompensator.getVoltageSetPoint());
            targetV = staticVarCompensator.getVoltageSetPoint();
            voltageControl = true;
        } else if (staticVarCompensator.getRegulationMode() == StaticVarCompensator.RegulationMode.REACTIVE_POWER) {
            throw new UnsupportedOperationException("SVC with reactive power regulation not supported");
        }
    }

    void addVscConverterStation(VscConverterStation vscCs, HvdcLine line) {
        double targetP = line.getConverterStation1() == vscCs && line.getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER
                ? line.getActivePowerSetpoint()
                : -line.getActivePowerSetpoint();
        generationTargetP += targetP;
        if (vscCs.isVoltageRegulatorOn()) {
            checkTargetV(vscCs.getVoltageSetpoint());
            targetV = vscCs.getVoltageSetpoint();
            voltageControl = true;
        } else {
            generationTargetQ += vscCs.getReactivePowerSetpoint();
        }
        setActivePowerLimits(-line.getMaxP(), line.getMaxP());
    }

    void addShuntCompensator(ShuntCompensator sc) {
        shunts.add(new LfShuntImpl(sc));
    }

    @Override
    public double getGenerationTargetP() {
        return generationTargetP / PerUnit.SB;
    }

    @Override
    public double getGenerationTargetQ() {
        return generationTargetQ / PerUnit.SB;
    }

    @Override
    public double getLoadTargetP() {
        return loadTargetP / PerUnit.SB;
    }

    @Override
    public double getLoadTargetQ() {
        return loadTargetQ / PerUnit.SB;
    }

    @Override
    public double getMinP() {
        return minP / PerUnit.SB;
    }

    @Override
    public double getMaxP() {
        return maxP / PerUnit.SB;
    }

    @Override
    public double getTargetV() {
        return targetV / nominalV;
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
        return v / nominalV;
    }

    @Override
    public void setV(double v) {
        this.v = v * nominalV;
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
        return nominalV;
    }

    @Override
    public List<LfShunt> getShunts() {
        return shunts;
    }

    @Override
    public void updateState() {
        bus.setV(v).setAngle(angle);
    }
}
