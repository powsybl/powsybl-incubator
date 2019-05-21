/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.LfBranch;
import com.powsybl.loadflow.simple.network.LfBus;

import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
abstract class AbstractOpenBranchAcFlowEquationTerm extends AbstractBranchAcFlowEquationTerm {

    protected final Equation equation;

    protected final List<Variable> variables;

    protected double shunt;

    protected AbstractOpenBranchAcFlowEquationTerm(LfBranch branch, EquationType equationType, VariableType variableType,
                                                   LfBus bus, EquationContext equationContext) {
        super(branch);
        equation = equationContext.getEquation(bus.getNum(), equationType);
        variables = Collections.singletonList(equationContext.getVariable(bus.getNum(), variableType));
        shunt = (g1 + y * sinKsi) * (g1 + y * sinKsi) + (-b1 + y * cosKsi) * (-b1 + y * cosKsi);
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
