/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc.equations;

import com.powsybl.loadflow.simple.equations.BusPhaseEquationTerm;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.equations.EquationType;
import com.powsybl.loadflow.simple.network.LfBranch;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.loadflow.simple.util.Evaluable;

import static com.powsybl.loadflow.simple.equations.EquationType.BUS_PHI;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class DcEquationSystem {

    private DcEquationSystem() {
    }

    public static EquationSystem create(LfNetwork network) {
        return create(network, new EquationContext());
    }

    public static EquationSystem create(LfNetwork network, EquationContext equationContext) {
        for (LfBus bus : network.getBuses()) {
            if (bus.isSlack()) {
                equationContext.getEquation(bus.getNum(), BUS_PHI).getTerms().add(new BusPhaseEquationTerm(bus, equationContext));
                equationContext.getEquation(bus.getNum(), EquationType.BUS_P).setToSolve(false);
            }
        }

        for (LfBranch branch : network.getBranches()) {
            LfBus bus1 = branch.getBus1();
            LfBus bus2 = branch.getBus2();
            if (bus1 != null && bus2 != null) {
                ClosedBranchSide1DcFlowEquationTerm p1 = ClosedBranchSide1DcFlowEquationTerm.create(branch, bus1, bus2, equationContext);
                ClosedBranchSide2DcFlowEquationTerm p2 = ClosedBranchSide2DcFlowEquationTerm.create(branch, bus1, bus2, equationContext);
                equationContext.getEquation(bus1.getNum(), EquationType.BUS_P).getTerms().add(p1);
                equationContext.getEquation(bus2.getNum(), EquationType.BUS_P).getTerms().add(p2);
                branch.setP1(p1);
                branch.setP2(p2);
            } else if (bus1 != null) {
                branch.setP1(Evaluable.ZERO);
            } else if (bus2 != null) {
                branch.setP2(Evaluable.ZERO);
            }
        }

        return new EquationSystem(equationContext.getEquations(), network);
    }
}
