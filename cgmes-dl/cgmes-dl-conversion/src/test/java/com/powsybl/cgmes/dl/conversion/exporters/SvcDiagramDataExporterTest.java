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
import com.powsybl.iidm.network.StaticVarCompensator;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class SvcDiagramDataExporterTest extends AbstractInjectionDiagramDataExporterTest {

    private StaticVarCompensator svc;

    @Before
    public void setUp() {
        super.setUp();

        network = Networks.createNetworkWithStaticVarCompensator();
        svc = network.getStaticVarCompensator("Svc");
        InjectionDiagramData<StaticVarCompensator> svcDiagramData = new InjectionDiagramData<>(svc, point, rotation);
        svcDiagramData.addTerminalPoint(terminalPoint1);
        svcDiagramData.addTerminalPoint(terminalPoint2);
        svc.addExtension(InjectionDiagramData.class, svcDiagramData);

        Mockito.when(cgmesDLModel.getTerminals()).thenReturn(getTerminals(svc.getId()));
    }

    @Override
    protected void checkStatements() {
        checkStatements(svc.getId(), svc.getName(), "bus-branch");
    }

}
