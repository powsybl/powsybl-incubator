/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion.exporters;

import com.powsybl.cgmes.iidm.extensions.dl.NetworkDiagramData;
import org.junit.Before;
import org.mockito.Mockito;

import com.powsybl.cgmes.iidm.Networks;
import com.powsybl.cgmes.iidm.extensions.dl.CouplingDeviceDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.DiagramTerminal;
import com.powsybl.iidm.network.Switch;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class SwitchDiagramDataExporterTest extends AbstractCouplingDeviceDiagramDataExporterTest {

    private Switch sw;

    @Before
    public void setUp() {
        super.setUp();

        network = Networks.createNetworkWithSwitch();
        sw = network.getSwitch("Switch");
        CouplingDeviceDiagramData<Switch> switchDiagramData = new CouplingDeviceDiagramData<>(sw);
        CouplingDeviceDiagramData.CouplingDeviceDiagramDetails details = switchDiagramData.new CouplingDeviceDiagramDetails(point, rotation);
        details.addTerminalPoint(DiagramTerminal.TERMINAL1, terminal1Point1);
        details.addTerminalPoint(DiagramTerminal.TERMINAL1, terminal1Point2);
        details.addTerminalPoint(DiagramTerminal.TERMINAL2, terminal2Point1);
        details.addTerminalPoint(DiagramTerminal.TERMINAL2, terminal2Point2);
        switchDiagramData.addData(basename, details);
        sw.addExtension(CouplingDeviceDiagramData.class, switchDiagramData);
        NetworkDiagramData.addDiagramName(network, basename);

        Mockito.when(cgmesDLModel.getTerminals()).thenReturn(getTerminals(sw.getId()));
    }

    @Override
    protected void checkStatements() {
        checkStatements(sw.getId(), sw.getName(), "bus-branch");
    }

}
