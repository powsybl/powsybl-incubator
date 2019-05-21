/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc.equations;

import com.powsybl.loadflow.simple.equations.VariableUpdate;
import com.powsybl.loadflow.simple.network.LfBranch;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ClosedBranchDcFlowUpdate implements VariableUpdate {

    private final LfBranch branch;

    private final ClosedBranchSide1DcFlowEquationTerm p1;

    private final ClosedBranchSide2DcFlowEquationTerm p2;

    public ClosedBranchDcFlowUpdate(LfBranch branch, ClosedBranchSide1DcFlowEquationTerm p1, ClosedBranchSide2DcFlowEquationTerm p2) {
        this.branch = Objects.requireNonNull(branch);
        this.p1 = Objects.requireNonNull(p1);
        this.p2 = Objects.requireNonNull(p2);
    }

    @Override
    public void update() {
        branch.setP1(p1.eval());
        branch.setQ1(Double.NaN);
        branch.setP2(p2.eval());
        branch.setQ2(Double.NaN);
    }
}
