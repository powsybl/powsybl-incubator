/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.Variable;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ClosedBranchSide2DcFlowEquationTerm extends AbstractClosedBranchDcFlowEquationTerm {

    public ClosedBranchSide2DcFlowEquationTerm(ClosedBranchDcContext branchContext, Bus bus, EquationContext equationContext) {
        super(branchContext, bus, equationContext);
    }

    @Override
    public double eval(double[] x) {
        Objects.requireNonNull(x);
        return branchContext.p2(x);
    }

    @Override
    public double der(Variable variable, double[] x) {
        Objects.requireNonNull(variable);
        Objects.requireNonNull(x);
        if (variable.equals(branchContext.getPh1Var())) {
            return -branchContext.getPower();
        } else if (variable.equals(branchContext.getPh2Var())) {
            return branchContext.getPower();
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }

    @Override
    public double rhs(Variable variable) {
        Objects.requireNonNull(variable);
        if (variable.equals(branchContext.getPh2Var())) {
            return branchContext.getPower() * (branchContext.getBc().a2() - branchContext.getBc().a1());
        }
        return 0;
    }
}
