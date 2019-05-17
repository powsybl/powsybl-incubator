/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.dl;

import org.junit.Test;

import com.powsybl.cgmes.iidm.Networks;
import com.powsybl.iidm.api.Network;
import com.powsybl.iidm.api.TwoWindingsTransformer;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class TransformerDiagramDataTest extends AbstractCouplingDeviceDiagramDataTest {

    @Test
    public void test() {
        Network network = Networks.createNetworkWithTwoWindingsTransformer();
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer("Transformer");

        CouplingDeviceDiagramData<TwoWindingsTransformer> twtDiagramData = new CouplingDeviceDiagramData<>(twt, new DiagramPoint(20, 10, 0), 90);
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(15, 10, 2));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL1, new DiagramPoint(0, 10, 1));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(25, 10, 1));
        twtDiagramData.addTerminalPoint(DiagramTerminal.TERMINAL2, new DiagramPoint(40, 10, 2));
        twt.addExtension(CouplingDeviceDiagramData.class, twtDiagramData);

        TwoWindingsTransformer twt2 = network.getTwoWindingsTransformer("Transformer");
        CouplingDeviceDiagramData<TwoWindingsTransformer> twtDiagramData2 = twt2.getExtension(CouplingDeviceDiagramData.class);

        checkDiagramData(twtDiagramData2);
    }

}
