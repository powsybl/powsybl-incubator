/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;

/**
 * <p>2 bus test network:</p>
 *<pre>
 *      0 MW, 0 MVar
 *   1 ===== 1pu
 *       |
 *       |
 *       | 0.1j
 *       |
 *       |
 *   2 ===== 1pu
 *      200 MW, 100 MVar
 *</pre>
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TwoBusNetworkFactory extends AbstractLoadFlowNetworkFactory {

    public static Network create() {
        Network network = NetworkFactory.create("2-bus", "code");
        Bus b1 = createBus(network, "b1");
        Bus b2 = createBus(network, "b2");
        createGenerator(b1, "g1", 0, 1);
        createLoad(b2, "l1", 2, 1);
        createLine(network, b1, b2, "l12", 0.1f);
        return network;
    }
}
