/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.*;

import java.util.List;
import java.util.Objects;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class VoltageLevelsArea extends AbstractNetworkArea {

    private final String name;

    private final List<VoltageLevel> areaVoltageLevels;

    public VoltageLevelsArea(String name, List<VoltageLevel> areaVoltageLevels) {
        this.name = Objects.requireNonNull(name);
        this.areaVoltageLevels = Objects.requireNonNull(areaVoltageLevels);
    }

    @Override
    public List<VoltageLevel> getAreaVoltageLevels(Network network) {
        return areaVoltageLevels;
    }

    @Override
    public String getName() {
        return name;
    }
}
