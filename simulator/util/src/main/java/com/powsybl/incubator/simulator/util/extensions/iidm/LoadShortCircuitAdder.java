/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.Load;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class LoadShortCircuitAdder extends AbstractExtensionAdder<Load, LoadShortCircuit> {

    private double ratedMechanicalP = 0.;
    private double ratedPowerFactor = 0.;
    private double ratedS = 0.;
    private double ratedU = 0.;
    private double efficiency = 0.;
    private double iaIrRatio = 0.;
    private int polePairNumber = 1;
    private double rxLockedRotorRatio = 0.;
    private LoadShortCircuit.LoadShortCircuitType loadShortCircuitType = LoadShortCircuit.LoadShortCircuitType.CONSTANT_LOAD;

    public LoadShortCircuitAdder(Load load) {
        super(load);
    }

    @Override
    public Class<? super LoadShortCircuit> getExtensionClass() {
        return LoadShortCircuit.class;
    }

    @Override
    protected LoadShortCircuit createExtension(Load load) {
        return new LoadShortCircuit(load, ratedMechanicalP, ratedPowerFactor, ratedS, ratedU,
        efficiency, iaIrRatio, polePairNumber, rxLockedRotorRatio, loadShortCircuitType);
    }

    public LoadShortCircuitAdder withRatedMechanicalP(double ratedMechanicalP) {
        this.ratedMechanicalP = ratedMechanicalP;
        return this;
    }

    public LoadShortCircuitAdder withRatedS(double ratedS) {
        this.ratedS = ratedS;
        return this;
    }

    public LoadShortCircuitAdder withRatedPowerFactor(double ratedPowerFactor) {
        this.ratedPowerFactor = ratedPowerFactor;
        return this;
    }

    public LoadShortCircuitAdder withRatedU(double ratedU) {
        this.ratedU = ratedU;
        return this;
    }

    public LoadShortCircuitAdder withEfficiency(double efficiency) {
        this.efficiency = efficiency;
        return this;
    }

    public LoadShortCircuitAdder withIaIrRatio(double iaIrRatio) {
        this.iaIrRatio = iaIrRatio;
        return this;
    }

    public LoadShortCircuitAdder withRxLockedRotorRatio(double rxLockedRotorRatio) {
        this.rxLockedRotorRatio = rxLockedRotorRatio;
        return this;
    }

    public LoadShortCircuitAdder withPolePairNumber(int polePairNumber) {
        this.polePairNumber = polePairNumber;
        return this;
    }

    public LoadShortCircuitAdder withLoadShortCircuitType(LoadShortCircuit.LoadShortCircuitType loadShortCircuitType) {
        this.loadShortCircuitType = loadShortCircuitType;
        return this;
    }

}
