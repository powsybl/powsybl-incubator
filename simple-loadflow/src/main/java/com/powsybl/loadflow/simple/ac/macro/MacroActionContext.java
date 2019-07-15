/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.macro;

import com.powsybl.loadflow.simple.ac.nr.NewtonRaphsonResult;
import com.powsybl.loadflow.simple.network.LfNetwork;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MacroActionContext {

    private final int macroIteration;

    private final LfNetwork network;

    private final NewtonRaphsonResult newtonRaphsonResult;

    MacroActionContext(int macroIteration, LfNetwork network, NewtonRaphsonResult newtonRaphsonResult) {
        this.macroIteration = macroIteration;
        this.network = Objects.requireNonNull(network);
        this.newtonRaphsonResult = Objects.requireNonNull(newtonRaphsonResult);
    }

    public int getMacroIteration() {
        return macroIteration;
    }

    public LfNetwork getNetwork() {
        return network;
    }

    public NewtonRaphsonResult getNewtonRaphsonResult() {
        return newtonRaphsonResult;
    }
}
