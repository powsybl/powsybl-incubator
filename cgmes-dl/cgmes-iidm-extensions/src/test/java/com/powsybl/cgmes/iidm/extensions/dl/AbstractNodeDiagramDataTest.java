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
public abstract class AbstractNodeDiagramDataTest {

    protected <T> void checkDiagramData(NodeDiagramData<?> diagramData) {
        assertNotNull(diagramData);
        assertEquals(1, diagramData.getPoint1().getSeq(), 0);
        assertEquals(0, diagramData.getPoint1().getX(), 0);
        assertEquals(10, diagramData.getPoint1().getY(), 0);
        assertEquals(2, diagramData.getPoint2().getSeq(), 0);
        assertEquals(10, diagramData.getPoint2().getX(), 0);
        assertEquals(0, diagramData.getPoint2().getY(), 0);
    }

}
