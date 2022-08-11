/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit.cgmes;

import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class MiniGridTest {

    @Test
    void bcTest() {
        Properties parameters = new Properties();
        parameters.setProperty("iidm.import.cgmes.post-processors", CgmesShortCircuitImportPostProcessor.NAME);
        Network network = Importers.loadNetwork(CgmesConformity1Catalog.miniBusBranch().dataSource(), parameters);
        // TODO
    }
}
