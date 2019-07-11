package com.powsybl.substationdiagram.model;

import com.powsybl.substationdiagram.library.ComponentType;

public class ArrowNode extends Node {

    public ArrowNode(Graph graph) {
        super(NodeType.OTHER, "", "", ComponentType.ARROW, false, graph);
    }

}
