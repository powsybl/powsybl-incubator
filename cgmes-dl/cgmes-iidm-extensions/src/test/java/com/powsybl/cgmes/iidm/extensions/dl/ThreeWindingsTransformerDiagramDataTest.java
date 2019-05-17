/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.dl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.powsybl.cgmes.iidm.Networks;
import com.powsybl.iidm.api.Network;
import com.powsybl.iidm.api.ThreeWindingsTransformer;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class ThreeWindingsTransformerDiagramDataTest {

    @Test
    public void test() {
        Network network = Networks.createNetworkWithThreeWindingsTransformer();
        ThreeWindingsTransformer twt = network.getThreeWindingsTransformer("Transformer3w");

        ThreeWindingsTransformerDiagramData twtDiagramData = new ThreeWindingsTransformerDiagramData(twt, new DiagramPoint(20, 13, 0), 90);
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(15, 10, 2));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(0, 10, 1));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(25, 10, 1));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(40, 10, 2));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL3, new DiagramPoint(20, 16, 1));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL3, new DiagramPoint(20, 30, 2));
        twt.addExtension(ThreeWindingsTransformerDiagramData.class, twtDiagramData);

        ThreeWindingsTransformer twt2 = network.getThreeWindingsTransformer("Transformer3w");
        ThreeWindingsTransformerDiagramData twtDiagramData2 = twt2.getExtension(ThreeWindingsTransformerDiagramData.class);

        assertNotNull(twtDiagramData2);
        assertEquals(0, twtDiagramData2.getPoint().getSeq(), 0);
        assertEquals(20, twtDiagramData2.getPoint().getX(), 0);
        assertEquals(13, twtDiagramData2.getPoint().getY(), 0);
        assertEquals(90, twtDiagramData2.getRotation(), 0);
        assertEquals(1, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL1).get(0).getSeq(), 0);
        assertEquals(0, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL1).get(0).getX(), 0);
        assertEquals(10, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL1).get(0).getY(), 0);
        assertEquals(2, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL1).get(1).getSeq(), 0);
        assertEquals(15, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL1).get(1).getX(), 0);
        assertEquals(10, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL1).get(1).getY(), 0);
        assertEquals(1, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL2).get(0).getSeq(), 0);
        assertEquals(25, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL2).get(0).getX(), 0);
        assertEquals(10, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL2).get(0).getY(), 0);
        assertEquals(2, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL2).get(1).getSeq(), 0);
        assertEquals(40, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL2).get(1).getX(), 0);
        assertEquals(10, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL2).get(1).getY(), 0);
        assertEquals(1, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL3).get(0).getSeq(), 0);
        assertEquals(20, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL3).get(0).getX(), 0);
        assertEquals(16, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL3).get(0).getY(), 0);
        assertEquals(2, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL3).get(1).getSeq(), 0);
        assertEquals(20, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL3).get(1).getX(), 0);
        assertEquals(30, twtDiagramData2.getTerminalPoints(DiagramTerminal.TERMINAL3).get(1).getY(), 0);
    }

}
