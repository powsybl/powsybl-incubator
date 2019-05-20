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
public class OpenBranchSide1ReactiveFlowEquationTerm extends AbstractOpenBranchAcFlowEquationTerm {

    private final Variable v2Var;

    private double q2;

    private double dq2dv2;

    public OpenBranchSide1ReactiveFlowEquationTerm(BranchCharacteristics bc, Bus bus2, EquationContext equationContext) {
        super(bc, EquationType.BUS_Q, VariableType.BUS_V, bus2, equationContext);
        v2Var = equationContext.getVariable(bus2.getId(), VariableType.BUS_V);
    }

    @Override
    public void update(double[] x) {
        Objects.requireNonNull(x);
        double v2 = x[v2Var.getColumn()];
        q2 = -r2 * r2 * v2 * v2 * (b2 + y * y * b1 / shunt - (b1 * b1 + g1 * g1) * y * cosKsi / shunt);
        dq2dv2 = -2 * v2 * r2 * r2 * (b2 + y * y * b1 / shunt - (b1 * b1 + g1 * g1) * y * cosKsi / shunt);
    }

    @Override
    public double eval() {
        return q2;
    }

    @Override
    public double der(Variable variable) {
        Objects.requireNonNull(variable);
        if (variable.equals(v2Var)) {
            return dq2dv2;
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }
}
