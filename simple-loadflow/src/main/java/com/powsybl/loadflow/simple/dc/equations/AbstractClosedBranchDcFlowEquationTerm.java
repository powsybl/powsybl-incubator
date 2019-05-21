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
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractClosedBranchDcFlowEquationTerm implements EquationTerm {

    protected final BranchCharacteristics bc;

    protected final Variable ph1Var;

    protected final Variable ph2Var;

    protected final List<Variable> variables;

    protected final Equation equation;

    protected final double power;

    protected AbstractClosedBranchDcFlowEquationTerm(BranchCharacteristics bc, Bus bus1, Bus bus2,
                                                     Equation equation, EquationContext equationContext) {
        this.bc = Objects.requireNonNull(bc);
        this.equation = Objects.requireNonNull(equation);
        ph1Var = equationContext.getVariable(bus1.getId(), VariableType.BUS_PHI);
        ph2Var = equationContext.getVariable(bus2.getId(), VariableType.BUS_PHI);
        variables = ImmutableList.of(ph1Var, ph2Var);
        power =  1 / bc.x() * bc.getBranch().getTerminal1().getVoltageLevel().getNominalV() * bc.r1() * bc.getBranch().getTerminal2().getVoltageLevel().getNominalV() * bc.r2();
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
