/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ShuntCompensatorReactiveFlowEquationTerm implements EquationTerm {

    private final ShuntCompensatorContext shuntContext;

    private final Equation equation;

    private final List<Variable> variables;

    public ShuntCompensatorReactiveFlowEquationTerm(ShuntCompensatorContext shuntContext, Bus bus,
                                                    NetworkContext networkContext, EquationContext equationContext) {
        this.shuntContext = Objects.requireNonNull(shuntContext);
        Objects.requireNonNull(bus);
        Objects.requireNonNull(networkContext);
        Objects.requireNonNull(equationContext);
        equation = equationContext.getEquation(bus.getId(), EquationType.BUS_Q);
        variables = Collections.singletonList(shuntContext.getvVar());
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
        return shuntContext.q(x);
    }

    @Override
    public double der(Variable variable, double[] x) {
        if (variable.equals(shuntContext.getvVar())) {
            double v = x[variable.getColumn()];
            return -2 * shuntContext.b() * v;
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }

    @Override
    public double rhs(Variable variable) {
        return 0;
    }
}
