/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.dl;

import org.junit.Test;

import com.powsybl.cgmes.iidm.Networks;
import com.powsybl.iidm.api.Line;
import com.powsybl.iidm.api.Network;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class LineDiagramDataTest extends AbstractLineDiagramDataTest {

    @Test
    public void test() {
        Network network = Networks.createNetworkWithLine();
        Line line = network.getLine("Line");

        LineDiagramData<Line> lineDiagramData = new LineDiagramData<>(line);
        lineDiagramData.addPoint(new DiagramPoint(10, 0, 2));
        lineDiagramData.addPoint(new DiagramPoint(0, 10, 1));
        line.addExtension(LineDiagramData.class, lineDiagramData);

        Line line2 = network.getLine("Line");
        LineDiagramData<Line> lineDiagramData2 = line2.getExtension(LineDiagramData.class);

        checkDiagramData(lineDiagramData2);
    }

}
