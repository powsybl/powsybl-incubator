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
        return new LoadShortCircuit(load);
    }

}
