/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion.importers;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.iidm.extensions.dl.DiagramPoint;
import com.powsybl.cgmes.iidm.extensions.dl.InjectionDiagramData;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class LoadDiagramDataImporter extends AbstractInjectionDiagramDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(LoadDiagramDataImporter.class);

    public LoadDiagramDataImporter(Network network, Map<String, PropertyBags> terminalsDiagramData) {
        super(network, terminalsDiagramData);
    }

    public void importDiagramData(PropertyBag loadDiagramData) {
        Objects.requireNonNull(loadDiagramData);
        String loadId = loadDiagramData.getId("identifiedObject");
        Load load = network.getLoad(loadId);
        if (load != null) {
            InjectionDiagramData<Load> loadIidmDiagramData = new InjectionDiagramData<>(load,
                    new DiagramPoint(loadDiagramData.asDouble("x"), loadDiagramData.asDouble("y"), loadDiagramData.asInt("seq")),
                    loadDiagramData.asDouble("rotation"));
            addTerminalPoints(loadId, load.getName(), loadIidmDiagramData);
            load.addExtension(InjectionDiagramData.class, loadIidmDiagramData);
        } else {
            LOG.warn("Cannot find load {}, name {} in network {}: skipping load diagram data", loadId, loadDiagramData.get("name"), network.getId());
        }
    }

}
