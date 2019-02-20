/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.powsybl.substationdiagram.library.ComponentSize;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.BaseNode;
import com.powsybl.substationdiagram.svg.GraphMetadata;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
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

    private final NavigationListener navigationListener;
    private StyleHandler styleHandler;

    public NodeHandler(Node node, ComponentType componentType, boolean rotated, GraphMetadata metadata, NavigationListener navigationListener, StyleHandler styleHandler) {
        this.node = Objects.requireNonNull(node);
        this.componentType = Objects.requireNonNull(componentType);
        this.rotated = rotated;
        this.metadata = Objects.requireNonNull(metadata);
        this.navigationListener = navigationListener;
        this.styleHandler = styleHandler;
        setDragAndDrop();
        if (navigationListener != null) {
            setClick();
        }
        Label label1 = new Label(node.getId());

        if (styleHandler != null) {
            String style = styleHandler.getNodeStyle(node.getId(), componentType);
            this.node.setStyle(style);

            if (node instanceof javafx.scene.Group) {
                javafx.scene.Group g = (javafx.scene.Group) node;
                g.getChildren().forEach(x -> x.setStyle(style));
            }

        }
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
        setwireLabels(w);
    }

    public void setwireLabels(WireHandler w) {
        Node node1 = w.getNodeHandler1().getNode();
        Node node2 = w.getNodeHandler2().getNode();

        if (styleHandler != null && !this.componentType.equals(ComponentType.NODE) && !this.componentType.equals(ComponentType.BUSBAR_SECTION) && !this.componentType.equals(ComponentType.DISCONNECTOR) && !this.componentType.equals(ComponentType.BREAKER) && !this.componentType.equals(ComponentType.LOAD_BREAK_SWITCH)) {
            Double[] pows = styleHandler.getPowers(w.getNode().getId(), this.node.getId(), this.getComponentType());

            String p = "inv";
            if (pows[0] != null) {
                p = String.valueOf(Math.round(pows[0].doubleValue()));
            }
            String q = "inv";
            if (pows[1] != null) {
                q =  String.valueOf(Math.round(pows[1].doubleValue()));
            }

            double center1X = w.getNodeHandler1().getX();
            double center1Y = w.getNodeHandler1().getY();
            double center2X = w.getNodeHandler2().getX();
            double center2Y = w.getNodeHandler2().getY();

            double relocateX = 0;
            double relocateY = 0;
            double coefficient = 0.5;
            if (center1X == center2X) {
                relocateY = (center2Y - center1Y) / 2;
            } else if (center1Y == center2Y) {
                relocateX = (center2X - center1X) / 2;
            }
            if (node.getId().equals(w.getNodeHandler2().getNode().getId())) {
                relocateX = -relocateX;
                relocateY = -relocateY;
            }
            if (relocateX < 0) {
                coefficient = 1.5;
            }
            if (relocateY < 0) {
                coefficient = 1.5;
            }
            Image image = null;
            Image image2 = null;
            if (p != null && !p.equals("0")) {
                image = (pows[0] > 0 && relocateY > 0) || (pows[0] < 0 && relocateY < 0) ? new Image(getClass().getResourceAsStream("arrow-up-black.png"))
                    : new Image(getClass().getResourceAsStream("arrow-down-black.png"));
            }
            if (q != null && !q.equals("0")) {
                image2 = (pows[1] > 0 && relocateY > 0) || (pows[1] < 0 && relocateY < 0) ? new Image(getClass().getResourceAsStream("arrow-up-blue.png"))
                    : new Image(getClass().getResourceAsStream("arrow-down-blue.png"));
            }
            Label label1 = new Label(p);
            label1.setLabelFor(w.getNode());
            if (image != null) {
                label1.setGraphic(new ImageView(image));
                label1.setTranslateX(relocateX * coefficient);
                label1.setTranslateY(relocateY * coefficient);
            } else {
                label1.setTranslateX(relocateX * coefficient + 16);
                label1.setTranslateY(relocateY * coefficient);
            }
            Label label2 = new Label(q);
            label2.setLabelFor(w.getNode());
            if (image2 != null) {
                label2.setGraphic(new ImageView(image2));
                label2.setTranslateX(relocateX);
                label2.setTranslateY(relocateY);
            } else {
                label2.setTranslateX(relocateX + 16);
                label2.setTranslateY(relocateY);
            }
            Node parent = w.getNode().getParent();
            if (parent instanceof javafx.scene.Group) {
                javafx.scene.Group g = (javafx.scene.Group) node;
                g.getChildren().addAll(label1, label2);
            }
        }
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

    public void setClick() {
        if (componentType.equals(ComponentType.LINE) || componentType.equals(ComponentType.TWO_WINDINGS_TRANSFORMER)) {
            node.setOnMouseClicked(event -> {
                navigationListener.onNavigationEvent(node.getId(), componentType);
            });
            node.setOnMouseEntered(event -> {
                node.setCursor(Cursor.HAND);
            });
            node.setOnMouseExited(event -> {
                node.setCursor(Cursor.DEFAULT);
            });
            String next = navigationListener.getDestination(node.getId(), componentType);
            Tooltip t = new Tooltip("=> " + next);
            Tooltip.install(node, t);
        }
    }
}
