/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit.cgmes;

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.conversion.CgmesImportPostProcessor;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuitAdder;
import com.powsybl.incubator.simulator.util.extensions.iidm.GeneratorShortCircuitAdder2;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
@AutoService(CgmesImportPostProcessor.class)
public class CgmesShortCircuitImportPostProcessor implements CgmesImportPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CgmesShortCircuitImportPostProcessor.class);

    public static final String NAME = "ShortCircuit";

    @Override
    public String getName() {
        return NAME;
    }

    private final QueryCatalog queryCatalog = new QueryCatalog("short-circuit.sparql");

    private void processSynchronousMachines(Network network, TripleStore tripleStore) {
        for (PropertyBag propertyBag : tripleStore.query(queryCatalog.get("synchronousMachineShortCircuit"))) {
            String id = propertyBag.getId("ID");
            double r0 = propertyBag.asDouble("r0");
            double r2 = propertyBag.asDouble("r2");
            double x0 = propertyBag.asDouble("r0");
            double x2 = propertyBag.asDouble("r2");
            Generator generator = network.getGenerator(id);
            double directTransX = x2; // FIXME
            generator.newExtension(GeneratorShortCircuitAdder.class)
                    .withDirectTransX(x2)
                    .add();
            generator.newExtension(GeneratorShortCircuitAdder2.class)
                    .add();
        }
    }

    @Override
    public void process(Network network, TripleStore tripleStore) {
        LOGGER.info("Loading CGMES short circuit data...");
        processSynchronousMachines(network, tripleStore);
    }
}
