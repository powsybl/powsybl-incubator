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
import com.powsybl.iidm.api.Generator;
import com.powsybl.iidm.api.Injection;
import com.powsybl.iidm.api.Load;
import com.powsybl.iidm.api.ShuntCompensator;
import com.powsybl.iidm.api.StaticVarCompensator;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class InjectionDiagramData<T extends Injection<T>> extends AbstractExtension<T> {

    static final String NAME = "injection-diagram-data";

    private final DiagramPoint point;
    private final double rotation;
    private List<DiagramPoint> terminalPoints = new ArrayList<>();

    private InjectionDiagramData(T injection, DiagramPoint point, double rotation) {
        super(injection);
        this.point = Objects.requireNonNull(point);
        this.rotation = Objects.requireNonNull(rotation);
    }

    public InjectionDiagramData(Generator generator, DiagramPoint point, double rotation) {
        this((T) generator, point, rotation);
    }

    public InjectionDiagramData(Load load, DiagramPoint point, double rotation) {
        this((T) load, point, rotation);
    }

    public InjectionDiagramData(ShuntCompensator shunt, DiagramPoint point, double rotation) {
        this((T) shunt, point, rotation);
    }

    public InjectionDiagramData(StaticVarCompensator svc, DiagramPoint point, double rotation) {
        this((T) svc, point, rotation);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public void addTerminalPoint(DiagramPoint point) {
        Objects.requireNonNull(point);
        terminalPoints.add(point);
    }

    public DiagramPoint getPoint() {
        return point;
    }

    public double getRotation() {
        return rotation;
    }

    public List<DiagramPoint> getTerminalPoints() {
        return terminalPoints.stream().sorted().collect(Collectors.toList());
    }

}
