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
import com.powsybl.iidm.network.Load;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class LoadDiagramDataExporterTest extends AbstractInjectionDiagramDataExporterTest {

    private Load load;

    @Before
    public void setUp() {
        super.setUp();

        network = Networks.createNetworkWithLoad();
        load = network.getLoad("Load");
        InjectionDiagramData<Load> loadDiagramData = new InjectionDiagramData<>(load, point, rotation);
        loadDiagramData.addTerminalPoint(terminalPoint1);
        loadDiagramData.addTerminalPoint(terminalPoint2);
        load.addExtension(InjectionDiagramData.class, loadDiagramData);

        Mockito.when(cgmesDLModel.getTerminals()).thenReturn(getTerminals(load.getId()));
    }

    @Override
    protected void checkStatements() {
        checkStatements(load.getId(), load.getName(), "bus-branch");
    }

}
