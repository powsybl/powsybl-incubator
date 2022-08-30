/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Load;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class LoadShortCircuit extends AbstractExtension<Load> {

    public static final String NAME = "loadShortCircuit";

    public enum LoadShortCircuitType {
        UNKNOWN,
        CONSTANT_LOAD,
        ASYNCHRONOUS_MACHINE;
    }

    private final double ratedMechanicalP;
    private final double ratedPowerFactor; // cosPhi
    private final double ratedS;
    private final double ratedU;
    private final double efficiency;
    private final double iaIrRatio;
    private final int polePairNumber;
    private final double rxLockedRotorRatio;
    private final LoadShortCircuitType loadShortCircuitType;

    @Override
    public String getName() {
        return NAME;
    }

    public LoadShortCircuit(Load load, double ratedMechanicalP, double ratedPowerFactor, double ratedS, double ratedU,
                            double efficiency, double iaIrRatio, int polePairNumber, double rxLockedRotorRatio, LoadShortCircuitType loadShortCircuitType) {
        super(load);
        this.ratedMechanicalP = ratedMechanicalP;
        this.ratedPowerFactor = ratedPowerFactor;
        this.ratedS = ratedS;
        this.ratedU = ratedU;
        this.efficiency = efficiency;
        this.iaIrRatio = iaIrRatio;
        this.polePairNumber = polePairNumber;
        this.rxLockedRotorRatio = rxLockedRotorRatio;
        this.loadShortCircuitType = loadShortCircuitType;
    }

    public double getEfficiency() {
        return efficiency;
    }

    public double getRatedU() {
        return ratedU;
    }

    public double getIaIrRatio() {
        return iaIrRatio;
    }

    public LoadShortCircuitType getLoadShortCircuitType() {
        return loadShortCircuitType;
    }

    public double getRatedMechanicalP() {
        return ratedMechanicalP;
    }

    public double getRatedPowerFactor() {
        return ratedPowerFactor;
    }

    public double getRatedS() {
        return ratedS;
    }

    public double getRxLockedRotorRatio() {
        return rxLockedRotorRatio;
    }

    public int getPolePairNumber() {
        return polePairNumber;
    }
}
