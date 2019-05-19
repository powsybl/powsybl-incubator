/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.Equation;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationType;
import com.powsybl.loadflow.simple.equations.Variable;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class ClosedBranchSide1DcFlowEquationTerm extends AbstractClosedBranchDcFlowEquationTerm {

    private double p1;

    private ClosedBranchSide1DcFlowEquationTerm(BranchCharacteristics bc, Bus bus1, Bus bus2,
                                                Equation equation, EquationContext equationContext) {
        super(bc, bus1, bus2, equation, equationContext);
    }

    public static ClosedBranchSide1DcFlowEquationTerm create(BranchCharacteristics bc, Bus bus1, Bus bus2, EquationContext equationContext) {
        Objects.requireNonNull(bc);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);
        Objects.requireNonNull(equationContext);
        Equation equation = equationContext.getEquation(bus1.getId(), EquationType.BUS_P);
        return new ClosedBranchSide1DcFlowEquationTerm(bc, bus1, bus2, equation, equationContext);
    }

    @Override
    public void update(double[] x) {
        Objects.requireNonNull(x);
        double ph1 = x[ph1Var.getColumn()];
        double ph2 = x[ph2Var.getColumn()];
        double deltaPhase =  ph2 - ph1 + bc.a2() - bc.a1();
        p1 = -power * deltaPhase;
    }

    @Override
    public double eval() {
        return p1;
    }

    @Override
    public double der(Variable variable) {
        Objects.requireNonNull(variable);
        if (variable.equals(ph1Var)) {
            return power;
        } else if (variable.equals(ph2Var)) {
            return -power;
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }

    @Override
    public double rhs(Variable variable) {
        Objects.requireNonNull(variable);
        if (variable.equals(ph1Var)) {
            return -power * (bc.a2() - bc.a1());
        }
        return 0;
    }
}
