/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class AcEquationSystemMaker implements EquationSystemMaker {

    @Override
    public EquationSystem make(NetworkContext networkContext, EquationContext equationContext) {
        List<EquationTerm> equationTerms = new ArrayList<>();
        List<VariableUpdate> variableUpdates = new ArrayList<>();

        equationTerms.add(new BusPhaseEquationTerm(networkContext.getSlackBus(), equationContext));

        for (Bus bus : networkContext.getBuses()) {
            if (networkContext.isPvBus(bus.getId())) {
                equationTerms.add(new BusVoltageEquationTerm(bus, equationContext));
            }
        }

        for (Branch branch : networkContext.getBranches()) {
            Bus bus1 = branch.getTerminal1().getBusView().getBus();
            Bus bus2 = branch.getTerminal2().getBusView().getBus();
            if (bus1 != null && bus2 != null) {
                ClosedBranchAcContext branchContext = new ClosedBranchAcContext(branch, bus1, bus2, equationContext);
                if (!networkContext.isSlackBus(bus1.getId())) {
                    equationTerms.add(new ClosedBranchActiveFlowEquationTerm(branchContext, Branch.Side.ONE, bus1, bus2, networkContext, equationContext));
                }
                if (!networkContext.isPvBus(bus1.getId())) {
                    equationTerms.add(new ClosedBranchReactiveFlowEquationTerm(branchContext, Branch.Side.ONE, bus1, bus2, networkContext, equationContext));
                }
                if (!networkContext.isSlackBus(bus2.getId())) {
                    equationTerms.add(new ClosedBranchActiveFlowEquationTerm(branchContext, Branch.Side.TWO, bus1, bus2, networkContext, equationContext));
                }
                if (!networkContext.isPvBus(bus2.getId())) {
                    equationTerms.add(new ClosedBranchReactiveFlowEquationTerm(branchContext, Branch.Side.TWO, bus1, bus2, networkContext, equationContext));
                }
                variableUpdates.add(new ClosedBranchAcFlowUpdate(branchContext));
            } else if (bus1 != null) {
                OpenBranchSide2AcContext branchContext = new OpenBranchSide2AcContext(branch, bus1, equationContext);
                if (!networkContext.isSlackBus(bus1.getId())) {
                    equationTerms.add(new OpenBranchSide2ActiveFlowEquationTerm(branchContext, bus1, equationContext));
                }
                if (!networkContext.isPvBus(bus1.getId())) {
                    equationTerms.add(new OpenBranchSide2ReactiveFlowEquationTerm(branchContext, bus1, equationContext));
                }
                variableUpdates.add(new OpenBranchSide2AcFlowUpdate(branchContext));
            } else if (bus2 != null) {
                OpenBranchSide1AcContext branchContext = new OpenBranchSide1AcContext(branch, bus2, equationContext);
                if (!networkContext.isSlackBus(bus2.getId())) {
                    equationTerms.add(new OpenBranchSide1ActiveFlowEquationTerm(branchContext, bus2, equationContext));
                }
                if (!networkContext.isPvBus(bus2.getId())) {
                    equationTerms.add(new OpenBranchSide1ReactiveFlowEquationTerm(branchContext, bus2, equationContext));
                }
                variableUpdates.add(new OpenBranchSide1AcFlowUpdate(branchContext));
            }
        }

        return new EquationSystem(equationTerms, variableUpdates, networkContext);
    }
}
