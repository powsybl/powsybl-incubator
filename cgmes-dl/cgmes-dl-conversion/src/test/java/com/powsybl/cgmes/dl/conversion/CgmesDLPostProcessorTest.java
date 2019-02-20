/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.Mockito;

import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.cgmes.iidm.Networks;
import com.powsybl.cgmes.iidm.extensions.dl.NodeDiagramData;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesDLPostProcessorTest extends CgmesDLModelTest {

    @Test
    public void process() throws Exception {
        Network network = Networks.createNetworkWithBusbar();
        CgmesModelTripleStore cgmesModel = Mockito.mock(CgmesModelTripleStore.class);
        Mockito.when(cgmesModel.tripleStore()).thenReturn(tripleStore);
        CgmesModelExtension extension = Mockito.mock(CgmesModelExtension.class);
        Mockito.when(extension.getCgmesModel()).thenReturn(cgmesModel);
        network.addExtension(CgmesModelExtension.class, extension);

        new CgmesDLPostProcessor(queryCatalog).process(network, null);

        BusbarSection busbar = network.getBusbarSection("Busbar");
        NodeDiagramData<BusbarSection> busDiagramData = busbar.getExtension(NodeDiagramData.class);
        assertNotNull(busDiagramData);
        assertEquals(1, busDiagramData.getPoint1().getSeq(), 0);
        assertEquals(20, busDiagramData.getPoint1().getX(), 0);
        assertEquals(5, busDiagramData.getPoint1().getY(), 0);
        assertEquals(2, busDiagramData.getPoint2().getSeq(), 0);
        assertEquals(20, busDiagramData.getPoint2().getX(), 0);
        assertEquals(40, busDiagramData.getPoint2().getY(), 0);
    }

}
