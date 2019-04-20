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
public class OpenBranchSide1AcContext {

    private final BranchCharacteristics bc;
    private final double shunt;

    private final Variable v2Var;

    public OpenBranchSide1AcContext(Branch branch, Bus bus2, EquationContext equationContext) {
        Objects.requireNonNull(branch);
        Objects.requireNonNull(bus2);
        Objects.requireNonNull(equationContext);

        bc = new BranchCharacteristics(branch);
        shunt = (bc.g1() + bc.y() * Math.sin(bc.ksi())) * (bc.g1() + bc.y() * Math.sin(bc.ksi()))
                + (-bc.b1() + bc.y() * Math.cos(bc.ksi())) * (-bc.b1() + bc.y() * Math.cos(bc.ksi()));

        v2Var = equationContext.getVariable(bus2.getId(), VariableType.BUS_V);
    }

    public BranchCharacteristics getBc() {
        return bc;
    }

    public double getShunt() {
        return shunt;
    }

    public Variable getV2Var() {
        return v2Var;
    }

    public double p2(double v2) {
        return bc.r2() * bc.r2() * v2 * v2 * (bc.g2() + bc.y() * bc.y() * bc.g1() / shunt
                + (bc.b1() * bc.b1() + bc.g1() * bc.g1()) * bc.y() * Math.sin(bc.ksi()) / shunt);
    }

    public double p2(double[] x) {
        double v2 = x[v2Var.getColumn()];
        return p2(v2);
    }

    public double q2(double v2) {
        return -bc.r2() * bc.r2() * v2 * v2 * (bc.b2() + bc.y() * bc.y() * bc.b1() / shunt
                - (bc.b1() * bc.b1() + bc.g1() * bc.g1()) * bc.y() * Math.cos(bc.ksi()) / shunt);
    }

    public double q2(double[] x) {
        double v2 = x[v2Var.getColumn()];
        return q2(v2);
    }
}
