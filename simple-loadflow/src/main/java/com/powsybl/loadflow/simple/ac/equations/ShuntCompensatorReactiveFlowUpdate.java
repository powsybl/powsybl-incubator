/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.api.ShuntCompensator;
import com.powsybl.loadflow.simple.equations.VariableUpdate;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ShuntCompensatorReactiveFlowUpdate implements VariableUpdate {

    private final ShuntCompensator sc;

    private final ShuntCompensatorReactiveFlowEquationTerm q;

    public ShuntCompensatorReactiveFlowUpdate(ShuntCompensator sc, ShuntCompensatorReactiveFlowEquationTerm q) {
        this.sc = Objects.requireNonNull(sc);
        this.q = Objects.requireNonNull(q);
    }

    @Override
    public void update(double[] x) {
        sc.getTerminal().setQ(q.eval(x));
    }
}
