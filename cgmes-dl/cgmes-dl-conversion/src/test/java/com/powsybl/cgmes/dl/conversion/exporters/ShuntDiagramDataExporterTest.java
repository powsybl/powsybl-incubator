/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion.exporters;

import org.junit.Before;
import org.mockito.Mockito;

import com.powsybl.cgmes.iidm.Networks;
import com.powsybl.cgmes.iidm.extensions.dl.InjectionDiagramData;
import com.powsybl.iidm.network.ShuntCompensator;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class ShuntDiagramDataExporterTest extends AbstractInjectionDiagramDataExporterTest {

    private ShuntCompensator shunt;

    @Before
    public void setUp() {
        super.setUp();

        network = Networks.createNetworkWithShuntCompensator();
        shunt = network.getShuntCompensator("Shunt");
        InjectionDiagramData<ShuntCompensator> shuntDiagramData = new InjectionDiagramData<>(shunt, point, rotation);
        shuntDiagramData.addTerminalPoint(terminalPoint1);
        shuntDiagramData.addTerminalPoint(terminalPoint2);
        shunt.addExtension(InjectionDiagramData.class, shuntDiagramData);

        Mockito.when(cgmesDLModel.getTerminals()).thenReturn(getTerminals(shunt.getId()));
    }

    @Override
    protected void checkStatements() {
        checkStatements(shunt.getId(), shunt.getName(), "bus-branch");
    }

}
