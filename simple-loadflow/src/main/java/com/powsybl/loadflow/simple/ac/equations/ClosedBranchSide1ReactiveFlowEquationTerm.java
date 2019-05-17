/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.api.Bus;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationType;
import com.powsybl.loadflow.simple.equations.Variable;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ClosedBranchSide1ReactiveFlowEquationTerm extends AbstractClosedBranchAcFlowEquationTerm {

    public ClosedBranchSide1ReactiveFlowEquationTerm(BranchCharacteristics bc, Bus bus1, Bus bus2, EquationContext equationContext) {
        super(bc, bus1, bus2, equationContext.getEquation(bus1.getId(), EquationType.BUS_Q), equationContext);
    }

    @Override
    public double eval(double[] x) {
        Objects.requireNonNull(x);
        double v1 = x[v1Var.getColumn()];
        double v2 = x[v2Var.getColumn()];
        double ph1 = x[ph1Var.getColumn()];
        double ph2 = x[ph2Var.getColumn()];
        return bc.r1() * v1 * (-bc.b1() * bc.r1() * v1 + bc.y() * bc.r1() * v1 * Math.cos(bc.ksi()) - bc.y() * bc.r2() * v2 * Math.cos(bc.ksi() - bc.a1() + bc.a2() - ph1 + ph2));
    }

    private double dq1dv1(double v1, double v2, double ph1, double ph2, BranchCharacteristics bc) {
        return bc.r1() * (-2 * bc.b1() * bc.r1() * v1 + 2 * bc.y() * bc.r1() * v1 * Math.cos(bc.ksi()) - bc.y() * bc.r2() * v2 * Math.cos(bc.ksi() - bc.a1() + bc.a2() - ph1 + ph2));
    }

    private double dq1dv2(double v1, double ph1, double ph2, BranchCharacteristics bc) {
        return -bc.y() * bc.r1() * bc.r2() * v1 * Math.cos(bc.ksi() - bc.a1() + bc.a2() - ph1 + ph2);
    }

    private double dq1dph1(double v1, double v2, double ph1, double ph2, BranchCharacteristics bc) {
        return -bc.y() * bc.r1() * bc.r2() * v1 * v2 * Math.sin(bc.ksi() - bc.a1() + bc.a2() - ph1 + ph2);
    }

    private double dq1dph2(double v1, double v2, double ph1, double ph2, BranchCharacteristics bc) {
        return bc.y() * bc.r1() * bc.r2() * v1 * v2 * Math.sin(bc.ksi() - bc.a1() + bc.a2() - ph1 + ph2);
    }

    private double dq1(Variable variable, double v1, double v2, double ph1, double ph2) {
        if (variable.equals(v1Var)) {
            return dq1dv1(v1, v2, ph1, ph2, bc);
        } else if (variable.equals(v2Var)) {
            return dq1dv2(v1, ph1, ph2, bc);
        } else if (variable.equals(ph1Var)) {
            return dq1dph1(v1, v2, ph1, ph2, bc);
        } else if (variable.equals(ph2Var)) {
            return dq1dph2(v1, v2, ph1, ph2, bc);
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }

    @Override
    public double der(Variable variable, double[] x) {
        Objects.requireNonNull(variable);
        Objects.requireNonNull(x);
        double v1 = x[v1Var.getColumn()];
        double v2 = x[v2Var.getColumn()];
        double ph1 = x[ph1Var.getColumn()];
        double ph2 = x[ph2Var.getColumn()];
        return dq1(variable, v1, v2, ph1, ph2);
    }
}
