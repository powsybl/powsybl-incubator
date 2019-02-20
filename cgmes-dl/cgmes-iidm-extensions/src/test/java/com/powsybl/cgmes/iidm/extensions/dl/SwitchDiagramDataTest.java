/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.dl;

import org.junit.Test;

import com.powsybl.cgmes.iidm.Networks;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class SwitchDiagramDataTest extends AbstractCouplingDeviseDiagramDataTest {

    @Test
    public void test() {
        Network network = Networks.createNetworkWithSwitch();
        Switch sw = network.getSwitch("Switch");

        CouplingDeviseDiagramData<Switch> switchDiagramData = new CouplingDeviseDiagramData<>(sw, new DiagramPoint(20, 10, 0), 90);
        switchDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(15, 10, 2));
        switchDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(0, 10, 1));
        switchDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(25, 10, 1));
        switchDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(40, 10, 2));
        sw.addExtension(CouplingDeviseDiagramData.class, switchDiagramData);

        Switch sw2 = network.getSwitch("Switch");
        CouplingDeviseDiagramData<Switch> switchDiagramData2 = sw2.getExtension(CouplingDeviseDiagramData.class);

        checkDiagramData(switchDiagramData2);
    }

}
