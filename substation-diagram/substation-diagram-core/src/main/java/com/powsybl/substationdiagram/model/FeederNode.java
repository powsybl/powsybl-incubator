/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.substationdiagram.library.ComponentType;

import java.util.Objects;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class FeederNode extends Node {

    private int order = -1;

    private Cell.Direction direction = Cell.Direction.UNDEFINED;

    protected FeederNode(String id, String name, ComponentType componentType, Graph graph) {
        super(NodeType.FEEDER, id, name, componentType, graph);
    }

    public static FeederNode create(Graph graph, Injection injection) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(injection);
        ComponentType componentType;
        switch (injection.getType()) {
            case GENERATOR:
                componentType = ComponentType.GENERATOR;
                break;
            case LOAD:
                componentType = ComponentType.LOAD;
                break;
            case HVDC_CONVERTER_STATION:
                componentType = ComponentType.VSC_CONVERTER_STATION;
                break;
            case STATIC_VAR_COMPENSATOR:
                componentType = ComponentType.STATIC_VAR_COMPENSATOR;
                break;
            case SHUNT_COMPENSATOR:
                componentType = ((ShuntCompensator) injection).getbPerSection() >= 0 ? ComponentType.CAPACITOR : ComponentType.INDUCTOR;
                break;
            case DANGLING_LINE:
                componentType = ComponentType.DANGLING_LINE;
                break;
            default:
                throw new AssertionError();
        }
        return new FeederNode(injection.getId(), injection.getName(), componentType, graph);
    }

    public static FeederNode create(Graph graph, Branch branch, Branch.Side side) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(branch);
        ComponentType componentType;
        switch (branch.getType()) {
            case LINE:
                componentType = ComponentType.LINE;
                break;
            case TWO_WINDINGS_TRANSFORMER:
                componentType = ComponentType.TWO_WINDINGS_TRANSFORMER;
                break;
            default:
                throw new AssertionError();
        }
        String id = branch.getId() + "_" + side.name();
        String name = branch.getName() + "_" + side.name();
        return new FeederNode(id, name, componentType, graph);
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Cell.Direction getDirection() {
        return direction;
    }

    public void setDirection(Cell.Direction direction) {
        this.direction = direction;
    }
}
