/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.gl.conversion;

import java.util.Objects;

import com.powsybl.cgmes.iidm.extensions.gl.LinePosition;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.triplestore.api.TripleStore;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class LinePositionExporter extends AbstractPositionExporter {

    public LinePositionExporter(TripleStore tripleStore, ExportContext context) {
        super(tripleStore, context);
    }

    public void exportPosition(Line line) {
        Objects.requireNonNull(line);
        LinePosition<Line> linePosition = line.getExtension(LinePosition.class);
        exportPosition(line.getId(), line.getName(), linePosition);
    }

    public void exportPosition(DanglingLine danglingLine) {
        Objects.requireNonNull(danglingLine);
        LinePosition<DanglingLine> linePosition = danglingLine.getExtension(LinePosition.class);
        exportPosition(danglingLine.getId(), danglingLine.getName(), linePosition);
    }

    private void exportPosition(String id, String name, LinePosition<?> linePosition) {
        String locationId = addLocation(id, name);
        for (int i = 0; i < linePosition.getCoordinates().size(); i++) {
            addLocationPoint(locationId, linePosition.getCoordinates().get(i), i + 1);
        }
    }

}
