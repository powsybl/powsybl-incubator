/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.ac.nr.NewtonRaphsonResult;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class MacroIterationContext {

    private final int macroIteration;

    private final NetworkContext networkContext;

    private final NewtonRaphsonResult newtonRaphsonResult;

    MacroIterationContext(int macroIteration, NetworkContext networkContext, NewtonRaphsonResult newtonRaphsonResult) {
        this.macroIteration = macroIteration;
        this.networkContext = Objects.requireNonNull(networkContext);
        this.newtonRaphsonResult = Objects.requireNonNull(newtonRaphsonResult);
    }

    int getMacroIteration() {
        return macroIteration;
    }

    NetworkContext getNetworkContext() {
        return networkContext;
    }

    NewtonRaphsonResult getNewtonRaphsonResult() {
        return newtonRaphsonResult;
    }
}
