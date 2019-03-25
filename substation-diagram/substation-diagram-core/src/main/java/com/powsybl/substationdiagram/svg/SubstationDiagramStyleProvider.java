/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import java.util.Optional;

import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public interface SubstationDiagramStyleProvider {

    public static final String SUBSTATION_STYLE_CLASS = "substation-diagram";
    public static final String WIRE_STYLE_CLASS = "wire";
    public static final String GRID_STYLE_CLASS = "grid";
    public static final String BUS_STYLE_CLASS = "bus";
    public static final String LABEL_STYLE_CLASS = "component-label";

    Optional<String> getGlobalStyle(Graph graph);

    Optional<String> getCompomentStyle(Graph graph, ComponentType componentType);

    Optional<String> getNodeStyle(Node node);

    Optional<String> getWireStyle(Edge edge);
}
