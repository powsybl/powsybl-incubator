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

import com.powsybl.cgmes.iidm.extensions.dl.CouplingDeviseDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.DiagramPoint;
import com.powsybl.cgmes.iidm.extensions.dl.DiagramTerminal;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBags;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public abstract class AbstractCouplingDeviseDiagramDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCouplingDeviseDiagramDataImporter.class);

    protected Network network;
    protected Map<String, PropertyBags> terminalsDiagramData;

    public AbstractCouplingDeviseDiagramDataImporter(Network network, Map<String, PropertyBags> terminalsDiagramData) {
        this.network = Objects.requireNonNull(network);
        this.terminalsDiagramData = Objects.requireNonNull(terminalsDiagramData);
    }

    protected <T> void addTerminalPoints(String equipmentId, String equipmentName, DiagramTerminal terminal, String terminalSide, CouplingDeviseDiagramData<?> diagramData) {
        String terminalKey = equipmentId + "_" + terminalSide;
        if (terminalsDiagramData.containsKey(terminalKey)) {
            PropertyBags equipmentTerminalsDiagramData = terminalsDiagramData.get(terminalKey);
            equipmentTerminalsDiagramData.forEach(terminalDiagramData -> {
                diagramData.addTerminalPoint(terminal, new DiagramPoint(terminalDiagramData.asDouble("x"), terminalDiagramData.asDouble("y"), terminalDiagramData.asInt("seq")));
            });
        } else {
            LOG.warn("Cannot find terminal diagram data of equipment {}, name {}, terminal {}", equipmentId, equipmentName, terminal);
        }
    }
}
