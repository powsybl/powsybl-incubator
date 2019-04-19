/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.Variable;
import com.powsybl.loadflow.simple.equations.VariableType;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ClosedBranchAcContext {

    private final BranchCharacteristics bc;

    private final Variable v1Var;
    private final Variable v2Var;
    private final Variable ph1Var;
    private final Variable ph2Var;

    public ClosedBranchAcContext(Branch branch, Bus bus1, Bus bus2, EquationContext equationContext) {
        Objects.requireNonNull(branch);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);
        Objects.requireNonNull(equationContext);

        bc = new BranchCharacteristics(branch);

        v1Var = equationContext.getVariable(bus1.getId(), VariableType.BUS_V);
        v2Var = equationContext.getVariable(bus2.getId(), VariableType.BUS_V);
        ph1Var = equationContext.getVariable(bus1.getId(), VariableType.BUS_PHI);
        ph2Var = equationContext.getVariable(bus2.getId(), VariableType.BUS_PHI);
    }

    public BranchCharacteristics getBc() {
        return bc;
    }

    public Variable getV1Var() {
        return v1Var;
    }

    public Variable getV2Var() {
        return v2Var;
    }

    public Variable getPh1Var() {
        return ph1Var;
    }

    public Variable getPh2Var() {
        return ph2Var;
    }

    public double p1(double v1, double v2, double ph1, double ph2) {
        return bc.r1() * v1 * (bc.g1() * bc.r1() * v1 + bc.y() * bc.r1() * v1 * Math.sin(bc.ksi()) - bc.y() * bc.r2() * v2 * Math.sin(bc.ksi() - bc.a1() + bc.a2() - ph1 + ph2));
    }

    public double p1(double[] x) {
        double v1 = x[v1Var.getColumn()];
        double v2 = x[v2Var.getColumn()];
        double ph1 = x[ph1Var.getColumn()];
        double ph2 = x[ph2Var.getColumn()];
        return p1(v1, v2, ph1, ph2);
    }

    public double  p2(double v1, double v2, double ph1, double ph2) {
        return bc.r2() * v2 * (bc.g2() * bc.r2() * v2 - bc.y() * bc.r1() * v1 * Math.sin(bc.ksi() + bc.a1() - bc.a2() + ph1 - ph2) + bc.y() * bc.r2() * v2 * Math.sin(bc.ksi()));
    }

    public double p2(double[] x) {
        double v1 = x[v1Var.getColumn()];
        double v2 = x[v2Var.getColumn()];
        double ph1 = x[ph1Var.getColumn()];
        double ph2 = x[ph2Var.getColumn()];
        return p2(v1, v2, ph1, ph2);
    }

    public double q1(double v1, double v2, double ph1, double ph2) {
        return bc.r1() * v1 * (-bc.b1() * bc.r1() * v1 + bc.y() * bc.r1() * v1 * Math.cos(bc.ksi()) - bc.y() * bc.r2() * v2 * Math.cos(bc.ksi() - bc.a1() + bc.a2() - ph1 + ph2));
    }

    public double q1(double[] x) {
        double v1 = x[v1Var.getColumn()];
        double v2 = x[v2Var.getColumn()];
        double ph1 = x[ph1Var.getColumn()];
        double ph2 = x[ph2Var.getColumn()];
        return q1(v1, v2, ph1, ph2);
    }

    public double q2(double v1, double v2, double ph1, double ph2) {
        return bc.r2() * v2 * (-bc.b2() * bc.r2() * v2 - bc.y() * bc.r1() * v1 * Math.cos(bc.ksi() + bc.a1() - bc.a2() + ph1 - ph2) + bc.y() * bc.r2() * v2 * Math.cos(bc.ksi()));
    }

    public double q2(double[] x) {
        double v1 = x[v1Var.getColumn()];
        double v2 = x[v2Var.getColumn()];
        double ph1 = x[ph1Var.getColumn()];
        double ph2 = x[ph2Var.getColumn()];
        return q2(v1, v2, ph1, ph2);
    }
}
