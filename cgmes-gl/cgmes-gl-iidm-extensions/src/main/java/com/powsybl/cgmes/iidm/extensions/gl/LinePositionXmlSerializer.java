/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionXmlSerializer;
import com.powsybl.commons.xml.XmlReaderContext;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.commons.xml.XmlWriterContext;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
@AutoService(ExtensionXmlSerializer.class)
public class LinePositionXmlSerializer<T extends Identifiable<T>> implements ExtensionXmlSerializer<T, LinePosition<T>> {

    @Override
    public String getExtensionName() {
        return LinePosition.NAME;
    }

    @Override
    public String getCategoryName() {
        return "network";
    }

    @Override
    public Class<? super LinePosition<T>> getExtensionClass() {
        return LinePosition.class;
    }

    @Override
    public boolean hasSubElements() {
        return true;
    }

    @Override
    public InputStream getXsdAsStream() {
        return getClass().getResourceAsStream("/xsd/linePosition.xsd");
    }

    @Override
    public String getNamespaceUri() {
        return "http://www.itesla_project.eu/schema/iidm/ext/line_position/1_0";
    }

    @Override
    public String getNamespacePrefix() {
        return "lp";
    }

    @Override
    public void write(LinePosition<T> linePosition, XmlWriterContext context) throws XMLStreamException {
        for (Coordinate point : linePosition.getCoordinates()) {
            context.getWriter().writeEmptyElement(getNamespaceUri(), "coordinate");
            XmlUtil.writeDouble("longitude", point.getLongitude(), context.getWriter());
            XmlUtil.writeDouble("latitude", point.getLatitude(), context.getWriter());
        }
    }

    @Override
    public LinePosition<T> read(T line, XmlReaderContext context) throws XMLStreamException {
        List<Coordinate> coordinates = new ArrayList<>();
        XmlUtil.readUntilEndElement(getExtensionName(), context.getReader(), () -> {
            double longitude = XmlUtil.readDoubleAttribute(context.getReader(), "longitude");
            double latitude = XmlUtil.readDoubleAttribute(context.getReader(), "latitude");
            coordinates.add(new Coordinate(longitude, latitude));
        });
        return createLinePosition(line, coordinates);
    }

    private LinePosition<T> createLinePosition(T line, List<Coordinate> coordinates) {
        if (line instanceof Line) {
            return new LinePosition<>((Line) line, coordinates);
        } else if (line instanceof DanglingLine) {
            return new LinePosition<>((DanglingLine) line, coordinates);
        } else {
            throw new AssertionError("Unsupported equipment");
        }
    }

}
