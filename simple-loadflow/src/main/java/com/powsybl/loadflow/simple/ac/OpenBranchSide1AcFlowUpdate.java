/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.equations.VariableUpdate;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class OpenBranchSide1AcFlowUpdate implements VariableUpdate {

    private final OpenBranchSide1AcContext branchContext;

    public OpenBranchSide1AcFlowUpdate(OpenBranchSide1AcContext branchContext) {
        this.branchContext = Objects.requireNonNull(branchContext);
    }

    @Override
    public void update(double[] x) {
        branchContext.getBc().getBranch().getTerminal1().setP(Double.NaN);
        branchContext.getBc().getBranch().getTerminal1().setQ(Double.NaN);
        branchContext.getBc().getBranch().getTerminal2().setP(branchContext.p2(x));
        branchContext.getBc().getBranch().getTerminal2().setQ(branchContext.q2(x));
    }
}
