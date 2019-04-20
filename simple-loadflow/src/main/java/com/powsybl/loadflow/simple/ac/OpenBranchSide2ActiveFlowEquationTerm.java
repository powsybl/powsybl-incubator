/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class OpenBranchSide2ActiveFlowEquationTerm implements EquationTerm {

    private final OpenBranchSide2AcContext branchContext;

    private final Equation equation;

    private final List<Variable> variables;

    public OpenBranchSide2ActiveFlowEquationTerm(OpenBranchSide2AcContext branchContext, Bus bus1, EquationContext equationContext) {
        this.branchContext = Objects.requireNonNull(branchContext);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(equationContext);
        equation = equationContext.getEquation(bus1.getId(), EquationType.BUS_P);
        variables = Collections.singletonList(equationContext.getVariable(bus1.getId(), VariableType.BUS_V));
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
        return branchContext.p1(x);
    }

    @Override
    public double evalDer(Variable variable, double[] x) {
        if (variable.equals(branchContext.getV1Var())) {
            BranchCharacteristics bc = branchContext.getBc();
            double v1 = x[branchContext.getV1Var().getColumn()];
            return 2 * bc.r1() * bc.r1() * v1 * (bc.g1() + bc.y() * bc.y() * bc.g2() / branchContext.getShunt()
                    + (bc.b2() * bc.b2() + bc.g2() * bc.g2()) * bc.y() * Math.sin(bc.ksi()) / branchContext.getShunt());
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }

    @Override
    public double evalRhs(Variable variable) {
        return 0;
    }
}
