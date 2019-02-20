/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.dl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public abstract class AbstractCouplingDeviseDiagramDataTest {

    protected <T> void checkDiagramData(CouplingDeviseDiagramData<?> diagramData) {
        assertNotNull(diagramData);
        assertEquals(0, diagramData.getPoint().getSeq(), 0);
        assertEquals(20, diagramData.getPoint().getX(), 0);
        assertEquals(10, diagramData.getPoint().getY(), 0);
        assertEquals(90, diagramData.getRotation(), 0);
        assertEquals(1, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL1).get(0).getSeq(), 0);
        assertEquals(0, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL1).get(0).getX(), 0);
        assertEquals(10, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL1).get(0).getY(), 0);
        assertEquals(2, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL1).get(1).getSeq(), 0);
        assertEquals(15, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL1).get(1).getX(), 0);
        assertEquals(10, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL1).get(1).getY(), 0);
        assertEquals(1, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL2).get(0).getSeq(), 0);
        assertEquals(25, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL2).get(0).getX(), 0);
        assertEquals(10, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL2).get(0).getY(), 0);
        assertEquals(2, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL2).get(1).getSeq(), 0);
        assertEquals(40, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL2).get(1).getX(), 0);
        assertEquals(10, diagramData.getTerminalPoints(DiagramTerminal.TERMINAL2).get(1).getY(), 0);
    }

}
