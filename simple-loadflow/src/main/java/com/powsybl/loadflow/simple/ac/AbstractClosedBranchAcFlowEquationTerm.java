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
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractClosedBranchAcFlowEquationTerm implements EquationTerm {

    protected final ClosedBranchAcContext branchContext;

    protected final Branch.Side side;

    protected final Equation equation;

    protected final List<Variable> variables = new ArrayList<>(4);

    protected AbstractClosedBranchAcFlowEquationTerm(ClosedBranchAcContext branchContext, Branch.Side side, Bus bus1, Bus bus2,
                                                     EquationType equationType, NetworkContext networkContext, EquationContext equationContext) {
        this.branchContext = Objects.requireNonNull(branchContext);
        this.side = Objects.requireNonNull(side);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);

        String busId1 = bus1.getId();
        String busId2 = bus2.getId();

        switch (side) {
            case ONE:
                equation = equationContext.getEquation(busId1, equationType);
                break;
            case TWO:
                equation = equationContext.getEquation(busId2, equationType);
                break;
            default:
                throw new IllegalStateException("Unknown side: " + side);
        }

        if (!networkContext.isPvBus(busId1)) {
            variables.add(branchContext.getV1Var());
        }
        if (!networkContext.isPvBus(busId2)) {
            variables.add(branchContext.getV2Var());
        }
        if (!networkContext.isSlackBus(busId1)) {
            variables.add(branchContext.getPh1Var());
        }
        if (!networkContext.isSlackBus(busId2)) {
            variables.add(branchContext.getPh2Var());
        }
    }

    @Override
    public Equation getEquation() {
        return equation;
    }

    @Override
    public List<Variable> getVariables() {
        return variables;
    }

    @Override
    public double evalRhs(Variable variable) {
        return 0;
    }
}
