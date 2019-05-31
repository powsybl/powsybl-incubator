/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.gl.conversion;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.iidm.extensions.gl.LinePosition;
import com.powsybl.cgmes.iidm.extensions.gl.PositionPoint;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class LinePositionImporter {

    private static final Logger LOG = LoggerFactory.getLogger(LinePositionImporter.class);

    private Network network;

    public LinePositionImporter(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    public void importPosition(PropertyBag linePositionData) {
        Objects.requireNonNull(linePositionData);
        if (!CgmesGLUtils.checkCoordinateSystem(linePositionData.getId("crsName"), linePositionData.getId("crsUrn"))) {
            throw new PowsyblException("Unsupported coodinates system: " + linePositionData.getId("crsName"));
        }
        String lineId = linePositionData.getId("powerSystemResource");
        Line line = network.getLine(lineId);
        if (line != null) {
            LinePosition<Line> linePosition = line.getExtension(LinePosition.class);
            if (linePosition == null) {
                linePosition = new LinePosition<>(line);
            }
            linePosition.addPoint(new PositionPoint(linePositionData.asDouble("x"), linePositionData.asDouble("y"), linePositionData.asInt("seq")));
            line.addExtension(LinePosition.class, linePosition);
        } else {
            DanglingLine danglingLine = network.getDanglingLine(lineId);
            if (danglingLine != null) {
                LinePosition<DanglingLine> danglingLinePosition = danglingLine.getExtension(LinePosition.class);
                if (danglingLinePosition == null) {
                    danglingLinePosition = new LinePosition<>(danglingLine);
                }
                danglingLinePosition.addPoint(new PositionPoint(linePositionData.asDouble("x"), linePositionData.asDouble("y"), linePositionData.asInt("seq")));
                danglingLine.addExtension(LinePosition.class, danglingLinePosition);
            } else {
                LOG.warn("Cannot find line/dangling {}, name {} in network {}: skipping line position", lineId, linePositionData.get("name"), network.getId());
            }
        }
    }

}
