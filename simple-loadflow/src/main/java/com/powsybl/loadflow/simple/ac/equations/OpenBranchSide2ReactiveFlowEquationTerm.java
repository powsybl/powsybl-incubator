/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationType;
import com.powsybl.loadflow.simple.equations.Variable;
import com.powsybl.loadflow.simple.equations.VariableType;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class OpenBranchSide2ReactiveFlowEquationTerm extends AbstractOpenBranchAcFlowEquationTerm {

    private final Variable v1Var;

    private double q1;

    private double dq1dv1;

    public OpenBranchSide2ReactiveFlowEquationTerm(BranchCharacteristics bc, Bus bus1, EquationContext equationContext) {
        super(bc, EquationType.BUS_Q, VariableType.BUS_V, bus1, equationContext);
        v1Var = equationContext.getVariable(bus1.getId(), VariableType.BUS_V);
    }

    @Override
    public void update(double[] x) {
        Objects.requireNonNull(x);
        double v1 = x[v1Var.getColumn()];
        q1 = -r1 * r1 * v1 * v1 * (b1 + y * y * b2 / shunt - (b2 * b2 + g2 * g2) * y * cosKsi / shunt);
        dq1dv1 = -2 * v1 * r1 * r1 * (b1 + y * y * b2 / shunt - (b2 * b2 + g2 * g2) * y * cosKsi / shunt);
    }

    @Override
    public double eval() {
        return q1;
    }

    @Override
    public double der(Variable variable) {
        Objects.requireNonNull(variable);
        if (variable.equals(v1Var)) {
            return dq1dv1;
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }
}
