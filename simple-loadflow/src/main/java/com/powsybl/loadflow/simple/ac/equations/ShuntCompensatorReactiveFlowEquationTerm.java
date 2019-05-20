/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ShuntCompensatorReactiveFlowEquationTerm implements EquationTerm {

    private final ShuntCompensator sc;

    private final Variable vVar;

    private final Equation equation;

    private final List<Variable> variables;

    private final double b;

    private double q;

    private double dqdv;

    public ShuntCompensatorReactiveFlowEquationTerm(ShuntCompensator sc, Bus bus, NetworkContext networkContext,
                                                    EquationContext equationContext) {
        this.sc = Objects.requireNonNull(sc);
        Objects.requireNonNull(bus);
        Objects.requireNonNull(networkContext);
        Objects.requireNonNull(equationContext);
        equation = equationContext.getEquation(bus.getId(), EquationType.BUS_Q);
        vVar = equationContext.getVariable(bus.getId(), VariableType.BUS_V);
        variables = Collections.singletonList(vVar);
        b = sc.getCurrentB();
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
    public void update(double[] x) {
        Objects.requireNonNull(x);
        double v = x[vVar.getColumn()];
        q = -b * v * v;
        dqdv = -2 * b * v;
    }

    @Override
    public double eval() {
        return q;
    }

    @Override
    public double der(Variable variable) {
        Objects.requireNonNull(variable);
        if (variable.equals(vVar)) {
            return dqdv;
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }

    @Override
    public double rhs(Variable variable) {
        return 0;
    }
}
