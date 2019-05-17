/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.api.Branch;
import com.powsybl.iidm.api.Bus;
import com.powsybl.iidm.api.ShuntCompensator;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class AcEquationSystem {

    private AcEquationSystem() {
    }

    public static EquationSystem create(NetworkContext networkContext, EquationContext equationContext) {
        List<EquationTerm> equationTerms = new ArrayList<>();
        List<VariableUpdate> variableUpdates = new ArrayList<>();

        equationTerms.add(new BusPhaseEquationTerm(networkContext.getSlackBus(), equationContext));

        for (Bus bus : networkContext.getBuses()) {
            if (networkContext.isPvBus(bus.getId())) {
                equationTerms.add(new BusVoltageEquationTerm(bus, equationContext));
            }
        }

        for (Branch branch : networkContext.getBranches()) {
            BranchCharacteristics bc = new BranchCharacteristics(branch);
            Bus bus1 = branch.getTerminal1().getBusView().getBus();
            Bus bus2 = branch.getTerminal2().getBusView().getBus();
            if (bus1 != null && bus2 != null) {
                ClosedBranchSide1ActiveFlowEquationTerm p1 = new ClosedBranchSide1ActiveFlowEquationTerm(bc, bus1, bus2, equationContext);
                ClosedBranchSide1ReactiveFlowEquationTerm q1 = new ClosedBranchSide1ReactiveFlowEquationTerm(bc, bus1, bus2, equationContext);
                ClosedBranchSide2ActiveFlowEquationTerm p2 = new ClosedBranchSide2ActiveFlowEquationTerm(bc, bus1, bus2, equationContext);
                ClosedBranchSide2ReactiveFlowEquationTerm q2 = new ClosedBranchSide2ReactiveFlowEquationTerm(bc, bus1, bus2, equationContext);
                if (!networkContext.isSlackBus(bus1.getId())) {
                    equationTerms.add(p1);
                }
                if (!networkContext.isPvBus(bus1.getId())) {
                    equationTerms.add(q1);
                }
                if (!networkContext.isSlackBus(bus2.getId())) {
                    equationTerms.add(p2);
                }
                if (!networkContext.isPvBus(bus2.getId())) {
                    equationTerms.add(q2);
                }
                variableUpdates.add(new ClosedBranchAcFlowUpdate(branch, p1, q1, p2, q2));
            } else if (bus1 != null) {
                OpenBranchSide2ActiveFlowEquationTerm p1 = new OpenBranchSide2ActiveFlowEquationTerm(bc, bus1, equationContext);
                OpenBranchSide2ReactiveFlowEquationTerm q1 = new OpenBranchSide2ReactiveFlowEquationTerm(bc, bus1, equationContext);
                if (!networkContext.isSlackBus(bus1.getId())) {
                    equationTerms.add(p1);
                }
                if (!networkContext.isPvBus(bus1.getId())) {
                    equationTerms.add(q1);
                }
                variableUpdates.add(new OpenBranchSide2AcFlowUpdate(branch, p1, q1));
            } else if (bus2 != null) {
                OpenBranchSide1ActiveFlowEquationTerm p2 = new OpenBranchSide1ActiveFlowEquationTerm(bc, bus2, equationContext);
                OpenBranchSide1ReactiveFlowEquationTerm q2 = new OpenBranchSide1ReactiveFlowEquationTerm(bc, bus2, equationContext);
                if (!networkContext.isSlackBus(bus2.getId())) {
                    equationTerms.add(p2);
                }
                if (!networkContext.isPvBus(bus2.getId())) {
                    equationTerms.add(q2);
                }
                variableUpdates.add(new OpenBranchSide1AcFlowUpdate(branch, p2, q2));
            }
        }

        for (ShuntCompensator sc : networkContext.getShuntCompensators()) {
            Bus bus = sc.getTerminal().getBusView().getBus();
            if (bus != null && !networkContext.isPvBus(bus.getId())) {
                ShuntCompensatorReactiveFlowEquationTerm q = new ShuntCompensatorReactiveFlowEquationTerm(sc, bus, networkContext, equationContext);
                equationTerms.add(q);
                variableUpdates.add(new ShuntCompensatorReactiveFlowUpdate(sc, q));
            }
        }

        return new EquationSystem(equationTerms, variableUpdates, networkContext);
    }
}
