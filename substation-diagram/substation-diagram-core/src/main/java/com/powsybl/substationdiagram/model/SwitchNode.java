/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.SwitchKind;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.substationdiagram.library.ComponentType;

import java.util.Objects;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SwitchNode extends Node {

    private final SwitchKind kind;

    protected SwitchNode(String id, String name, ComponentType componentType, boolean fictitious, Graph graph, SwitchKind kind) {
        super(NodeType.SWITCH, id, name, componentType, fictitious, graph);
        this.kind = Objects.requireNonNull(kind);
    }

    public static SwitchNode create(Graph graph, Switch aSwitch) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(aSwitch);
        ComponentType componentType;
        switch (aSwitch.getKind()) {
            case BREAKER:
                componentType = ComponentType.BREAKER;
                break;
            case DISCONNECTOR:
                componentType = ComponentType.DISCONNECTOR;
                break;
            case LOAD_BREAK_SWITCH:
                componentType = ComponentType.LOAD_BREAK_SWITCH;
                break;
            default:
                throw new AssertionError();
        }
        return new SwitchNode(aSwitch.getId(), aSwitch.getName(), componentType, false, graph, aSwitch.getKind());
    }

    public static SwitchNode create(Graph graph, Terminal terminal) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(terminal);
        Bus bus = terminal.getBusBreakerView().getConnectableBus();
        String id = bus.getId() + "_" + terminal.getConnectable().getId();
        String name = bus.getName() + "_" + terminal.getConnectable().getName();
        return new SwitchNode(id, name, ComponentType.DISCONNECTOR, false, graph, SwitchKind.DISCONNECTOR);
    }

    public static SwitchNode createFictitious(Graph graph, String id) {
        return new SwitchNode(id, id, ComponentType.NODE, true, graph, SwitchKind.BREAKER);
    }

    public SwitchKind getKind() {
        return kind;
    }

    public Node getOtherAdjNode(Node adj) {
        // a switch node has 2 and only 2 adjacent nodes.
        if (getAdjacentNodes().size() != 2) {
            throw new PowsyblException("Error switch node not having exactly 2 adjacent nodes " + getId());
        }
        return getAdjacentNodes().get(getAdjacentNodes().get(0).equals(adj) ? 1 : 0);
    }
}
