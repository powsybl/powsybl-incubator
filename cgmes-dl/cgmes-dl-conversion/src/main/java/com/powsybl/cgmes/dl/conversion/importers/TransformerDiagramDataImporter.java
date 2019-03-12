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
import com.powsybl.cgmes.iidm.extensions.dl.ThreeWindingsTransformerDiagramData;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class TransformerDiagramDataImporter extends AbstractCouplingDeviseDiagramDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerDiagramDataImporter.class);

    public TransformerDiagramDataImporter(Network network, Map<String, PropertyBags> terminalsDiagramData) {
        super(network, terminalsDiagramData);
    }

    public void importDiagramData(PropertyBag transformersDiagramData) {
        Objects.requireNonNull(transformersDiagramData);
        String transformerId = transformersDiagramData.getId("identifiedObject");
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(transformerId);
        if (transformer != null) {
            CouplingDeviseDiagramData<TwoWindingsTransformer> transformerIidmDiagramData = new CouplingDeviseDiagramData<>(transformer,
                    new DiagramPoint(transformersDiagramData.asDouble("x"), transformersDiagramData.asDouble("y"), transformersDiagramData.asInt("seq")),
                    transformersDiagramData.asDouble("rotation"));
            addTerminalPoints(transformerId, transformer.getName(), DiagramTerminal.TERMINAL1, "1", transformerIidmDiagramData);
            addTerminalPoints(transformerId, transformer.getName(), DiagramTerminal.TERMINAL2, "2", transformerIidmDiagramData);
            transformer.addExtension(CouplingDeviseDiagramData.class, transformerIidmDiagramData);
        } else {
            ThreeWindingsTransformer transformer3w = network.getThreeWindingsTransformer(transformerId);
            if (transformer3w != null) {
                ThreeWindingsTransformerDiagramData transformerIidmDiagramData = new ThreeWindingsTransformerDiagramData(transformer3w,
                        new DiagramPoint(transformersDiagramData.asDouble("x"), transformersDiagramData.asDouble("y"), transformersDiagramData.asInt("seq")),
                        transformersDiagramData.asDouble("rotation"));
                addTerminalPoints(transformerId, transformer3w.getName(), DiagramTerminal.TERMINAL1, "1", transformerIidmDiagramData);
                addTerminalPoints(transformerId, transformer3w.getName(), DiagramTerminal.TERMINAL2, "2", transformerIidmDiagramData);
                addTerminalPoints(transformerId, transformer3w.getName(), DiagramTerminal.TERMINAL3, "3", transformerIidmDiagramData);
                transformer3w.addExtension(ThreeWindingsTransformerDiagramData.class, transformerIidmDiagramData);
            } else {
                LOG.warn("Cannot find transformer {}, name {} in network {}: skipping transformer diagram data", transformerId, transformersDiagramData.get("name"), network.getId());
            }
        }
    }

    private void addTerminalPoints(String transformerId, String transformerName, DiagramTerminal terminal, String terminalSide, ThreeWindingsTransformerDiagramData transformerDiagramData) {
        String terminalKey = transformerId + "_" + terminalSide;
        if (terminalsDiagramData.containsKey(terminalKey)) {
            PropertyBags equipmentTerminalsDiagramData = terminalsDiagramData.get(terminalKey);
            equipmentTerminalsDiagramData.forEach(terminalDiagramData ->
                transformerDiagramData.addTerminalPoint(terminal, new DiagramPoint(terminalDiagramData.asDouble("x"), terminalDiagramData.asDouble("y"), terminalDiagramData.asInt("seq")))
            );
        } else {
            LOG.warn("Cannot find terminal diagram data of transformer {}, name {}, terminal {}", transformerId, transformerName, terminal);
        }
    }

}
