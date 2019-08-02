/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network.impl;

import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.loadflow.simple.util.Evaluable;
import com.powsybl.loadflow.simple.network.LfShunt;
import com.powsybl.loadflow.simple.network.PerUnit;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfShuntImpl implements LfShunt {

    private final ShuntCompensator shuntCompensator;

    private final double b;

    private Evaluable q = Evaluable.NAN;

    public LfShuntImpl(ShuntCompensator shuntCompensator) {
        this.shuntCompensator = Objects.requireNonNull(shuntCompensator);
        double nominalV = shuntCompensator.getTerminal().getVoltageLevel().getNominalV();
        double zb = nominalV * nominalV / PerUnit.SB;
        b = shuntCompensator.getCurrentB() * zb;
    }

    @Override
    public double getB() {
        return b;
    }

    @Override
    public void setQ(Evaluable q) {
        this.q = Objects.requireNonNull(q);
    }

    @Override
    public void updateState() {
        shuntCompensator.getTerminal().setQ(q.eval() * PerUnit.SB);
    }
}
