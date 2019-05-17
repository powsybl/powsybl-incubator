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
import com.powsybl.cgmes.iidm.extensions.dl.LineDiagramData;
import com.powsybl.iidm.api.DanglingLine;
import com.powsybl.iidm.api.Line;
import com.powsybl.iidm.api.Network;
import com.powsybl.triplestore.api.PropertyBag;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class LineDiagramDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(LineDiagramDataImporter.class);

    private Network network;

    public LineDiagramDataImporter(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    public void importDiagramData(PropertyBag lineDiagramData) {
        Objects.requireNonNull(lineDiagramData);
        String lineId = lineDiagramData.getId("identifiedObject");
        Line line = network.getLine(lineId);
        if (line != null) {
            LineDiagramData<Line> lineIidmDiagramData = line.getExtension(LineDiagramData.class);
            if (lineIidmDiagramData == null) {
                lineIidmDiagramData = new LineDiagramData<>(line);
            }
            lineIidmDiagramData.addPoint(new DiagramPoint(lineDiagramData.asDouble("x"), lineDiagramData.asDouble("y"), lineDiagramData.asInt("seq")));
            line.addExtension(LineDiagramData.class, lineIidmDiagramData);
        } else {
            DanglingLine danglingLine = network.getDanglingLine(lineId);
            if (danglingLine != null) {
                LineDiagramData<DanglingLine> danglingLineDiagramData = danglingLine.getExtension(LineDiagramData.class);
                if (danglingLineDiagramData == null) {
                    danglingLineDiagramData = new LineDiagramData<>(danglingLine);
                }
                danglingLineDiagramData.addPoint(new DiagramPoint(lineDiagramData.asDouble("x"), lineDiagramData.asDouble("y"), lineDiagramData.asInt("seq")));
                danglingLine.addExtension(LineDiagramData.class, danglingLineDiagramData);
            } else {
                LOG.warn("Cannot find line/dangling line {}, name {} in network {}: skipping line diagram data", lineId, lineDiagramData.get("name"), network.getId());
            }
        }
    }

}
