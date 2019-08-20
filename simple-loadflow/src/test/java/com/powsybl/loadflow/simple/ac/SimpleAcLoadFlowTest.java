/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

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
public class SimpleAcLoadFlowTest {

    @Test
    public void metaInfoTest() {
        Network network = Mockito.mock(Network.class);
        MatrixFactory matrixFactory = Mockito.mock(MatrixFactory.class);
        LoadFlow loadFlow = new SimpleAcLoadFlowFactory(matrixFactory).create(network, null, 0);
        assertTrue(loadFlow instanceof SimpleAcLoadFlow);
        assertEquals("Simple loadflow", loadFlow.getName());
        assertEquals(new PowsyblCoreVersion().getMavenProjectVersion(), loadFlow.getVersion());
    }

    @Test
    public void constructionMethodsTest() {
        Network network = Mockito.mock(Network.class);
        MatrixFactory matrixFactory = Mockito.mock(MatrixFactory.class);

        // Factory
        LoadFlow loadFlow1 = new SimpleAcLoadFlowFactory().create(network, null, 0);
        assertNotNull(loadFlow1);
        assertTrue(loadFlow1 instanceof SimpleAcLoadFlow);

        // Factory with MatrixFactory
        LoadFlow loadFlow2 = new SimpleAcLoadFlowFactory(matrixFactory).create(network, null, 0);
        assertNotNull(loadFlow2);
        assertTrue(loadFlow2 instanceof SimpleAcLoadFlow);

        // Constructor with Network
        LoadFlow loadFlow3 = new SimpleAcLoadFlow(network);
        assertNotNull(loadFlow3);

        // Constructor with Network and MatrixFactory
        LoadFlow loadFlow4 = new SimpleAcLoadFlow(network, matrixFactory);
        assertNotNull(loadFlow4);

        // Static factory method
        LoadFlow loadFlow5 = SimpleAcLoadFlow.create(network);
        assertNotNull(loadFlow5);
    }
}
