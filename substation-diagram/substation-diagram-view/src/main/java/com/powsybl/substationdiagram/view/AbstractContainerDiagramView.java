/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.commons.PowsyblException;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import com.powsybl.substationdiagram.view.app.VoltageLevelHandler;

import afester.javafx.svg.SvgLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Polyline;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public abstract class AbstractContainerDiagramView extends BorderPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContainerDiagramView.class);

    private double pressedX;
    private double pressedY;

    protected AbstractContainerDiagramView(Group svgImage) {
        super(svgImage);

        registerEvents(svgImage);
    }

    private void registerEvents(Group svgImage) {
        setOnScroll(event -> {
            double zoomFactor = 1.05;
            double deltaY = event.getDeltaY();
            if (deltaY < 0) {
                zoomFactor = 2.0 - zoomFactor;
            }
            setScaleX(getScaleX() * zoomFactor);
            setScaleY(getScaleY() * zoomFactor);

            event.consume();
        });

        setOnMousePressed(event -> {
            if (event.getButton().equals(MouseButton.SECONDARY)) {
                pressedX = -svgImage.getTranslateX() + event.getX();
                pressedY = -svgImage.getTranslateY() + event.getY();
            }
            event.consume();
        });
        setOnMouseDragged(event -> {
            if (event.getButton().equals(MouseButton.SECONDARY)) {
                svgImage.setTranslateX(event.getX() - pressedX);
                svgImage.setTranslateY(event.getY() - pressedY);
            }
            event.consume();
        });
    }

    private static void installHandlers(Node node, GraphMetadata metadata, Map<String, WireHandler> wireHandlers,
                                        Map<String, NodeHandler> nodeHandlers,
                                        Map<String, VoltageLevelHandler> vlHandlers) {
        if (!StringUtils.isEmpty(node.getId())) {
            GraphMetadata.NodeMetadata nodeMetadata = metadata.getNodeMetadata(node.getId());
            if (nodeMetadata != null) {
                if (node instanceof Group &&
                        (nodeMetadata.getComponentType() != null) &&
                        (nodeMetadata.getComponentType().equals(ComponentType.BREAKER) || nodeMetadata.getComponentType().equals(ComponentType.DISCONNECTOR) || nodeMetadata.getComponentType().equals(ComponentType.LOAD_BREAK_SWITCH))) {
                    setNodeVisibility((Group) node, nodeMetadata);
                }
                installNodeHandlers(node, metadata, nodeMetadata, nodeHandlers, vlHandlers);
            }
            GraphMetadata.WireMetadata wireMetadata = metadata.getWireMetadata(node.getId());
            if (wireMetadata != null) {
                installWireHandlers(node, metadata, wireMetadata, nodeHandlers, wireHandlers);
            }
            GraphMetadata.ArrowMetadata arrowMetadata = metadata.getArrowMetadata(node.getId());
            if (arrowMetadata != null) {
                WireHandler wireHandler = wireHandlers.get(arrowMetadata.getWireId());
                wireHandler.addArrow((Group) node);
            }
        }

        // propagate to children
        if (node instanceof Group) {
            ((Group) node).getChildren().forEach(child -> installHandlers(child, metadata, wireHandlers, nodeHandlers, vlHandlers));
        }
    }

    private static void installNodeHandlers(Node node, GraphMetadata metadata, GraphMetadata.NodeMetadata nodeMetadata, Map<String, NodeHandler> nodeHandlers, Map<String, VoltageLevelHandler> vlHandlers) {
        if (!nodeMetadata.isVLabel()) {
            NodeHandler nodeHandler = new NodeHandler(node, nodeMetadata.getComponentType(),
                                                      nodeMetadata.getRotationAngle(),
                                                      nodeMetadata.isRotated(), metadata,
                                                      nodeMetadata.getVId(),
                                                      nodeMetadata.getDirection());
            LOGGER.trace("Add handler to node {} in voltageLevel {}", node.getId(), nodeMetadata.getVId());
            nodeHandlers.put(node.getId(), nodeHandler);
        } else {  // handler for voltageLevel label
            VoltageLevelHandler vlHandler = new VoltageLevelHandler(node, metadata, nodeMetadata.getVId());
            LOGGER.trace("Add handler to voltageLvel label {}", node.getId());
            vlHandlers.put(nodeMetadata.getVId(), vlHandler);
        }
    }

    private static void installWireHandlers(Node node, GraphMetadata metadata, GraphMetadata.WireMetadata wireMetadata, Map<String, NodeHandler> nodeHandlers, Map<String, WireHandler> wireHandlers) {
        NodeHandler nodeHandler1 = nodeHandlers.get(wireMetadata.getNodeId1());
        if (nodeHandler1 == null) {
            throw new PowsyblException("Node 1 " + wireMetadata.getNodeId1() + " not found");
        }
        NodeHandler nodeHandler2 = nodeHandlers.get(wireMetadata.getNodeId2());
        if (nodeHandler2 == null) {
            throw new PowsyblException("Node 2 " + wireMetadata.getNodeId2() + " not found");
        }
        WireHandler wireHandler = new WireHandler((Polyline) node, nodeHandler1, nodeHandler2, wireMetadata.isStraight(),
                wireMetadata.isSnakeLine(), metadata);
        LOGGER.trace(" Added handler to wire between {} and {}", wireMetadata.getNodeId1(), wireMetadata.getNodeId2());
        wireHandlers.put(node.getId(), wireHandler);
    }

    private static void setNodeVisibility(Group node, GraphMetadata.NodeMetadata nodeMetadata) {
        node.getChildren().forEach(child -> child.setVisible((nodeMetadata.isOpen() && child.getId().equals("open")) || (!nodeMetadata.isOpen() && child.getId().equals("closed"))));
    }

    private static void installHandlers(Node node, GraphMetadata metadata) {
        Map<String, WireHandler> wireHandlers = new HashMap<>();
        Map<String, NodeHandler> nodeHandlers = new HashMap<>();
        Map<String, VoltageLevelHandler> vlHandlers = new HashMap<>();

        installHandlers(node, metadata, wireHandlers, nodeHandlers, vlHandlers);

        // resolve links
        for (WireHandler wireHandler : wireHandlers.values()) {
            wireHandler.getNodeHandler1().addWire(wireHandler);
            wireHandler.getNodeHandler2().addWire(wireHandler);
        }

        // resolve voltageLevel handler
        vlHandlers.values().forEach(v -> v.addNodeHandlers(nodeHandlers.values().stream().collect(Collectors.toList())));
    }

    protected static Group loadSvgAndMetadata(InputStream svgInputStream, InputStream metadataInputStream) {
        // convert svg file to JavaFX components
        Group svgImage = new SvgLoader().loadSvg(svgInputStream);

        // load metadata
        GraphMetadata metadata = GraphMetadata.parseJson(metadataInputStream);

        // install node and wire handlers to allow diagram edition
        installHandlers(svgImage, metadata);

        return svgImage;
    }
}
