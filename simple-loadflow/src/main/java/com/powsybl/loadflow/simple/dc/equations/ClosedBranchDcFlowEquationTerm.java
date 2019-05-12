/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc.equations;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ClosedBranchDcFlowEquationTerm implements EquationTerm {

    private final ClosedBranchDcContext branchContext;
    private final Branch.Side side;

    private final List<Variable> variables = new ArrayList<>(2);

    private final Equation equation;

    public ClosedBranchDcFlowEquationTerm(ClosedBranchDcContext branchContext, Branch.Side side, Bus bus1, Bus bus2,
                                          NetworkContext networkContext, EquationContext equationContext) {
        this.branchContext = Objects.requireNonNull(branchContext);
        this.side = Objects.requireNonNull(side);

        switch (side) {
            case ONE:
                equation = equationContext.getEquation(bus1.getId(), EquationType.BUS_P);
                break;
            case TWO:
                equation = equationContext.getEquation(bus2.getId(), EquationType.BUS_P);
                break;
            default:
                throw new IllegalStateException("Unknown side: " + side);
        }

        variables.add(branchContext.getPh1Var());
        variables.add(branchContext.getPh2Var());
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
    public double eval(double[] x) {
        Objects.requireNonNull(x);
        switch (side) {
            case ONE:
                return branchContext.p1(x);
            case TWO:
                return branchContext.p2(x);
            default:
                throw new IllegalStateException("Unknown side: " + side);
        }
    }

    @Override
    public double der(Variable variable, double[] x) {
        Objects.requireNonNull(variable);
        Objects.requireNonNull(x);
        switch (side) {
            case ONE:
                if (variable.equals(branchContext.getPh1Var())) {
                    return branchContext.getPower();
                } else if (variable.equals(branchContext.getPh2Var())) {
                    return -branchContext.getPower();
                } else {
                    throw new IllegalStateException("Unknown variable: " + variable);
                }
            case TWO:
                if (variable.equals(branchContext.getPh1Var())) {
                    return -branchContext.getPower();
                } else if (variable.equals(branchContext.getPh2Var())) {
                    return branchContext.getPower();
                } else {
                    throw new IllegalStateException("Unknown variable: " + variable);
                }
            default:
                throw new IllegalStateException("Unknown side: " + side);
        }
    }

    @Override
    public double rhs(Variable variable) {
        Objects.requireNonNull(variable);
        if (side == Branch.Side.ONE && variable.equals(branchContext.getPh1Var())) {
            return -branchContext.getPower() * (branchContext.getBc().a2() - branchContext.getBc().a1());
        } else if (side == Branch.Side.TWO && variable.equals(branchContext.getPh2Var())) {
            return branchContext.getPower() * (branchContext.getBc().a2() - branchContext.getBc().a1());
        }
        return 0;
    }
}
