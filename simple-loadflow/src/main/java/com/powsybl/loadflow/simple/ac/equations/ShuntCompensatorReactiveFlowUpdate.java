/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.loadflow.simple.equations.VariableUpdate;
import com.powsybl.loadflow.simple.network.LfShunt;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ShuntCompensatorReactiveFlowUpdate implements VariableUpdate {

    private final LfShunt shunt;

    private final ShuntCompensatorReactiveFlowEquationTerm q;

    public ShuntCompensatorReactiveFlowUpdate(LfShunt shunt, ShuntCompensatorReactiveFlowEquationTerm q) {
        this.shunt = Objects.requireNonNull(shunt);
        this.q = Objects.requireNonNull(q);
    }

    @Override
    public void update() {
        shunt.setQ(q.eval());
    }
}
