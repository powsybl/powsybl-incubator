package com.powsybl.substationdiagram.svg;

import java.util.Optional;

import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;

public interface SubstationDiagramStyleProvider {
    
    public static final String SUBSTATION_STYLE_CLASS = "substation-diagram";
    public static final String WIRE_STYLE_CLASS = "wire";
    public static final String GRID_STYLE_CLASS = "grid";
    public static final String BUS_STYLE_CLASS = "bus";

    Optional<String> getGlobalStyle(Graph graph);

    Optional<String> getCompomentStyle(Graph graph, ComponentType componentType);

    Optional<String> getNodeStyle(Node node);

    Optional<String> getWireStyle(Edge edge);
}
