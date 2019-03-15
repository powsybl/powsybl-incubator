/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.dl.conversion;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesDLModel {

    public static final String CIM_NAMESPACE = "http://iec.ch/TC57/2013/CIM-schema-cim16#";
    public static final String TERMINAL_DIAGRAM_DATA_QUERY_KEY = "terminalDiagramData";
    public static final String BUS_DIAGRAM_DATA_QUERY_KEY = "busDiagramData";
    public static final String BUSBAR_DIAGRAM_DATA_QUERY_KEY = "busbarDiagramData";
    public static final String LINE_DIAGRAM_DATA_QUERY_KEY = "lineDiagramData";
    public static final String GENERATOR_DIAGRAM_DATA_QUERY_KEY = "generatorDiagramData";
    public static final String LOAD_DIAGRAM_DATA_QUERY_KEY = "loadDiagramData";
    public static final String SHUNT_DIAGRAM_DATA_QUERY_KEY = "shuntDiagramData";
    public static final String SWITCH_DIAGRAM_DATA_QUERY_KEY = "switchDiagramData";
    public static final String TRANSFORMER_DIAGRAM_DATA_QUERY_KEY = "transformerDiagramData";
    public static final String HVDC_LINE_DIAGRAM_DATA_QUERY_KEY = "hvdcLineDiagramData";
    public static final String SVC_DIAGRAM_DATA_QUERY_KEY = "staticVarCompensator";

    private static final Logger LOG = LoggerFactory.getLogger(CgmesDLModel.class);

    private final TripleStore tripleStore;
    private final QueryCatalog queryCatalog;

    public CgmesDLModel(TripleStore tripleStore) {
        this(tripleStore, new QueryCatalog("CGMES-DL.sparql"));
    }

    public CgmesDLModel(TripleStore tripleStore, QueryCatalog queryCatalog) {
        this.tripleStore = Objects.requireNonNull(tripleStore);
        tripleStore.defineQueryPrefix("cim", CIM_NAMESPACE);
        this.queryCatalog = Objects.requireNonNull(queryCatalog);
    }

    private PropertyBags getDiagramData(String queryKey) {
        String query = queryCatalog.get(queryKey);
        if (query == null) {
            LOG.warn("Query [{}] not found in catalog", queryKey);
            return new PropertyBags();
        }
        return tripleStore.query(query);
    }

    public PropertyBags getTerminalsDiagramData() {
        LOG.info("Querying triple store for terminals diagram data");
        return getDiagramData(TERMINAL_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getBusesDiagramData() {
        LOG.info("Querying triple store for buses diagram data");
        return getDiagramData(BUS_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getBusbarsDiagramData() {
        LOG.info("Querying triple store for busbars diagram data");
        return getDiagramData(BUSBAR_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getLinesDiagramData() {
        LOG.info("Querying triple store for lines diagram data");
        return getDiagramData(LINE_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getGeneratorsDiagramData() {
        LOG.info("Querying triple store for generators diagram data");
        return getDiagramData(GENERATOR_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getLoadsDiagramData() {
        LOG.info("Querying triple store for loads diagram data");
        return getDiagramData(LOAD_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getShuntsDiagramData() {
        LOG.info("Querying triple store for shunts diagram data");
        return getDiagramData(SHUNT_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getSwitchesDiagramData() {
        LOG.info("Querying triple store for switches diagram data");
        return getDiagramData(SWITCH_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getTransformersDiagramData() {
        LOG.info("Querying triple store for transformers diagram data");
        return getDiagramData(TRANSFORMER_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getHvdcLinesDiagramData() {
        LOG.info("Querying triple store for HVDC lines diagram data");
        return getDiagramData(HVDC_LINE_DIAGRAM_DATA_QUERY_KEY);
    }

    public PropertyBags getSvcsDiagramData() {
        LOG.info("Querying triple store for SVCs diagram data");
        return getDiagramData(SVC_DIAGRAM_DATA_QUERY_KEY);
    }

}
