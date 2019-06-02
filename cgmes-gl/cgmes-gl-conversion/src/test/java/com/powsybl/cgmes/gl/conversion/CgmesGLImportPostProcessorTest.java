/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.gl.conversion;

import org.junit.Test;

import com.powsybl.cgmes.conversion.Profiling;
import com.powsybl.cgmes.iidm.extensions.gl.GLTestUtils;
import com.powsybl.iidm.network.Network;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesGLImportPostProcessorTest extends CgmesGLModelTest  {

    @Test
    public void process() {
        Network network = GLTestUtils.getNetwork();
        new CgmesGLImportPostProcessor(queryCatalog).process(network, tripleStore, new Profiling());
        GLTestUtils.checkNetwork(network);
    }

}
