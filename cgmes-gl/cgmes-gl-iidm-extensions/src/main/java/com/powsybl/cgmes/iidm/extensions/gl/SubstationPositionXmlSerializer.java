/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionXmlSerializer;
import com.powsybl.commons.xml.XmlReaderContext;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.commons.xml.XmlWriterContext;
import com.powsybl.iidm.network.Substation;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
@AutoService(ExtensionXmlSerializer.class)
public class SubstationPositionXmlSerializer implements ExtensionXmlSerializer<Substation, SubstationPosition> {

    @Override
    public String getExtensionName() {
        return SubstationPosition.NAME;
    }

    @Override
    public String getCategoryName() {
        return "network";
    }

    @Override
    public Class<? super SubstationPosition> getExtensionClass() {
        return SubstationPosition.class;
    }

    @Override
    public boolean hasSubElements() {
        return true;
    }

    @Override
    public InputStream getXsdAsStream() {
        return getClass().getResourceAsStream("/xsd/substationPosition.xsd");
    }

    @Override
    public String getNamespaceUri() {
        return "http://www.itesla_project.eu/schema/iidm/ext/substation_position/1_0";
    }

    @Override
    public String getNamespacePrefix() {
        return "sp";
    }

    @Override
    public void write(SubstationPosition substationPosition, XmlWriterContext context) throws XMLStreamException {
        context.getWriter().writeEmptyElement(getNamespaceUri(), "coordinate");
        XmlUtil.writeDouble("longitude", substationPosition.getCoordinate().getLongitude(), context.getWriter());
        XmlUtil.writeDouble("latitude", substationPosition.getCoordinate().getLatitude(), context.getWriter());
    }

    @Override
    public SubstationPosition read(Substation substation, XmlReaderContext context) throws XMLStreamException {
        Coordinate[] coordinate = new Coordinate[1];
        XmlUtil.readUntilEndElement(getExtensionName(), context.getReader(), () -> {
            double longitude = XmlUtil.readDoubleAttribute(context.getReader(), "longitude");
            double latitude = XmlUtil.readDoubleAttribute(context.getReader(), "latitude");
            coordinate[0] = new Coordinate(longitude, latitude);
        });
        return new SubstationPosition(substation, coordinate[0]);
    }

}
