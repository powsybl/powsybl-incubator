/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.library.ComponentType;

import java.util.Objects;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class Feeder2WTNode extends FeederBranchNode {
    protected Feeder2WTNode(String id, String name, ComponentType componentType, boolean fictitious, Graph graph, VoltageLevel vlOtherSide) {
        super(id, name, componentType, fictitious, graph, vlOtherSide);
    }

    public static FeederNode create(Graph graph, TwoWindingsTransformer branch, TwoWindingsTransformer.Side side) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(branch);
        ComponentType componentType;

        if (branch.getPhaseTapChanger() == null) {
            componentType = ComponentType.TWO_WINDINGS_TRANSFORMER;
        } else {
            componentType = ComponentType.PHASE_SHIFT_TRANSFORMER;
        }

        String id = branch.getId() + "_" + side.name();
        String name = branch.getName() + "_" + side.name();
        TwoWindingsTransformer.Side otherSide = side == TwoWindingsTransformer.Side.ONE
                ? TwoWindingsTransformer.Side.TWO
                : TwoWindingsTransformer.Side.ONE;
        VoltageLevel vlOtherSide = branch.getTerminal(otherSide).getVoltageLevel();
        return new Feeder2WTNode(id, name, componentType, false, graph, vlOtherSide);
    }

    public static Feeder2WTNode create(Graph graph, String id, String name, VoltageLevel vlOtherSide) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(vlOtherSide);
        return new Feeder2WTNode(id, name, ComponentType.TWO_WINDINGS_TRANSFORMER, false, graph, vlOtherSide);
    }
}
