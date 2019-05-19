/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
abstract class AbstractOpenBranchAcEquationTerm extends AbstractBranchAcEquationTerm {

    protected final Equation equation;

    protected final List<Variable> variables;

    protected double shunt;

    protected AbstractOpenBranchAcEquationTerm(BranchCharacteristics bc, EquationType equationType, VariableType variableType,
                                               Bus bus, EquationContext equationContext) {
        super(bc);
        equation = equationContext.getEquation(bus.getId(), equationType);
        variables = Collections.singletonList(equationContext.getVariable(bus.getId(), variableType));
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
