/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.loadflow.simple.equations.VariableUpdate;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ShuntCompensatorReactiveFlowUpdate implements VariableUpdate {

    private final ShuntCompensator sc;

    private final ShuntCompensatorReactiveFlowEquationTerm equationTerm;

    public ShuntCompensatorReactiveFlowUpdate(ShuntCompensator sc, ShuntCompensatorReactiveFlowEquationTerm equationTerm) {
        this.sc = Objects.requireNonNull(sc);
        this.equationTerm = Objects.requireNonNull(equationTerm);
    }

    @Override
    public void update(double[] x) {
        sc.getTerminal().setQ(equationTerm.eval(x));
    }
}
