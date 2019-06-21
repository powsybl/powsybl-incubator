/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.LfBranch;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.LfShunt;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class AcEquationSystem {

    private AcEquationSystem() {
    }

    public static EquationSystem create(NetworkContext networkContext) {
        List<EquationTerm> equationTerms = new ArrayList<>();
        List<VariableUpdate> variableUpdates = new ArrayList<>();

        EquationContext equationContext = new EquationContext();

        for (LfBus bus : networkContext.getBuses()) {
            if (bus.isSlack()) {
                equationTerms.add(new BusPhaseEquationTerm(bus, equationContext));
                equationContext.getEquation(bus.getNum(), EquationType.BUS_P).setPartOfSystem(false);
            }
            if (bus.hasVoltageControl()) {
                equationTerms.add(new BusVoltageEquationTerm(bus, equationContext));
                equationContext.getEquation(bus.getNum(), EquationType.BUS_Q).setPartOfSystem(false);
            }
            for (LfShunt shunt : bus.getShunts()) {
                ShuntCompensatorReactiveFlowEquationTerm q = new ShuntCompensatorReactiveFlowEquationTerm(shunt, bus, networkContext, equationContext);
                equationTerms.add(q);
                variableUpdates.add(new ShuntCompensatorReactiveFlowUpdate(shunt, q));
            }
        }

        for (LfBranch branch : networkContext.getBranches()) {
            LfBus bus1 = branch.getBus1();
            LfBus bus2 = branch.getBus2();
            if (bus1 != null && bus2 != null) {
                ClosedBranchSide1ActiveFlowEquationTerm p1 = new ClosedBranchSide1ActiveFlowEquationTerm(branch, bus1, bus2, equationContext);
                ClosedBranchSide1ReactiveFlowEquationTerm q1 = new ClosedBranchSide1ReactiveFlowEquationTerm(branch, bus1, bus2, equationContext);
                ClosedBranchSide2ActiveFlowEquationTerm p2 = new ClosedBranchSide2ActiveFlowEquationTerm(branch, bus1, bus2, equationContext);
                ClosedBranchSide2ReactiveFlowEquationTerm q2 = new ClosedBranchSide2ReactiveFlowEquationTerm(branch, bus1, bus2, equationContext);
                equationTerms.add(p1);
                equationTerms.add(q1);
                equationTerms.add(p2);
                equationTerms.add(q2);
                variableUpdates.add(new ClosedBranchAcFlowUpdate(branch, p1, q1, p2, q2));
            } else if (bus1 != null) {
                OpenBranchSide2ActiveFlowEquationTerm p1 = new OpenBranchSide2ActiveFlowEquationTerm(branch, bus1, equationContext);
                OpenBranchSide2ReactiveFlowEquationTerm q1 = new OpenBranchSide2ReactiveFlowEquationTerm(branch, bus1, equationContext);
                equationTerms.add(p1);
                equationTerms.add(q1);
                variableUpdates.add(new OpenBranchSide2AcFlowUpdate(branch, p1, q1));
            } else if (bus2 != null) {
                OpenBranchSide1ActiveFlowEquationTerm p2 = new OpenBranchSide1ActiveFlowEquationTerm(branch, bus2, equationContext);
                OpenBranchSide1ReactiveFlowEquationTerm q2 = new OpenBranchSide1ReactiveFlowEquationTerm(branch, bus2, equationContext);
                equationTerms.add(p2);
                equationTerms.add(q2);
                variableUpdates.add(new OpenBranchSide1AcFlowUpdate(branch, p2, q2));
            }
        }

        return new EquationSystem(equationTerms, variableUpdates, networkContext);
    }
}
