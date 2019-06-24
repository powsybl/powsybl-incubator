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

    private double targetV = Double.NaN;

    private int neighbors = 0;

    private final List<LfShunt> shunts = new ArrayList<>();

    public LfBusImpl(Bus bus, int num) {
        super(num);
        this.bus = Objects.requireNonNull(bus);
        nominalV = bus.getVoltageLevel().getNominalV();
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

    void addLoad(Load load) {
        this.loadTargetP += load.getP0();
        this.loadTargetQ += load.getQ0();
    }

    void addBattery(Battery battery) {
        this.loadTargetP += battery.getP0();
        this.loadTargetQ += battery.getQ0();
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

    void addVscConverterStattion(VscConverterStation vscCs, HvdcLine line) {
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
        return bus.getV() / nominalV;
    }

    @Override
    public void setV(double v) {
        bus.setV(v * nominalV);
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
        return nominalV;
    }

    @Override
    public List<LfShunt> getShunts() {
        return shunts;
    }
}
