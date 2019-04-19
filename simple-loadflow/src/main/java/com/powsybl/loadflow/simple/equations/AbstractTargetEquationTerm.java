/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractTargetEquationTerm implements EquationTerm {

    private final Equation equation;

    private final List<Variable> variables;

    protected AbstractTargetEquationTerm(String id, EquationType equationType, VariableType variableType, EquationContext context) {
        this.equation = context.getEquation(id, equationType);
        variables = Collections.singletonList(context.getVariable(id, variableType));
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
        return x[variables.get(0).getColumn()];
    }

    @Override
    public double evalDer(Variable variable, double[] x) {
        return 1;
    }

    @Override
    public double evalRhs(Variable variable) {
        return 0;
    }
}
