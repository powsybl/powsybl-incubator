/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.loadflow.simple.equations.VariableUpdate;
import com.powsybl.loadflow.simple.network.LfBranch;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class OpenBranchSide2AcFlowUpdate implements VariableUpdate {

    private final LfBranch branch;

    private final OpenBranchSide2ActiveFlowEquationTerm p1;

    private final OpenBranchSide2ReactiveFlowEquationTerm q1;

    public OpenBranchSide2AcFlowUpdate(LfBranch branch, OpenBranchSide2ActiveFlowEquationTerm p1, OpenBranchSide2ReactiveFlowEquationTerm q1) {
        this.branch = Objects.requireNonNull(branch);
        this.p1 = Objects.requireNonNull(p1);
        this.q1 = Objects.requireNonNull(q1);
    }

    @Override
    public void update() {
        branch.setP1(p1.eval());
        branch.setQ1(q1.eval());
        branch.setP2(Double.NaN);
        branch.setQ2(Double.NaN);
    }
}
