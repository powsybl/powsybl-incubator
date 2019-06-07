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

    public LfBranchImpl(Branch branch, LfBus bus1, LfBus bus2) {
        super(bus1, bus2);
        this.branch = Objects.requireNonNull(branch);
        init();
    }

    private void init() {
        if (branch instanceof Line) {
            initLine((Line) branch);
        } else if (branch instanceof TwoWindingsTransformer) {
            initTransformer((TwoWindingsTransformer) branch);
        } else {
            throw new PowsyblException("Unsupported type of branch for flow equations for branch: " + branch.getId());
        }

        double z = Math.hypot(r, x);
        y = 1 / z;
        ksi = Math.atan2(r, x);
    }

    private void initLine(Line line) {
        r = line.getR();
        x = line.getX();
        g1 = line.getG1();
        g2 = line.getG2();
        b1 = line.getB1();
        b2 = line.getB2();
    }

    private void initTransformer(TwoWindingsTransformer tf) {
        r = Transformers.getR(tf);
        x = Transformers.getX(tf);
        g1 = Transformers.getG1(tf);
        b1 = Transformers.getB1(tf);
        r1 = Transformers.getRatio(tf);
        a1 = Transformers.getAngle(tf);
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
