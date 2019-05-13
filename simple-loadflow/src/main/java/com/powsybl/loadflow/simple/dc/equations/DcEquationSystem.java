/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc.equations;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class DcEquationSystem {

    private DcEquationSystem() {
    }

    public static EquationSystem create(NetworkContext networkContext, EquationContext equationContext) {
        List<EquationTerm> equationTerms = new ArrayList<>();
        List<VariableUpdate> variableUpdates = new ArrayList<>();

        equationTerms.add(new BusPhaseEquationTerm(networkContext.getSlackBus(), equationContext));

        for (Branch branch : networkContext.getBranches()) {
            Bus bus1 = branch.getTerminal1().getBusView().getBus();
            Bus bus2 = branch.getTerminal2().getBusView().getBus();
            if (bus1 != null && bus2 != null) {
                BranchCharacteristics bc = new BranchCharacteristics(branch);
                ClosedBranchSide1DcFlowEquationTerm p1 = new ClosedBranchSide1DcFlowEquationTerm(bc, bus1, bus2, equationContext);
                ClosedBranchSide2DcFlowEquationTerm p2 = new ClosedBranchSide2DcFlowEquationTerm(bc, bus1, bus2, equationContext);
                if (!networkContext.isSlackBus(bus1.getId())) {
                    equationTerms.add(p1);
                }
                if (!networkContext.isSlackBus(bus2.getId())) {
                    equationTerms.add(p2);
                }
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
