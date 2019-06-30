/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc.equations;

import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.LfBranch;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class DcEquationSystem {

    private DcEquationSystem() {
    }

    public static EquationSystem create(NetworkContext networkContext) {
        return create(networkContext, new EquationContext());
    }

    public static EquationSystem create(NetworkContext networkContext, EquationContext equationContext) {
        List<EquationTerm> equationTerms = new ArrayList<>();
        List<VariableUpdate> variableUpdates = new ArrayList<>();

        for (LfBus bus : networkContext.getBuses()) {
            if (bus.isSlack()) {
                equationTerms.add(new BusPhaseEquationTerm(bus, equationContext));
                equationContext.getEquation(bus.getNum(), EquationType.BUS_P).setToSolve(false);
            }
        }

        for (LfBranch branch : networkContext.getBranches()) {
            LfBus bus1 = branch.getBus1();
            LfBus bus2 = branch.getBus2();
            if (bus1 != null && bus2 != null) {
                ClosedBranchSide1DcFlowEquationTerm p1 = ClosedBranchSide1DcFlowEquationTerm.create(branch, bus1, bus2, equationContext);
                ClosedBranchSide2DcFlowEquationTerm p2 = ClosedBranchSide2DcFlowEquationTerm.create(branch, bus1, bus2, equationContext);
                equationTerms.add(p1);
                equationTerms.add(p2);
                variableUpdates.add(new ClosedBranchDcFlowUpdate(branch, p1, p2));
            } else if (bus1 != null) {
                variableUpdates.add(new OpenBranchSide2DcFlowUpdate(branch));
            } else if (bus2 != null) {
                variableUpdates.add(new OpenBranchSide1DcFlowUpdate(branch));
            }
        }

        return new EquationSystem(equationTerms, variableUpdates, networkContext);
    }
}
