/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.library.ComponentType;

import java.util.Objects;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BusNode extends Node {

    @JsonIgnore
    private double pxWidth = 1;
    private Position structuralPosition;
    private Position position = new Position(-1, -1);

    protected BusNode(String id, String name, Graph graph, Identifiable identifiable) {
        super(NodeType.BUS, id, name, ComponentType.BUSBAR_SECTION, graph, identifiable);
    }

    public static BusNode create(Graph graph, BusbarSection busbarSection) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(busbarSection);
        return new BusNode(busbarSection.getId(), busbarSection.getName(), graph, busbarSection);
    }

    public static BusNode create(Graph graph, Bus bus) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(bus);
        return new BusNode(bus.getId(), bus.getName(), graph, bus);
    }

    public void calculateCoord(LayoutParameters layoutParameters) {
        setY(layoutParameters.getInitialYBus() +
                     (position.getV() - 1) * layoutParameters.getVerticalSpaceBus());
        setX(layoutParameters.getInitialXBus()
                     + position.getH() * layoutParameters.getCellWidth()
                     + layoutParameters.getHorizontalBusPadding() / 2);
        setPxWidth(position.getHSpan() * layoutParameters.getCellWidth() - layoutParameters.getHorizontalBusPadding());
    }

    public double getPxWidth() {
        return pxWidth;
    }

    public void setPxWidth(double widthBus) {
        this.pxWidth = widthBus;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Position getStructuralPosition() {
        return structuralPosition;
    }

    public void setStructuralPosition(Position structuralPosition) {
        this.structuralPosition = structuralPosition;
    }
}
