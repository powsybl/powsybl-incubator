/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.api.Branch;
import com.powsybl.loadflow.simple.equations.VariableUpdate;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class OpenBranchSide2AcFlowUpdate implements VariableUpdate {

    private final Branch branch;

    private final OpenBranchSide2ActiveFlowEquationTerm p1;

    private final OpenBranchSide2ReactiveFlowEquationTerm q1;

    public OpenBranchSide2AcFlowUpdate(Branch branch, OpenBranchSide2ActiveFlowEquationTerm p1, OpenBranchSide2ReactiveFlowEquationTerm q1) {
        this.branch = Objects.requireNonNull(branch);
        this.p1 = Objects.requireNonNull(p1);
        this.q1 = Objects.requireNonNull(q1);
    }

    @Override
    public void update(double[] x) {
        branch.getTerminal1().setP(p1.eval(x));
        branch.getTerminal1().setQ(q1.eval(x));
        branch.getTerminal2().setP(Double.NaN);
        branch.getTerminal2().setQ(Double.NaN);
    }
}
