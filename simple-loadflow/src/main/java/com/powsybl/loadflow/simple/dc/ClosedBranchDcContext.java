/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.Variable;
import com.powsybl.loadflow.simple.equations.VariableType;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ClosedBranchDcContext {

    private final BranchCharacteristics bc;

    private final Variable ph1Var;
    private final Variable ph2Var;

    private final double power;

    public ClosedBranchDcContext(Branch branch, Bus bus1, Bus bus2, EquationContext equationContext) {
        bc = new BranchCharacteristics(branch);
        ph1Var = equationContext.getVariable(bus1.getId(), VariableType.BUS_PHI);
        ph2Var = equationContext.getVariable(bus2.getId(), VariableType.BUS_PHI);

        double v1 = bc.getBranch().getTerminal1().getVoltageLevel().getNominalV();
        double v2 = bc.getBranch().getTerminal2().getVoltageLevel().getNominalV();
        power =  1 / bc.x() * v1 * bc.r1() * v2 * bc.r2();

    }

    public BranchCharacteristics getBc() {
        return bc;
    }

    public Variable getPh1Var() {
        return ph1Var;
    }

    public Variable getPh2Var() {
        return ph2Var;
    }

    public double getPower() {
        return power;
    }

    public double p1(double[] x) {
        double ph1 = x[ph1Var.getColumn()];
        double ph2 = x[ph2Var.getColumn()];
        double deltaPhase =  ph2 - ph1 + bc.a2() - bc.a1();
        return -power * deltaPhase;
    }

    public double p2(double[] x) {
        return -p1(x);
    }
}
