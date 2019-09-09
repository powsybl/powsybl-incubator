/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.simple.network.FirstSlackBusSelector;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.loadflow.simple.network.LfReactiveDiagram;
import com.powsybl.loadflow.simple.network.impl.LfNetworks;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class AcloadFlowReactiveLimitsTest {

    @Test
    public void test() {
        Network network = EurostagTutorialExample1Factory.createWithMoreGenerators();

        Generator gen = network.getGenerator("GEN");
        gen.newMinMaxReactiveLimits()
                .setMinQ(0)
                .setMaxQ(280)
                .add();

        List<LfNetwork> lfNetworks = LfNetworks.create(network, new FirstSlackBusSelector());
        assertEquals(1, lfNetworks.size());
        LfNetwork lfNetwork = lfNetworks.get(0);
        LfBus genBus = lfNetwork.getBus(0);
        assertEquals("VLGEN_0", genBus.getId());
        LfReactiveDiagram diagram = genBus.getReactiveDiagram().orElse(null);
        assertNotNull(diagram);
        assertEquals(0.06, diagram.getMinQ(0), 0);
        assertEquals(2.87, diagram.getMaxQ(0), 0.001);
    }
}
