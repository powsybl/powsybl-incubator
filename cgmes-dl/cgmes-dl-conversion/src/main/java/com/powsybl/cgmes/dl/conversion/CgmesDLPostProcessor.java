/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.ImportPostProcessor;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
@AutoService(ImportPostProcessor.class)
public class CgmesDLPostProcessor implements ImportPostProcessor {

    private static final String NAME = "cgmesDLImport";
    private static final Logger LOG = LoggerFactory.getLogger(CgmesDLPostProcessor.class);

    private final QueryCatalog queryCatalog;

    CgmesDLPostProcessor(QueryCatalog queryCatalog) {
        this.queryCatalog = Objects.requireNonNull(queryCatalog);
    }

    public CgmesDLPostProcessor() {
        this(new QueryCatalog("CGMES-DL.sparql"));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void process(Network network, ComputationManager computationManager) throws Exception {
        CgmesModelExtension extension = network.getExtension(CgmesModelExtension.class);
        if (extension != null) {
            LOG.info("Execute {} post processor on network {}", getName(), network.getId());
            CgmesModel cgmesModel = extension.getCgmesModel();
            if (cgmesModel instanceof CgmesModelTripleStore) {
                TripleStore tripleStore = ((CgmesModelTripleStore) cgmesModel).tripleStore();
                CgmesDLModel cgmesDLModel = new CgmesDLModel(tripleStore, queryCatalog);
                new CgmesDLImporter(network, cgmesDLModel).importDLData();
            } else {
                LOG.warn("Cannot run {} post processor on network {}: CGMES model not based on triple store", getName(), network.getId());
            }
        }
    }

}
