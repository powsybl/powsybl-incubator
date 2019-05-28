/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.substationdiagram.library.ComponentType;

import java.util.Objects;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class FeederNode extends Node {

    private int order = -1;

    private AbstractBusCell.Direction direction = AbstractBusCell.Direction.UNDEFINED;

    protected FeederNode(String id, String name, ComponentType componentType, boolean fictitious, Graph graph) {
        super(NodeType.FEEDER, id, name, componentType, fictitious, graph);
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
        return new FeederNode(injection.getId(), injection.getName(), componentType, false, graph);
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
                if (((TwoWindingsTransformer) branch).getPhaseTapChanger() == null) {
                    componentType = ComponentType.TWO_WINDINGS_TRANSFORMER;
                } else {
                    componentType = ComponentType.PHASE_SHIFT_TRANSFORMER;
                }
                break;
            default:
                throw new AssertionError();
        }
        String id = branch.getId() + "_" + side.name();
        String name = branch.getName() + "_" + side.name();
        return new FeederNode(id, name, componentType, false, graph);
    }

    public static FeederNode create(Graph graph, ThreeWindingsTransformer twt, ThreeWindingsTransformer.Side side) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(twt);
        Objects.requireNonNull(side);
        String id = twt.getId() + "_" + side.name();
        String name = twt.getName() + "_" + side.name();
        return new FeederNode(id, name, ComponentType.THREE_WINDINGS_TRANSFORMER, false, graph);
    }

    public static FeederNode createFictitious(Graph graph, String id) {
        return new FeederNode(id, id, ComponentType.NODE, true, graph);
    }

    @Override
    public void setCell(AbstractCell cell) {
        if (!(cell instanceof ExternCell)) {
            throw new PowsyblException("The Cell of a feeder node shall be an ExternCell");
        }
        super.setCell(cell);
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public AbstractBusCell.Direction getDirection() {
        return direction;
    }

    public void setDirection(AbstractBusCell.Direction direction) {
        this.direction = direction;
    }
}
