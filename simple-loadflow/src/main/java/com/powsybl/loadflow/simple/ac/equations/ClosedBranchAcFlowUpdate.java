/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.network.Branch;
import com.powsybl.loadflow.simple.equations.VariableUpdate;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ClosedBranchAcFlowUpdate implements VariableUpdate {

    private final Branch branch;

    private final ClosedBranchSide1ActiveFlowEquationTerm p1;

    private final ClosedBranchSide1ReactiveFlowEquationTerm q1;

    private final ClosedBranchSide2ActiveFlowEquationTerm p2;

    private final ClosedBranchSide2ReactiveFlowEquationTerm q2;

    public ClosedBranchAcFlowUpdate(Branch branch,
                                    ClosedBranchSide1ActiveFlowEquationTerm p1, ClosedBranchSide1ReactiveFlowEquationTerm q1,
                                    ClosedBranchSide2ActiveFlowEquationTerm p2, ClosedBranchSide2ReactiveFlowEquationTerm q2) {
        this.branch = Objects.requireNonNull(branch);
        this.p1 = Objects.requireNonNull(p1);
        this.q1 = Objects.requireNonNull(q1);
        this.p2 = Objects.requireNonNull(p2);
        this.q2 = Objects.requireNonNull(q2);
    }

    @Override
    public void update() {
        branch.getTerminal1().setP(p1.eval());
        branch.getTerminal1().setQ(q1.eval());
        branch.getTerminal2().setP(p2.eval());
        branch.getTerminal2().setQ(q2.eval());
    }
}
