/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

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
public class OpenBranchSide2AcContext {

    private final BranchCharacteristics bc;
    private final double shunt;

    private final Variable v1Var;

    public OpenBranchSide2AcContext(Branch branch, Bus bus1, EquationContext equationContext) {
        Objects.requireNonNull(branch);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(equationContext);

        bc = new BranchCharacteristics(branch);
        shunt = (bc.g2() + bc.y() * Math.sin(bc.ksi())) * (bc.g2() + bc.y() * Math.sin(bc.ksi()))
                + (-bc.b2() + bc.y() * Math.cos(bc.ksi())) * (-bc.b2() + bc.y() * Math.cos(bc.ksi()));

        v1Var = equationContext.getVariable(bus1.getId(), VariableType.BUS_V);
    }

    public BranchCharacteristics getBc() {
        return bc;
    }

    public double getShunt() {
        return shunt;
    }

    public Variable getV1Var() {
        return v1Var;
    }

    public double p1(double v1) {
        return bc.r1() * bc.r1() * v1 * v1 * (bc.g1() + bc.y() * bc.y() * bc.g2() / shunt
                + (bc.b2() * bc.b2() + bc.g2() * bc.g2()) * bc.y() * Math.sin(bc.ksi()) / shunt);
    }

    public double p1(double[] x) {
        double v1 = x[v1Var.getColumn()];
        return p1(v1);
    }

    public double q1(double v1) {
        return -bc.r1() * bc.r1() * v1 * v1 * (bc.b1() + bc.y() * bc.y() * bc.b2() / shunt
                - (bc.b2() * bc.b2() + bc.g2() * bc.g2()) * bc.y() * Math.cos(bc.ksi()) / shunt);
    }

    public double q1(double[] x) {
        double v1 = x[v1Var.getColumn()];
        return q1(v1);
    }
}
