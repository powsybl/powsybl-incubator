/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.*;

import java.util.List;

/**
 * @author Ameni Walha <ameni.walha at rte-france.com>
 */
public class VoltageLevelsArea extends AbstractNetworkArea {

    private final List<VoltageLevel> areaVoltageLevels;

    public VoltageLevelsArea(List<VoltageLevel> areaVoltageLevels) {
        this.areaVoltageLevels = areaVoltageLevels;
    }

    @Override
    public List<VoltageLevel> getAreaVoltageLevels(Network network) {
        return areaVoltageLevels;
    }

    @Override
    public String getName() {
        StringBuilder sbld = new StringBuilder("VoltageLevelsArea{ ");
        for (VoltageLevel v : areaVoltageLevels) {
            sbld.append(v.getName());
            sbld.append(", ");
        }
        sbld.append("}");
        return sbld.toString();
    }
}
