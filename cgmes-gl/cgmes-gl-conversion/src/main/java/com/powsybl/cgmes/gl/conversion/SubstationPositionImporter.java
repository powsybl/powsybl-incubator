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

import com.powsybl.cgmes.iidm.extensions.gl.CoordinateSystem;
import com.powsybl.cgmes.iidm.extensions.gl.PositionPoint;
import com.powsybl.cgmes.iidm.extensions.gl.SubstationPosition;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.triplestore.api.PropertyBag;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class SubstationPositionImporter {

    private static final Logger LOG = LoggerFactory.getLogger(SubstationPositionImporter.class);

    private Network network;

    public SubstationPositionImporter(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    public void importPosition(PropertyBag substationPositionData) {
        Objects.requireNonNull(substationPositionData);
        String substationId = substationPositionData.getId("powerSystemResource");
        Substation substation = network.getSubstation(substationId);
        if (substation != null) {
            SubstationPosition<Substation> substationPosition = new SubstationPosition<Substation>(substation,
                    new PositionPoint(substationPositionData.asDouble("x"), substationPositionData.asDouble("y"), 0),
                    new CoordinateSystem(substationPositionData.get("crsName"), substationPositionData.get("crsUrn")));
            substation.addExtension(SubstationPosition.class, substationPosition);
        } else {
            LOG.warn("Cannot find substation {}, name {} in network {}: skipping substation position", substationId, substationPositionData.get("name"), network.getId());
        }
    }

}
