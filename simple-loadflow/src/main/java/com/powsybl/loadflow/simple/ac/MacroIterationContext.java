/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MacroIterationContext {

    private final NetworkContext networkContext;

    private final NewtonRaphsonResult newtonRaphsonResult;

    private final int iteration;

    public MacroIterationContext(NetworkContext networkContext, NewtonRaphsonResult newtonRaphsonResult, int iteration) {
        this.networkContext = Objects.requireNonNull(networkContext);
        this.newtonRaphsonResult = Objects.requireNonNull(newtonRaphsonResult);
        this.iteration = iteration;
    }

    public NetworkContext getNetworkContext() {
        return networkContext;
    }

    public NewtonRaphsonResult getNewtonRaphsonResult() {
        return newtonRaphsonResult;
    }

    public int getIteration() {
        return iteration;
    }
}
