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

    public class AsynchronousMachineLoadData {

        private final double ratedMechanicalP;
        private final double ratedPowerFactor; // cosPhi
        private final double ratedS;
        private final double ratedU;
        private final double efficiency;
        private final double iaIrRatio;
        private final int polePairNumber;
        private final double rxLockedRotorRatio;

        public AsynchronousMachineLoadData(double ratedMechanicalP, double ratedPowerFactor, double ratedS, double ratedU,
                                           double efficiency, double iaIrRatio, int polePairNumber, double rxLockedRotorRatio) {
            this.ratedMechanicalP = ratedMechanicalP;
            this.ratedPowerFactor = ratedPowerFactor;
            this.ratedS = ratedS;
            this.ratedU = ratedU;
            this.efficiency = efficiency;
            this.iaIrRatio = iaIrRatio;
            this.polePairNumber = polePairNumber;
            this.rxLockedRotorRatio = rxLockedRotorRatio;
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

    public static final String NAME = "loadShortCircuit";

    public enum LoadShortCircuitType {
        UNKNOWN,
        CONSTANT_LOAD,
        ASYNCHRONOUS_MACHINE;
    }

    private double xdEquivalent; // equivalent direct admittance of the load used in the admittance matrix, computed based on characteristics of the load
    private double rdEquivalent; // equivalent direct resistance of the load used in the admittance matrix, computed based on characteristics of the load
    private LoadShortCircuitType loadShortCircuitType;
    private AsynchronousMachineLoadData asynchronousMachineLoadData;

    @Override
    public String getName() {
        return NAME;
    }

    public LoadShortCircuit(Load load) {
        super(load);

        this.asynchronousMachineLoadData = null;
        this.loadShortCircuitType = LoadShortCircuitType.UNKNOWN;
    }

    public LoadShortCircuitType getLoadShortCircuitType() {
        return loadShortCircuitType;
    }

    public AsynchronousMachineLoadData getAsynchronousMachineLoadData() {
        return asynchronousMachineLoadData;
    }

    public double getRdEquivalent() {
        return rdEquivalent;
    }

    public double getXdEquivalent() {
        return xdEquivalent;
    }

    public void setRdEquivalent(double rdEquivalent) {
        this.rdEquivalent = rdEquivalent;
    }

    public void setXdEquivalent(double xdEquivalent) {
        this.xdEquivalent = xdEquivalent;
    }

    public void setAsynchronousMachineLoadData(double ratedMechanicalP, double ratedPowerFactor, double ratedS, double ratedU,
                                               double efficiency, double iaIrRatio, int polePairNumber, double rxLockedRotorRatio) {
        this.asynchronousMachineLoadData = new AsynchronousMachineLoadData(ratedMechanicalP, ratedPowerFactor, ratedS, ratedU,
        efficiency, iaIrRatio, polePairNumber, rxLockedRotorRatio);
        this.loadShortCircuitType = LoadShortCircuitType.ASYNCHRONOUS_MACHINE;
    }

    public void setLoadShortCircuitType(LoadShortCircuitType loadShortCircuitType) {
        this.loadShortCircuitType = loadShortCircuitType;
    }
}
