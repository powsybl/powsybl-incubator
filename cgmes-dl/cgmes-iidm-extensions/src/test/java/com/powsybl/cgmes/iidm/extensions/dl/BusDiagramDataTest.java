/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.dl;

import org.junit.Test;

import com.powsybl.cgmes.iidm.Networks;
import com.powsybl.iidm.api.Bus;
import com.powsybl.iidm.api.Network;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class BusDiagramDataTest extends AbstractNodeDiagramDataTest {

    @Test
    public void test() {
        Network network = Networks.createNetworkWithBus();
        Bus bus = network.getVoltageLevel("VoltageLevel").getBusBreakerView().getBus("Bus");

        NodeDiagramData<Bus> busDiagramData = new NodeDiagramData<>(bus);
        busDiagramData.setPoint1(new DiagramPoint(0, 10, 1));
        busDiagramData.setPoint2(new DiagramPoint(10, 0, 2));
        bus.addExtension(NodeDiagramData.class, busDiagramData);

        Bus bus2 = network.getVoltageLevel("VoltageLevel").getBusBreakerView().getBus("Bus");
        NodeDiagramData<Bus> busDiagramData2 = bus2.getExtension(NodeDiagramData.class);

        checkDiagramData(busDiagramData2);
    }

}
