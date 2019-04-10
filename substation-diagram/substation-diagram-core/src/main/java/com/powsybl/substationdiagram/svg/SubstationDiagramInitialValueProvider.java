package com.powsybl.substationdiagram.svg;

import com.powsybl.substationdiagram.model.Node;

public interface SubstationDiagramInitialValueProvider {

    public enum Direction {
        UP, DOWN;
    }

    InitialValue getInitialValue(Node node);

}
