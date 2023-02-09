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
    public void applyGenerators(Generator g, int busNum, boolean vregul, double targetV, double targetP, double targetQ,
            double p, double q) {
        double vb = g.getTerminal().getVoltageLevel().getNominalV();
        g.setTargetV(targetV * vb);
    }

}
