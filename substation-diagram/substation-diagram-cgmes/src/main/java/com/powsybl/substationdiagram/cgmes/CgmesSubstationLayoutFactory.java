/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import com.powsybl.substationdiagram.layout.SubstationLayout;
import com.powsybl.substationdiagram.layout.SubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.model.SubstationGraph;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesSubstationLayoutFactory implements SubstationLayoutFactory {

    @Override
    public SubstationLayout create(SubstationGraph graph, VoltageLevelLayoutFactory vLayoutFactory) {
        return new CgmesSubstationLayout(graph);
    }

}
