/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.google.common.collect.ImmutableList;
import com.powsybl.loadflow.simple.equations.Equation;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.Variable;
import com.powsybl.loadflow.simple.equations.VariableType;
import com.powsybl.loadflow.simple.network.LfBranch;
import com.powsybl.loadflow.simple.network.LfBus;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractClosedBranchAcFlowEquationTerm extends AbstractBranchAcFlowEquationTerm {

    protected final Variable v1Var;

    protected final Variable v2Var;

    protected final Variable ph1Var;

    protected final Variable ph2Var;

    protected final Equation equation;

    protected final List<Variable> variables;

    protected final double a1;

    protected final double a2;

    protected AbstractClosedBranchAcFlowEquationTerm(LfBranch branch, LfBus bus1, LfBus bus2, Equation equation,
                                                     EquationContext equationContext) {
        super(branch);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);
        this.equation = Objects.requireNonNull(equation);
        v1Var = equationContext.getVariable(bus1.getNum(), VariableType.BUS_V);
        v2Var = equationContext.getVariable(bus2.getNum(), VariableType.BUS_V);
        ph1Var = equationContext.getVariable(bus1.getNum(), VariableType.BUS_PHI);
        ph2Var = equationContext.getVariable(bus2.getNum(), VariableType.BUS_PHI);
        variables = ImmutableList.of(v1Var, v2Var, ph1Var, ph2Var);
        a1 = this.branch.a1();
        a2 = this.branch.a2();
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
