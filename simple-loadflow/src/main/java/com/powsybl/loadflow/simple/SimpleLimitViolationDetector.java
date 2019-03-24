/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.CurrentLimits;
import com.powsybl.security.AbstractLimitViolationDetector;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;

import java.util.function.Consumer;

/**
 * Workaround for limit detection in Mw.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleLimitViolationDetector extends AbstractLimitViolationDetector {

    private static final double COS_PHI = 1d;

    @Override
    public void checkCurrent(Branch branch, Branch.Side side, Consumer<LimitViolation> consumer) {
        checkCurrent(branch, side, branch.getTerminal(side).getP(), consumer);
    }

    @Override
    public void checkCurrent(Branch branch, Consumer<LimitViolation> consumer) {
        checkCurrent(branch, Branch.Side.ONE, consumer);
    }

    @Override
    public void checkCurrent(Branch branch, Branch.Side side, double currentValue, Consumer<LimitViolation> consumer) {
        CurrentLimits currentLimits = branch.getCurrentLimits(side);
        if (currentLimits != null) {
            double permanentLimitA = currentLimits.getPermanentLimit();
            double permanentLimitMw = Math.sqrt(3) * branch.getTerminal(side).getVoltageLevel().getNominalV() * permanentLimitA * COS_PHI / Math.pow(10, 3);
            if (currentValue >= permanentLimitMw) {
                consumer.accept(new LimitViolation(branch.getId(),
                        branch.getName(),
                        LimitViolationType.CURRENT,
                        null,
                        Integer.MAX_VALUE,
                        permanentLimitMw,
                        1f,
                        currentValue,
                        side));
            }
        }
    }

    @Override
    public void checkVoltage(Bus bus, double voltageValue, Consumer<LimitViolation> consumer) {
        // nothing to do
    }
}
