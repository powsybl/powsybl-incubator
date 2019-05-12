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
public class OpenBranchSide2ReactiveFlowEquationTerm extends AbstractOpenBranchAcEquationTerm {

    private final Variable v1Var;

    public OpenBranchSide2ReactiveFlowEquationTerm(BranchCharacteristics bc, Bus bus1, EquationContext equationContext) {
        super(bc, EquationType.BUS_Q, VariableType.BUS_V, bus1, equationContext);
        v1Var = equationContext.getVariable(bus1.getId(), VariableType.BUS_V);
    }

    @Override
    public double eval(double[] x) {
        Objects.requireNonNull(x);
        double v1 = x[v1Var.getColumn()];
        return -bc.r1() * bc.r1() * v1 * v1 * (bc.b1() + bc.y() * bc.y() * bc.b2() / bc.shunt()
                - (bc.b2() * bc.b2() + bc.g2() * bc.g2()) * bc.y() * Math.cos(bc.ksi()) / bc.shunt());
    }

    @Override
    public double der(Variable variable, double[] x) {
        Objects.requireNonNull(variable);
        Objects.requireNonNull(x);
        if (variable.equals(v1Var)) {
            double v1 = x[v1Var.getColumn()];
            return -2 * v1 * bc.r1() * bc.r1() * (bc.b1() + bc.y() * bc.y() * bc.b2() / bc.shunt()
                    - (bc.b2() * bc.b2() + bc.g2() * bc.g2()) * bc.y() * Math.cos(bc.ksi()) / bc.shunt());
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }
}
