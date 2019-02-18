/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.library;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Jeanson Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ComponentMetadataAdapter extends XmlAdapter<AdaptedComponentMetadata, ComponentMetadata> {

    @Override
    public ComponentMetadata unmarshal(AdaptedComponentMetadata adapted) {
        return new ComponentMetadata(adapted.getType(), adapted.getId(), adapted.getAnchorPoints(), adapted.getSize());
    }

    @Override
    public AdaptedComponentMetadata marshal(ComponentMetadata componentMetadata) {
        AdaptedComponentMetadata adapted = new AdaptedComponentMetadata();
        adapted.setType(componentMetadata.getType());
        adapted.setId(componentMetadata.getId());
        adapted.setSize(componentMetadata.getSize());
        adapted.setAnchorPoints(componentMetadata.getAnchorPoints());
        return adapted;
    }
}
