/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.commons.PowsyblException;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.NetworkContext;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DistributedSlackAction implements MacroAction {

    private double[] normalizedParticipationFactor;

    @Override
    public String getName() {
        return "Distributed slack";
    }

    @Override
    public void init(MacroIterationContext macroIterationContext) {
        NetworkContext networkContext = macroIterationContext.getNetworkContext();
        normalizedParticipationFactor = new double[networkContext.getBuses().size()];
        double sum = 0d;
        for (LfBus bus : networkContext.getBuses()) {
            normalizedParticipationFactor[bus.getNum()] = bus.getParticipationFactor();
            sum += bus.getParticipationFactor();
        }
        if (sum == 0) {
            throw new PowsyblException("No generator participating to slack distribution");
        }
        for (int i = 0; i < normalizedParticipationFactor.length; i++) {
            normalizedParticipationFactor[i] /= sum;
        }
    }

    @Override
    public boolean run(MacroIterationContext context) {
        return false;
    }
}
