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
import com.powsybl.iidm.api.Bus;
import com.powsybl.iidm.api.Network;
import com.powsybl.iidm.api.VoltageLevel;
import com.powsybl.triplestore.api.PropertyBag;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class BusDiagramDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(BusDiagramDataImporter.class);

    private Network network;

    public BusDiagramDataImporter(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    public void importDiagramData(PropertyBag busDiagramData) {
        Objects.requireNonNull(busDiagramData);
        String busId = busDiagramData.getId("identifiedObject");
        String vlId = busDiagramData.getId("voltageLevel");
        VoltageLevel vl = network.getVoltageLevel(vlId);
        if (vl != null) {
            Bus bus = vl.getBusBreakerView().getBus(busId);
            if (bus != null) {
                NodeDiagramData<Bus> busIidmDiagramData = bus.getExtension(NodeDiagramData.class);
                if (busIidmDiagramData == null) {
                    busIidmDiagramData = new NodeDiagramData<>(bus);
                }
                if (busDiagramData.asInt("seq") == 1) {
                    busIidmDiagramData.setPoint1(new DiagramPoint(busDiagramData.asDouble("x"), busDiagramData.asDouble("y"), busDiagramData.asInt("seq")));
                } else {
                    busIidmDiagramData.setPoint2(new DiagramPoint(busDiagramData.asDouble("x"), busDiagramData.asDouble("y"), busDiagramData.asInt("seq")));
                }
                bus.addExtension(NodeDiagramData.class, busIidmDiagramData);
            } else {
                LOG.warn("Cannot find bus {}, name {} in network {}: skipping bus diagram data", busId, busDiagramData.get("name"), network.getId());
            }
        } else {
            LOG.warn("Cannot find voltage level {}, name {} in network {}: skipping bus diagram data", vlId, busDiagramData.get("vlname"), network.getId());
        }
    }

}
