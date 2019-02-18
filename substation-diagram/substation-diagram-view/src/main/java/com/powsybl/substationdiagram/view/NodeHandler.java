/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import com.powsybl.substationdiagram.library.ComponentSize;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.BaseNode;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Jeanson Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NodeHandler implements BaseNode {

    private final Node node;

    private final ComponentType componentType;

    private final List<WireHandler> wireHandlers = new ArrayList<>();

    private final boolean rotated;

    private final GraphMetadata metadata;

    private double mouseX;
    private double mouseY;

    public NodeHandler(Node node, ComponentType componentType, boolean rotated, GraphMetadata metadata) {
        this.node = Objects.requireNonNull(node);
        this.componentType = Objects.requireNonNull(componentType);
        this.rotated = rotated;
        this.metadata = Objects.requireNonNull(metadata);
        setDragAndDrop();
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String getId() {
        return node.getId();
    }

    @Override
    public ComponentType getComponentType() {
        return componentType;
    }

    public void addWire(WireHandler w) {
        wireHandlers.add(w);
    }

    @Override
    public boolean isRotated() {
        return rotated;
    }

    @Override
    public double getX() {
        ComponentSize size = metadata.getComponentMetadata(componentType).getSize();
        return node.localToParent(node.getLayoutX() + size.getWidth() / 2,
                                  node.getLayoutY() + size.getHeight() / 2).getX();
    }

    @Override
    public double getY() {
        ComponentSize size = metadata.getComponentMetadata(componentType).getSize();
        return node.localToParent(node.getLayoutX() + size.getWidth() / 2,
                                  node.getLayoutY() + size.getHeight() / 2).getY();
    }

    public void setDragAndDrop() {
        node.setOnMousePressed(event -> {
            mouseX = event.getSceneX() - node.getTranslateX();
            mouseY = event.getSceneY() - node.getTranslateY();
            event.consume();
        });

        node.setOnMouseDragged(event -> {
            node.setTranslateX(event.getSceneX() - mouseX);
            node.setTranslateY(event.getSceneY() - mouseY);
            for (WireHandler w : wireHandlers) {
                w.refresh();
            }

            event.consume();
        });
    }
}
