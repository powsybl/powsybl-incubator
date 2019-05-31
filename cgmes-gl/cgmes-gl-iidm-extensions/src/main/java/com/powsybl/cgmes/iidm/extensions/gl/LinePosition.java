/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class LinePosition<T extends Identifiable<T>> extends AbstractExtension<T> {

    static final String NAME = "line-position";

    private List<PositionPoint> points = new ArrayList<>();

    private LinePosition(T line) {
        super(line);
    }

    public LinePosition(Line line) {
        this((T) line);
    }

    public LinePosition(DanglingLine danglingLine) {
        this((T) danglingLine);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public void addPoint(PositionPoint point) {
        Objects.requireNonNull(point);
        points.add(point);
    }

    public List<PositionPoint> getPoints() {
        return points.stream().sorted().collect(Collectors.toList());
    }

}