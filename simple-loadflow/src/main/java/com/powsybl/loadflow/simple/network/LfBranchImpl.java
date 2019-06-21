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

    protected LfBranchImpl(LfBus bus1, LfBus bus2, PiModel piModel, Branch branch) {
        super(bus1, bus2, piModel, branch.getTerminal1().getVoltageLevel().getNominalV(), branch.getTerminal2().getVoltageLevel().getNominalV());
        this.branch = branch;
    }

    public static LfBranchImpl create(Branch branch, LfBus bus1, LfBus bus2) {
        Objects.requireNonNull(branch);
        PiModel piModel;
        if (branch instanceof Line) {
            Line line = (Line) branch;
            piModel = new PiModel(line.getR(), line.getX())
                    .setG1(line.getG1())
                    .setG2(line.getG2())
                    .setB1(line.getB1())
                    .setB2(line.getB2());
        } else if (branch instanceof TwoWindingsTransformer) {
            TwoWindingsTransformer twt = (TwoWindingsTransformer) branch;
            piModel = new PiModel(Transformers.getR(twt), Transformers.getX(twt))
                    .setG1(Transformers.getG1(twt))
                    .setB1(Transformers.getB1(twt))
                    .setR1(Transformers.getRatio(twt))
                    .setA1(Transformers.getAngle(twt));
        } else {
            throw new PowsyblException("Unsupported type of branch for flow equations for branch: " + branch.getId());
        }
        return new LfBranchImpl(bus1, bus2, piModel, branch);
    }

    @Override
    public void setP1(double p1) {
        branch.getTerminal1().setP(p1 * PerUnit.SB);
    }

    @Override
    public void setP2(double p2) {
        branch.getTerminal2().setP(p2 * PerUnit.SB);
    }

    @Override
    public void setQ1(double q1) {
        branch.getTerminal1().setQ(q1 * PerUnit.SB);
    }

    @Override
    public void setQ2(double q2) {
        branch.getTerminal2().setQ(q2 * PerUnit.SB);
    }
}
