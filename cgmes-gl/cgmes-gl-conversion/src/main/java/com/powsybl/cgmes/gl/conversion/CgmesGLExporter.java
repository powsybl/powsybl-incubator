/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.gl.conversion;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesNamespace;
import com.powsybl.cgmes.model.CgmesSubset;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesGLExporter {

    private static final Logger LOG = LoggerFactory.getLogger(CgmesGLExporter.class);
    public static final String MD_NAMESPACE = "http://iec.ch/TC57/61970-552/ModelDescription/1#";

    private Network network;
    private TripleStore tripleStore;

    public CgmesGLExporter(Network network, TripleStore tripleStore) {
        this.network = Objects.requireNonNull(network);
        this.tripleStore = Objects.requireNonNull(tripleStore);
    }

    public CgmesGLExporter(Network network) {
        this(network, TripleStoreFactory.create());
    }

    public void exportData(DataSource dataSource) {
        Objects.requireNonNull(dataSource);
        ExportContext context = new ExportContext();
        context.setBasename(dataSource.getBaseName());
        context.setGlContext(CgmesGLUtils.contextNameFor(CgmesSubset.GEOGRAPHICAL_LOCATION, tripleStore, dataSource.getBaseName()));
        addNamespaces(context);
        addModel(context);
        addCoordinateSystem(context);
        exportSubstationsPosition(context);
        exportLinesPosition(context);
        tripleStore.write(dataSource);
    }

    private void addNamespaces(ExportContext context) {
        if (!namespaceAlreadyExist("data")) {
            tripleStore.addNamespace("data", "http://" + context.getBasename().toLowerCase() + "/#");
        }
        if (!namespaceAlreadyExist("cim")) {
            tripleStore.addNamespace("cim", CgmesNamespace.CIM_16_NAMESPACE);
        }
        if (!namespaceAlreadyExist("md")) {
            tripleStore.addNamespace("md", MD_NAMESPACE);
        }
    }

    private boolean namespaceAlreadyExist(String prefix) {
        return tripleStore.getNamespaces().stream().map(prefixNamespace -> prefixNamespace.getPrefix()).anyMatch(prefix::equals);
    }

    private void addModel(ExportContext context) {
        PropertyBag modelProperties = new PropertyBag(Arrays.asList("Model.scenarioTime", "Model.created", "Model.description", "Model.version", "Model.profile", "Model.DependentOn"));
        modelProperties.setResourceNames(Arrays.asList("Model.DependentOn"));
        modelProperties.setClassPropertyNames(Arrays.asList("Model.scenarioTime", "Model.created", "Model.description", "Model.version", "Model.profile", "Model.DependentOn"));
        modelProperties.put("Model.scenarioTime", network.getCaseDate().toString());
        modelProperties.put("Model.created", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date()));
        modelProperties.put("Model.description", network.getName());
        modelProperties.put("Model.version", "4");
        modelProperties.put("Model.profile", "http://entsoe.eu/CIM/GeographicalLocation/2/1");
        modelProperties.put("Model.DependentOn", network.getId());
        tripleStore.add(context.getGlContext(), MD_NAMESPACE, "FullModel", modelProperties);
    }

    private void addCoordinateSystem(ExportContext context) {
        PropertyBag coordinateSystemProperties = new PropertyBag(Arrays.asList("IdentifiedObject.name", "crsUrn"));
        coordinateSystemProperties.setClassPropertyNames(Arrays.asList("IdentifiedObject.name"));
        coordinateSystemProperties.put("IdentifiedObject.name", CgmesGLUtils.COORDINATE_SYSTEM_NAME);
        coordinateSystemProperties.put("crsUrn", CgmesGLUtils.COORDINATE_SYSTEM_URN);
        context.setCoordinateSystemId(tripleStore.add(context.getGlContext(), CgmesNamespace.CIM_16_NAMESPACE, "CoordinateSystem", coordinateSystemProperties));
    }

    private void exportSubstationsPosition(ExportContext context) {
        SubstationPositionExporter positionExporter = new SubstationPositionExporter(tripleStore, context);
        LOG.info("Exporting Substations Position");
        network.getSubstationStream().forEach(positionExporter::exportPosition);
    }

    private void exportLinesPosition(ExportContext context) {
        LinePositionExporter positionExporter = new LinePositionExporter(tripleStore, context);
        LOG.info("Exporting Lines Position");
        network.getLineStream().forEach(positionExporter::exportPosition);
        LOG.info("Exporting Dangling Lines Position");
        network.getDanglingLineStream().forEach(positionExporter::exportPosition);
    }

}
