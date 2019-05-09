/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Network;
import com.powsybl.math.matrix.MatrixFactory;
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
        SimpleAcLoadFlow loadFlow = new SimpleAcLoadFlow(network, matrixFactory);
        assertEquals("Simple loadflow", loadFlow.getName());
        assertEquals("1.0", loadFlow.getVersion());
    }
}
