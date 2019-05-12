/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc.equations;

import com.google.common.collect.ImmutableList;
import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractClosedBranchDcFlowEquationTerm implements EquationTerm {

    protected final ClosedBranchDcContext branchContext;

    protected final List<Variable> variables;

    protected final Equation equation;

    protected AbstractClosedBranchDcFlowEquationTerm(ClosedBranchDcContext branchContext, Bus bus, EquationContext equationContext) {
        this.branchContext = Objects.requireNonNull(branchContext);
        equation = equationContext.getEquation(bus.getId(), EquationType.BUS_P);
        variables = ImmutableList.of(branchContext.getPh1Var(), branchContext.getPh2Var());
    }

    @Override
    public Equation getEquation() {
        return equation;
    }

    @Override
    public List<Variable> getVariables() {
        return variables;
    }
}
