/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.tools.PowsyblCoreVersion;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleLoadFlowTest {

    @Test
    public void metaInfoTest() {
        Network network = Mockito.mock(Network.class);
        MatrixFactory matrixFactory = Mockito.mock(MatrixFactory.class);
        LoadFlow loadFlow = new SimpleLoadFlowFactory().create(network, null, 0);
        assertTrue(loadFlow instanceof SimpleLoadFlow);
        assertEquals("SimpleLoadflow", loadFlow.getName());
        assertEquals(new PowsyblCoreVersion().getMavenProjectVersion(), loadFlow.getVersion());
    }

    @Test
    public void constructionMethodsTest() {
        Network network = Mockito.mock(Network.class);

        // Factory
        LoadFlow loadFlow1 = new SimpleLoadFlowFactory().create(network, null, 0);
        assertNotNull(loadFlow1);
        assertTrue(loadFlow1 instanceof SimpleLoadFlow);

        // Factory with MatrixFactory
        LoadFlow loadFlow2 = new SimpleLoadFlowFactory().create(network, null, 0);
        assertNotNull(loadFlow2);
        assertTrue(loadFlow2 instanceof SimpleLoadFlow);

        // Constructor with Network
        LoadFlow loadFlow3 = new SimpleLoadFlow(network);
        assertNotNull(loadFlow3);

        // Constructor with Network and MatrixFactory
        LoadFlow loadFlow4 = new SimpleLoadFlow(network);
        assertNotNull(loadFlow4);

        // Static factory method
        LoadFlow loadFlow5 = SimpleLoadFlow.create(network);
        assertNotNull(loadFlow5);
    }
}
