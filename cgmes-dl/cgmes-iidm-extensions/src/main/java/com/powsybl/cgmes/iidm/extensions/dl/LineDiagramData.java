/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.dl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class LineDiagramData<T extends Identifiable<T>> extends AbstractExtension<T> {

    static final String NAME = "line-diagram-data";

    private List<DiagramPoint> points = new ArrayList<>();

    private LineDiagramData(T line) {
        super(line);
    }

    public LineDiagramData(Line line) {
        this((T) line);
    }

    public LineDiagramData(DanglingLine danglingLine) {
        this((T) danglingLine);
    }

    public LineDiagramData(HvdcLine hvdcLine) {
        this((T) hvdcLine);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public void addPoint(DiagramPoint point) {
        Objects.requireNonNull(point);
        points.add(point);
    }

    public List<DiagramPoint> getPoints() {
        return points.stream().sorted().collect(Collectors.toList());
    }

    public DiagramPoint getFirstPoint() {
        return points.stream().sorted().findFirst().get();
    }

    public DiagramPoint getLastPoint() {
        return points.stream().sorted().reduce((first, second) -> second).get();
    }

}
