/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

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

    private final OpenBranchSide2AcContext branchContext;

    public OpenBranchSide2ReactiveFlowEquationTerm(OpenBranchSide2AcContext branchContext, Bus bus1, EquationContext equationContext) {
        super(EquationType.BUS_Q, VariableType.BUS_V, bus1, equationContext);
        this.branchContext = Objects.requireNonNull(branchContext);
    }

    @Override
    public double eval(double[] x) {
        return branchContext.q1(x);
    }

    @Override
    public double der(Variable variable, double[] x) {
        if (variable.equals(branchContext.getV1Var())) {
            BranchCharacteristics bc = branchContext.getBc();
            double v1 = x[branchContext.getV1Var().getColumn()];
            return -2 * v1 * bc.r1() * bc.r1() * (bc.b1() + bc.y() * bc.y() * bc.b2() / branchContext.getShunt()
                    - (bc.b2() * bc.b2() + bc.g2() * bc.g2()) * bc.y() * Math.cos(bc.ksi()) / branchContext.getShunt());
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }
}
