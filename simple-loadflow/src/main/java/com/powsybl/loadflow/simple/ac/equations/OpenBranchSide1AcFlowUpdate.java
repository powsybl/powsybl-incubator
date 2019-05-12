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
public class OpenBranchSide1AcFlowUpdate implements VariableUpdate {

    private final Branch branch;

    private final OpenBranchSide1ActiveFlowEquationTerm p2;

    private final OpenBranchSide1ReactiveFlowEquationTerm q2;

    public OpenBranchSide1AcFlowUpdate(Branch branch, OpenBranchSide1ActiveFlowEquationTerm p2, OpenBranchSide1ReactiveFlowEquationTerm q2) {
        this.branch = Objects.requireNonNull(branch);
        this.p2 = Objects.requireNonNull(p2);
        this.q2 = Objects.requireNonNull(q2);
    }

    @Override
    public void update(double[] x) {
        branch.getTerminal1().setP(Double.NaN);
        branch.getTerminal1().setQ(Double.NaN);
        branch.getTerminal2().setP(p2.eval(x));
        branch.getTerminal2().setQ(q2.eval(x));
    }
}
