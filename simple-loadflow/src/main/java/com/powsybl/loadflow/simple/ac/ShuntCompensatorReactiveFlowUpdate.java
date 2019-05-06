/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.equations.VariableUpdate;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ShuntCompensatorReactiveFlowUpdate implements VariableUpdate {

    private final ShuntCompensatorContext shuntContext;

    public ShuntCompensatorReactiveFlowUpdate(ShuntCompensatorContext shuntContext) {
        this.shuntContext = Objects.requireNonNull(shuntContext);
    }

    @Override
    public void update(double[] x) {
        shuntContext.getSc().getTerminal().setQ(shuntContext.q(x));
    }
}
