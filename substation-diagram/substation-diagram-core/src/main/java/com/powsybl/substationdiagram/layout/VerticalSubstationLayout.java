/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import com.powsybl.substationdiagram.model.Coord;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.SubstationGraph;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class VerticalSubstationLayout implements SubstationLayout {

    public VerticalSubstationLayout(SubstationGraph graph) { }

    /**
     * Calculate relative coordinate of voltageLevels in the substation
     */
    @Override
    public Coord run(LayoutParameters layoutParam, Graph vlGraph) {
        int maxV = vlGraph.getNodeBuses().stream()
                .mapToInt(nodeBus -> nodeBus.getPosition().getV() + nodeBus.getPosition().getVSpan())
                .max().orElse(0);

        double x = 0;
        double y = layoutParam.getInitialYBus() + layoutParam.getStackHeight() +
                layoutParam.getExternCellHeight() + layoutParam.getVerticalSpaceBus() * (maxV + 2);

        return new Coord(x, y);
    }
}
