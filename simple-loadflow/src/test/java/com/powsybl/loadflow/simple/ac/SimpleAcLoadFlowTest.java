/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.DenseMatrixFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleAcLoadFlowTest {

    @Test
    public void tuto1() {
        Network network = EurostagTutorialExample1Factory.create();
        LoadFlowResult result = new SimpleAcLoadFlow(network, new DenseMatrixFactory())
                .run();
        assertTrue(result.isOk());

        Bus ngenBus = network.getBusBreakerView().getBusStream().filter(b -> b.getId().equals("NGEN")).findFirst().orElseThrow(AssertionError::new);
        Bus nhv1Bus = network.getBusBreakerView().getBusStream().filter(b -> b.getId().equals("NHV1")).findFirst().orElseThrow(AssertionError::new);
        Bus nhv2Bus = network.getBusBreakerView().getBusStream().filter(b -> b.getId().equals("NHV2")).findFirst().orElseThrow(AssertionError::new);
        Bus nloadBus = network.getBusBreakerView().getBusStream().filter(b -> b.getId().equals("NLOAD")).findFirst().orElseThrow(AssertionError::new);
        Line line1 = network.getLine("NHV1_NHV2_1");
        Line line2 = network.getLine("NHV1_NHV2_2");

        assertEquals(24.5, ngenBus.getV(), 1E-3d);
        assertEquals(0, ngenBus.getAngle(), 1E-6d);
        assertEquals(402.143, nhv1Bus.getV(), 1E-3d);
        assertEquals(-2.325965, nhv1Bus.getAngle(), 1E-6d);
        assertEquals(389.953, nhv2Bus.getV(), 1E-3d);
        assertEquals(-5.832323, nhv2Bus.getAngle(), 1E-6d);
        assertEquals(147.578, nloadBus.getV(), 1E-3d);
        assertEquals(-11.940451, nloadBus.getAngle(), 1E-6d);
        assertEquals(302.444, line1.getTerminal1().getP(), 1E-3d);
        assertEquals(98.74, line1.getTerminal1().getQ(), 1E-3d);
        assertEquals(-300.434, line1.getTerminal2().getP(), 1E-3d);
        assertEquals(-137.188, line1.getTerminal2().getQ(), 1E-3d);
        assertEquals(302.444, line2.getTerminal1().getP(), 1E-3d);
        assertEquals(98.74, line2.getTerminal1().getQ(), 1E-3d);
        assertEquals(-300.434, line2.getTerminal2().getP(), 1E-3d);
        assertEquals(-137.188, line2.getTerminal2().getQ(), 1E-3d);
    }
}
