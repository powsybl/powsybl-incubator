/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.opf;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.converter.DefaultNetworkApplier;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.*;

public class ReactiveOpfNetworkApplier extends DefaultNetworkApplier {

    public ReactiveOpfNetworkApplier(StringToIntMapper<AmplSubset> networkMapper) {
        super(networkMapper);
    }

    @Override
    public void applyShunt(ShuntCompensator sc, int busNum, double q, double b, int sections) {
        int nbSection = 0;
        while (nbSection <= sc.getMaximumSectionCount() && sc.getB(nbSection) < b) {
            ++nbSection;
        }
        if (nbSection == sc.getMaximumSectionCount()) {
            sc.setSectionCount(sc.getMaximumSectionCount());
        } else if (Math.abs(sc.getB(nbSection) - b) < Math.abs(sc.getB(nbSection - 1) - b)) {
            sc.setSectionCount(nbSection);
        } else {
            sc.setSectionCount(nbSection - 1);
        }
    }

}
