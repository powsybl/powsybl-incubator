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
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CouplingDeviseDiagramData<T extends Identifiable<T>> extends AbstractExtension<T> {

    static final String NAME = "coupling-devise-diagram-data";

    private final DiagramPoint point;
    private final double rotation;
    private List<DiagramPoint> terminal1Points = new ArrayList<>();
    private List<DiagramPoint> terminal2Points = new ArrayList<>();

    private CouplingDeviseDiagramData(T extendable, DiagramPoint point, double rotation) {
        super(extendable);
        this.point = Objects.requireNonNull(point);
        this.rotation = Objects.requireNonNull(rotation);
    }

    public CouplingDeviseDiagramData(Switch sw, DiagramPoint point, double rotation) {
        this((T) sw, point, rotation);
    }

    public CouplingDeviseDiagramData(TwoWindingsTransformer transformer, DiagramPoint point, double rotation) {
        this((T) transformer, point, rotation);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public void addTerminalPoint(DiagramTerminal terminal, DiagramPoint point) {
        Objects.requireNonNull(terminal);
        Objects.requireNonNull(point);
        switch (terminal) {
            case TERMINAL1:
                terminal1Points.add(point);
                break;
            case TERMINAL2:
                terminal2Points.add(point);
                break;
            default:
                throw new AssertionError("Unexpected terminal: " + terminal);
        }
    }

    public DiagramPoint getPoint() {
        return point;
    }

    public double getRotation() {
        return rotation;
    }

    public List<DiagramPoint> getTerminalPoints(DiagramTerminal terminal) {
        Objects.requireNonNull(terminal);
        switch (terminal) {
            case TERMINAL1:
                return terminal1Points.stream().sorted().collect(Collectors.toList());
            case TERMINAL2:
                return terminal2Points.stream().sorted().collect(Collectors.toList());
            default:
                throw new AssertionError("Unexpected terminal: " + terminal);
        }
    }

}
