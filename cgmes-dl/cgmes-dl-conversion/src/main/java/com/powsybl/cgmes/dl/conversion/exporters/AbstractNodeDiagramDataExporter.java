/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion.exporters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.dl.conversion.ExportContext;
import com.powsybl.cgmes.iidm.extensions.dl.NodeDiagramData;
import com.powsybl.triplestore.api.TripleStore;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public abstract class AbstractNodeDiagramDataExporter extends AbstractDiagramDataExporter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNodeDiagramDataExporter.class);

    public AbstractNodeDiagramDataExporter(TripleStore tripleStore, ExportContext context) {
        super(tripleStore, context);
    }

    protected void addDiagramData(String id, String name, NodeDiagramData<?> diagramData, String diagramObjectStyleId) {
        if (diagramData != null) {
            String diagramObjectId = addDiagramObject(id, name, 0, diagramObjectStyleId);
            addDiagramObjectPoint(diagramObjectId, diagramData.getPoint1());
            addDiagramObjectPoint(diagramObjectId, diagramData.getPoint2());
        } else {
            LOG.warn("Node {}, name {} has no diagram data, skipping export", id, name);
        }
    }

}
