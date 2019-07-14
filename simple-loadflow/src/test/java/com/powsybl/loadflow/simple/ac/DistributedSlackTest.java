/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.simple.network.DistributedSlackNetworkFactory;
import com.powsybl.math.matrix.DenseMatrixFactory;
import org.junit.Test;

import static com.powsybl.loadflow.simple.util.LoadFlowAssert.DELTA_POWER;
import static com.powsybl.loadflow.simple.util.LoadFlowAssert.assertActivePowerEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DistributedSlackTest {

    @Test
    public void test() {
        Network network = DistributedSlackNetworkFactory.create();
        SimpleAcLoadFlow loadFlow = new SimpleAcLoadFlow(network, new DenseMatrixFactory());
        LoadFlowParameters parameters = new LoadFlowParameters();
        SimpleAcLoadFlowParameters parametersExt = new SimpleAcLoadFlowParameters()
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED)
                .setDistributedSlack(true);
        parameters.addExtension(SimpleAcLoadFlowParameters.class, parametersExt);

        LoadFlowResult result = loadFlow.run(VariantManagerConstants.INITIAL_VARIANT_ID, parameters).join();
        assertTrue(result.isOk());
        Generator g1 = network.getGenerator("g1");
        Generator g2 = network.getGenerator("g2");
        Generator g3 = network.getGenerator("g3");
        Generator g4 = network.getGenerator("g4");
        assertEquals(148, g1.getTargetP(), DELTA_POWER);
        assertEquals(224, g2.getTargetP(), DELTA_POWER);
        assertEquals(126, g3.getTargetP(), DELTA_POWER);
        assertEquals(102, g4.getTargetP(), DELTA_POWER);
        Line l14 = network.getLine("l14");
        Line l24 = network.getLine("l24");
        Line l34 = network.getLine("l34");
        assertActivePowerEquals(148, l14.getTerminal1());
        assertActivePowerEquals(-148, l14.getTerminal2());
        assertActivePowerEquals(224, l24.getTerminal1());
        assertActivePowerEquals(-224, l24.getTerminal2());
        assertActivePowerEquals(228, l34.getTerminal1());
        assertActivePowerEquals(-228, l34.getTerminal2());
    }
}
