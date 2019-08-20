/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

import java.util.Objects;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Substation;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class SubstationPosition extends AbstractExtension<Substation> {

    static final String NAME = "substation-position";

    private final Coordinate coordinate;

    public SubstationPosition(Substation substation, Coordinate coordinate) {
        super(substation);
        this.coordinate = Objects.requireNonNull(coordinate);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

}
