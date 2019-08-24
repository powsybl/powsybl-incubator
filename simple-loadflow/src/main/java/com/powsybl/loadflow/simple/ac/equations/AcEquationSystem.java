/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.loadflow.simple.equations.BusPhaseEquationTerm;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.equations.EquationType;
import com.powsybl.loadflow.simple.network.LfBranch;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.loadflow.simple.network.LfShunt;

import java.util.Objects;

import static com.powsybl.loadflow.simple.equations.EquationType.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class AcEquationSystem {

    private AcEquationSystem() {
    }

    public static EquationSystem create(LfNetwork network, EquationContext equationContext) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(equationContext);

        for (LfBus bus : network.getBuses()) {
            if (bus.isSlack()) {
                equationContext.getEquation(bus.getNum(), BUS_PHI).getTerms().add(new BusPhaseEquationTerm(bus, equationContext));
                equationContext.getEquation(bus.getNum(), BUS_P).setToSolve(false);
            }
            if (bus.hasVoltageControl()) {
                equationContext.getEquation(bus.getNum(), BUS_V).getTerms().add(new BusVoltageEquationTerm(bus, equationContext));
                equationContext.getEquation(bus.getNum(), BUS_Q).setToSolve(false);
            }
            for (LfShunt shunt : bus.getShunts()) {
                ShuntCompensatorReactiveFlowEquationTerm q = new ShuntCompensatorReactiveFlowEquationTerm(shunt, bus, network, equationContext);
                equationContext.getEquation(bus.getNum(), BUS_Q).getTerms().add(q);
                shunt.setQ(q);
            }
        }

        for (LfBranch branch : network.getBranches()) {
            LfBus bus1 = branch.getBus1();
            LfBus bus2 = branch.getBus2();
            if (bus1 != null && bus2 != null) {
                ClosedBranchSide1ActiveFlowEquationTerm p1 = new ClosedBranchSide1ActiveFlowEquationTerm(branch, bus1, bus2, equationContext);
                ClosedBranchSide1ReactiveFlowEquationTerm q1 = new ClosedBranchSide1ReactiveFlowEquationTerm(branch, bus1, bus2, equationContext);
                ClosedBranchSide2ActiveFlowEquationTerm p2 = new ClosedBranchSide2ActiveFlowEquationTerm(branch, bus1, bus2, equationContext);
                ClosedBranchSide2ReactiveFlowEquationTerm q2 = new ClosedBranchSide2ReactiveFlowEquationTerm(branch, bus1, bus2, equationContext);
                equationContext.getEquation(bus1.getNum(), EquationType.BUS_P).getTerms().add(p1);
                equationContext.getEquation(bus1.getNum(), EquationType.BUS_Q).getTerms().add(q1);
                equationContext.getEquation(bus2.getNum(), EquationType.BUS_P).getTerms().add(p2);
                equationContext.getEquation(bus2.getNum(), EquationType.BUS_Q).getTerms().add(q2);
                branch.setP1(p1);
                branch.setQ1(q1);
                branch.setP2(p2);
                branch.setQ2(q2);
            } else if (bus1 != null) {
                OpenBranchSide2ActiveFlowEquationTerm p1 = new OpenBranchSide2ActiveFlowEquationTerm(branch, bus1, equationContext);
                OpenBranchSide2ReactiveFlowEquationTerm q1 = new OpenBranchSide2ReactiveFlowEquationTerm(branch, bus1, equationContext);
                equationContext.getEquation(bus1.getNum(), EquationType.BUS_P).getTerms().add(p1);
                equationContext.getEquation(bus1.getNum(), EquationType.BUS_Q).getTerms().add(q1);
                branch.setP1(p1);
                branch.setQ1(q1);
            } else if (bus2 != null) {
                OpenBranchSide1ActiveFlowEquationTerm p2 = new OpenBranchSide1ActiveFlowEquationTerm(branch, bus2, equationContext);
                OpenBranchSide1ReactiveFlowEquationTerm q2 = new OpenBranchSide1ReactiveFlowEquationTerm(branch, bus2, equationContext);
                equationContext.getEquation(bus2.getNum(), EquationType.BUS_P).getTerms().add(p2);
                equationContext.getEquation(bus2.getNum(), EquationType.BUS_Q).getTerms().add(q2);
                branch.setP2(p2);
                branch.setQ2(q2);
            }
        }

        return new EquationSystem(equationContext.getEquations(), network);
    }
}
