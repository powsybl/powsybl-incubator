/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.gl.conversion;

import java.util.Arrays;
import java.util.Objects;

import com.powsybl.cgmes.iidm.extensions.gl.Coordinate;
import com.powsybl.cgmes.model.CgmesNamespace;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.TripleStore;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public abstract class AbstractPositionExporter {

    protected TripleStore tripleStore;
    protected ExportContext context;

    public AbstractPositionExporter(TripleStore tripleStore, ExportContext context) {
        this.tripleStore = Objects.requireNonNull(tripleStore);
        this.context = Objects.requireNonNull(context);
    }

    protected String addLocation(String id, String name) {
        PropertyBag locationProperties = new PropertyBag(Arrays.asList("IdentifiedObject.name", "CoordinateSystem", "PowerSystemResources"));
        locationProperties.setResourceNames(Arrays.asList("CoordinateSystem", "PowerSystemResources"));
        locationProperties.setClassPropertyNames(Arrays.asList("IdentifiedObject.name"));
        locationProperties.put("IdentifiedObject.name", name);
        locationProperties.put("PowerSystemResources", id);
        locationProperties.put("CoordinateSystem", context.getCoordinateSystemId());
        return tripleStore.add(context.getGlContext(), CgmesNamespace.CIM_16_NAMESPACE, "Location", locationProperties);
    }

    protected void addLocationPoint(String locationId, Coordinate coordinate, int seq) {
        PropertyBag locationPointProperties = (seq == 0)
                ? new PropertyBag(Arrays.asList("xPosition", "yPosition", "Location"))
                : new PropertyBag(Arrays.asList("sequenceNumber", "xPosition", "yPosition", "Location"));
        locationPointProperties.setResourceNames(Arrays.asList("Location"));
        if (seq > 0) {
            locationPointProperties.put("sequenceNumber", Integer.toString(seq));
        }
        locationPointProperties.put("xPosition", Double.toString(coordinate.getLongitude()));
        locationPointProperties.put("yPosition", Double.toString(coordinate.getLatitude()));
        locationPointProperties.put("Location", locationId);
        tripleStore.add(context.getGlContext(), CgmesNamespace.CIM_16_NAMESPACE, "PositionPoint", locationPointProperties);
    }

}
