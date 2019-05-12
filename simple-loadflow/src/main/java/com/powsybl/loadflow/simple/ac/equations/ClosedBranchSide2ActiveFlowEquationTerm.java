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
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ClosedBranchSide2ActiveFlowEquationTerm extends AbstractClosedBranchAcFlowEquationTerm {

    public ClosedBranchSide2ActiveFlowEquationTerm(ClosedBranchAcContext branchContext, Bus bus, EquationContext equationContext) {
        super(branchContext, bus, EquationType.BUS_P, equationContext);
    }

    @Override
    public double eval(double[] x) {
        Objects.requireNonNull(x);
        double v1 = x[branchContext.getV1Var().getColumn()];
        double v2 = x[branchContext.getV2Var().getColumn()];
        double ph1 = x[branchContext.getPh1Var().getColumn()];
        double ph2 = x[branchContext.getPh2Var().getColumn()];
        return branchContext.p2(v1, v2, ph1, ph2);
    }

    private double dp2dv1(double v2, double ph1, double ph2, BranchCharacteristics bc) {
        return -bc.y() * bc.r1() * bc.r2() * v2 * Math.sin(bc.ksi() + bc.a1() - bc.a2() + ph1 - ph2);
    }

    private double dp2dv2(double v1, double v2, double ph1, double ph2, BranchCharacteristics bc) {
        return bc.r2() * (2 * bc.g2() * bc.r2() * v2 - bc.y() * bc.r1() * v1 * Math.sin(bc.ksi() + bc.a1() - bc.a2() + ph1 - ph2) + 2 * bc.y() * bc.r2() * v2 * Math.sin(bc.ksi()));
    }

    private double dp2dph1(double v1, double v2, double ph1, double ph2, BranchCharacteristics bc) {
        return -bc.y() * bc.r1() * bc.r2() * v1 * v2 * Math.cos(bc.ksi() + bc.a1() - bc.a2() + ph1 - ph2);
    }

    private double dp2dph2(double v1, double v2, double ph1, double ph2, BranchCharacteristics bc) {
        return bc.y() * bc.r1() * bc.r2() * v1 * v2 * Math.cos(bc.ksi() + bc.a1() - bc.a2() + ph1 - ph2);
    }

    private double dp2(Variable variable, double v1, double v2, double ph1, double ph2) {
        BranchCharacteristics bc = branchContext.getBc();
        if (variable.equals(branchContext.getV1Var())) {
            return dp2dv1(v2, ph1, ph2, bc);
        } else if (variable.equals(branchContext.getV2Var())) {
            return dp2dv2(v1, v2, ph1, ph2, bc);
        } else if (variable.equals(branchContext.getPh1Var())) {
            return dp2dph1(v1, v2, ph1, ph2, bc);
        } else if (variable.equals(branchContext.getPh2Var())) {
            return dp2dph2(v1, v2, ph1, ph2, bc);
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }

    @Override
    public double der(Variable variable, double[] x) {
        Objects.requireNonNull(variable);
        double v1 = x[branchContext.getV1Var().getColumn()];
        double v2 = x[branchContext.getV2Var().getColumn()];
        double ph1 = x[branchContext.getPh1Var().getColumn()];
        double ph2 = x[branchContext.getPh2Var().getColumn()];
        return dp2(variable, v1, v2, ph1, ph2);
    }

    @Override
    public double rhs(Variable variable) {
        return 0;
    }
}
