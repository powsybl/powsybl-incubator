/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.google.common.collect.ImmutableList;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractClosedBranchAcFlowEquationTerm implements EquationTerm {

    protected final ClosedBranchAcContext branchContext;

    protected final Branch.Side side;

    protected final Equation equation;

    protected final List<Variable> variables;

    protected AbstractClosedBranchAcFlowEquationTerm(ClosedBranchAcContext branchContext, Branch.Side side, Bus bus1, Bus bus2,
                                                     EquationType equationType, EquationContext equationContext) {
        this.branchContext = Objects.requireNonNull(branchContext);
        this.side = Objects.requireNonNull(side);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);

        switch (side) {
            case ONE:
                equation = equationContext.getEquation(bus1.getId(), equationType);
                break;
            case TWO:
                equation = equationContext.getEquation(bus2.getId(), equationType);
                break;
            default:
                throw new IllegalStateException("Unknown side: " + side);
        }

        variables = ImmutableList.of(branchContext.getV1Var(), branchContext.getV2Var(), branchContext.getPh1Var(), branchContext.getPh2Var());
    }

    @Override
    public Equation getEquation() {
        return equation;
    }

    @Override
    public List<Variable> getVariables() {
        return variables;
    }

    @Override
    public double rhs(Variable variable) {
        return 0;
    }
}
