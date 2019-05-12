/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
abstract class AbstractOpenBranchAcEquationTerm implements EquationTerm {

    private final Equation equation;

    private final List<Variable> variables;

    protected AbstractOpenBranchAcEquationTerm(EquationType equationType, VariableType variableType,
                                               Bus bus, EquationContext equationContext) {
        Objects.requireNonNull(equationType);
        Objects.requireNonNull(variableType);
        Objects.requireNonNull(bus);
        Objects.requireNonNull(equationContext);
        equation = equationContext.getEquation(bus.getId(), equationType);
        variables = Collections.singletonList(equationContext.getVariable(bus.getId(), variableType));
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
