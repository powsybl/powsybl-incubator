/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.simple.network.DistributedSlackNetworkFactory;
import com.powsybl.math.matrix.DenseMatrixFactory;
import org.junit.Test;

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
    }
}
