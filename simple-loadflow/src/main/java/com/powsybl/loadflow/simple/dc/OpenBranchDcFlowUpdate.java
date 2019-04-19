/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc;

import com.powsybl.iidm.network.Branch;
import com.powsybl.loadflow.simple.equations.VariableUpdate;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class OpenBranchDcFlowUpdate implements VariableUpdate {

    private final Branch branch;

    public OpenBranchDcFlowUpdate(Branch branch) {
        this.branch = Objects.requireNonNull(branch);
    }

    @Override
    public void update(double[] x) {
        branch.getTerminal1().setP(0);
        branch.getTerminal2().setP(0);
    }
}
