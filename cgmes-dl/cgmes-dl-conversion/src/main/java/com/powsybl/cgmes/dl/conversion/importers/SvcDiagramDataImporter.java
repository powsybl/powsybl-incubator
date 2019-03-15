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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class SvcDiagramDataImporter extends AbstractInjectionDiagramDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(ShuntDiagramDataImporter.class);

    public SvcDiagramDataImporter(Network network, Map<String, PropertyBags> terminalsDiagramData) {
        super(network, terminalsDiagramData);
    }

    public void importDiagramData(PropertyBag svcDiagramData) {
        Objects.requireNonNull(svcDiagramData);
        String svcId = svcDiagramData.getId("identifiedObject");
        StaticVarCompensator svc = network.getStaticVarCompensator(svcId);
        if (svc != null) {
            InjectionDiagramData<StaticVarCompensator> svcIidmDiagramData = new InjectionDiagramData<>(svc,
                    new DiagramPoint(svcDiagramData.asDouble("x"), svcDiagramData.asDouble("y"), svcDiagramData.asInt("seq")),
                    svcDiagramData.asDouble("rotation"));
            addTerminalPoints(svcId, svc.getName(), svcIidmDiagramData);
            svc.addExtension(InjectionDiagramData.class, svcIidmDiagramData);
        } else {
            LOG.warn("Cannot find svc {}, name {} in network {}: skipping svc diagram data", svcId, svcDiagramData.get("name"), network.getId());
        }
    }

}
