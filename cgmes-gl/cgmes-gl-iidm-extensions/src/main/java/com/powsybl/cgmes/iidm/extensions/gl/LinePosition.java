/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;

import java.util.List;
import java.util.Objects;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class LinePosition<T extends Identifiable<T>> extends AbstractExtension<T> {

    static final String NAME = "line-position";

    private final List<Coordinate> coordinates;

    private LinePosition(T line, List<Coordinate> coordinates) {
        super(line);
        this.coordinates = Objects.requireNonNull(coordinates);
    }

    public LinePosition(Line line, List<Coordinate> coordinates) {
        this((T) line, coordinates);
    }

    public LinePosition(DanglingLine danglingLine, List<Coordinate> coordinates) {
        this((T) danglingLine, coordinates);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

}
