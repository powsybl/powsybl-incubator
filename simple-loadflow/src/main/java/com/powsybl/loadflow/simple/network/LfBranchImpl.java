/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfBranchImpl extends AbstractLfBranch {

    private final Branch branch;

    protected LfBranchImpl(LfBus bus1, LfBus bus2, double r, double x, double g1, double g2, double b1, double b2,
                         double r1, double r2, double a1, double a2, Branch branch) {
        super(bus1, bus2, r, x, g1, g2, b1, b2, r1, r2, a1, a2);
        this.branch = branch;
    }

    public static LfBranchImpl create(Branch branch, LfBus bus1, LfBus bus2) {
        Objects.requireNonNull(branch);
        if (branch instanceof Line) {
            Line line = (Line) branch;
            return new LfBranchImpl(bus1, bus2, line.getR(), line.getX(), line.getG1(), line.getG2(),
                    line.getB1(), line.getB2(), 1, 1, 0, 0, line);
        } else if (branch instanceof TwoWindingsTransformer) {
            TwoWindingsTransformer twt = (TwoWindingsTransformer) branch;
            return new LfBranchImpl(bus1, bus2, Transformers.getR(twt), Transformers.getX(twt), Transformers.getG1(twt),
                    0, Transformers.getB1(twt), 0, Transformers.getRatio(twt), 1, Transformers.getAngle(twt), 0, twt);
        } else {
            throw new PowsyblException("Unsupported type of branch for flow equations for branch: " + branch.getId());
        }
    }

    @Override
    public void setP1(double p1) {
        branch.getTerminal1().setP(p1);
    }

    @Override
    public void setP2(double p2) {
        branch.getTerminal2().setP(p2);
    }

    @Override
    public void setQ1(double q1) {
        branch.getTerminal1().setQ(q1);
    }

    @Override
    public void setQ2(double q2) {
        branch.getTerminal2().setQ(q2);
    }
}
