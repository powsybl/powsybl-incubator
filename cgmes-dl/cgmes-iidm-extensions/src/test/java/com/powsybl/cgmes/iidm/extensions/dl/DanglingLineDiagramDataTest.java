/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.dl;

import org.junit.Test;

import com.powsybl.cgmes.iidm.Networks;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class DanglingLineDiagramDataTest extends AbstractLineDiagramDataTest {

    @Test
    public void test() {
        Network network = Networks.createNetworkWithDanglingLine();
        DanglingLine danglingLine = network.getDanglingLine("DanglingLine");

        LineDiagramData<DanglingLine> danglingLineDiagramData = new LineDiagramData<>(danglingLine);
        danglingLineDiagramData.addPoint(new DiagramPoint(10, 0, 2));
        danglingLineDiagramData.addPoint(new DiagramPoint(0, 10, 1));
        danglingLine.addExtension(LineDiagramData.class, danglingLineDiagramData);

        DanglingLine danglingLine2 = network.getDanglingLine("DanglingLine");
        LineDiagramData<DanglingLine> danglingLineDiagramData2 = danglingLine2.getExtension(LineDiagramData.class);

        checkDiagramData(danglingLineDiagramData2);
    }

}
