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
public abstract class AbstractLineDiagramDataTest {

    protected <T> void checkDiagramData(LineDiagramData<?> diagramData) {
        assertNotNull(diagramData);
        assertEquals(1, diagramData.getPoints().get(0).getSeq(), 0);
        assertEquals(0, diagramData.getPoints().get(0).getX(), 0);
        assertEquals(10, diagramData.getPoints().get(0).getY(), 0);
        assertEquals(2, diagramData.getPoints().get(1).getSeq(), 0);
        assertEquals(10, diagramData.getPoints().get(1).getX(), 0);
        assertEquals(0, diagramData.getPoints().get(1).getY(), 0);
        assertEquals(1, diagramData.getFirstPoint().getSeq(), 0);
        assertEquals(0, diagramData.getFirstPoint().getX(), 0);
        assertEquals(10, diagramData.getFirstPoint().getY(), 0);
        assertEquals(2, diagramData.getLastPoint().getSeq(), 0);
        assertEquals(10, diagramData.getLastPoint().getX(), 0);
        assertEquals(0, diagramData.getLastPoint().getY(), 0);
        assertEquals(1, diagramData.getFirstPoint(5).getSeq(), 0);
        assertEquals(3.535, diagramData.getFirstPoint(5).getX(), .001);
        assertEquals(6.464, diagramData.getFirstPoint(5).getY(), .001);
        assertEquals(2, diagramData.getLastPoint(5).getSeq(), 0);
        assertEquals(6.464, diagramData.getLastPoint(5).getX(), .001);
        assertEquals(3.535, diagramData.getLastPoint(5).getY(), .001);
    }

}
