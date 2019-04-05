package com.powsybl.substationdiagram.view;

import javafx.scene.Group;
import javafx.scene.layout.BorderPane;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public abstract class AbstractContainerDiagramView extends BorderPane {
    protected AbstractContainerDiagramView(Group svgImage) {
        super(svgImage);
    }
}
