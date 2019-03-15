/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion.importers;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.iidm.extensions.dl.DiagramPoint;
import com.powsybl.cgmes.iidm.extensions.dl.NodeDiagramData;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class BusbarDiagramDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(BusbarDiagramDataImporter.class);

    private Network network;

    public BusbarDiagramDataImporter(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    public void importDiagramData(PropertyBag busbarDiagramData) {
        Objects.requireNonNull(busbarDiagramData);
        String busbarId = busbarDiagramData.getId("busbarSection");
        BusbarSection busbar = network.getBusbarSection(busbarId);
        if (busbar != null) {
            NodeDiagramData<BusbarSection> busbarIidmDiagramData = busbar.getExtension(NodeDiagramData.class);
            if (busbarIidmDiagramData == null) {
                busbarIidmDiagramData = new NodeDiagramData<>(busbar);
            }
            if (busbarDiagramData.asInt("seq") == 1) {
                busbarIidmDiagramData.setPoint1(new DiagramPoint(busbarDiagramData.asDouble("x"), busbarDiagramData.asDouble("y"), busbarDiagramData.asInt("seq")));
            } else {
                busbarIidmDiagramData.setPoint2(new DiagramPoint(busbarDiagramData.asDouble("x"), busbarDiagramData.asDouble("y"), busbarDiagramData.asInt("seq")));
            }
            busbar.addExtension(NodeDiagramData.class, busbarIidmDiagramData);
        } else {
            LOG.warn("Cannot find busbar {}, name {} in network {}: skipping busbar diagram data", busbarId, busbarDiagramData.get("name"), network.getId());
        }
    }

}
