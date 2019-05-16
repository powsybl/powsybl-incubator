/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.google.common.collect.ImmutableList;
import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractClosedBranchAcFlowEquationTerm implements EquationTerm {

    protected final BranchCharacteristics bc;

    protected final Variable v1Var;

    protected final Variable v2Var;

    protected final Variable ph1Var;

    protected final Variable ph2Var;

    protected final Equation equation;

    protected final List<Variable> variables;

    protected final double r1;
    protected final double r2;
    protected final double g1;
    protected final double g2;
    protected final double b1;
    protected final double b2;
    protected final double y;
    protected final double ksi;
    protected final double a1;
    protected final double a2;
    protected final double cosKsi;
    protected final double sinKsi;

    protected AbstractClosedBranchAcFlowEquationTerm(BranchCharacteristics bc, Bus bus1, Bus bus2, Equation equation,
                                                     EquationContext equationContext) {
        this.bc = Objects.requireNonNull(bc);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);
        this.equation = Objects.requireNonNull(equation);
        v1Var = equationContext.getVariable(bus1.getId(), VariableType.BUS_V);
        v2Var = equationContext.getVariable(bus2.getId(), VariableType.BUS_V);
        ph1Var = equationContext.getVariable(bus1.getId(), VariableType.BUS_PHI);
        ph2Var = equationContext.getVariable(bus2.getId(), VariableType.BUS_PHI);
        variables = ImmutableList.of(v1Var, v2Var, ph1Var, ph2Var);
        r1 = bc.r1();
        r2 = bc.r2();
        b1 = bc.b1();
        b2 = bc.b2();
        g1 = bc.g1();
        g2 = bc.g2();
        y = bc.y();
        ksi = bc.ksi();
        a1 = bc.a1();
        a2 = bc.a2();
        cosKsi = Math.cos(ksi);
        sinKsi = Math.sin(ksi);
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
