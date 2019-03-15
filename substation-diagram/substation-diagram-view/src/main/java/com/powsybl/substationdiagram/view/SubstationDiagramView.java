/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.view;

import afester.javafx.svg.SvgLoader;
import com.powsybl.commons.PowsyblException;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.svg.GraphMetadata;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Polyline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class SubstationDiagramView extends BorderPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubstationDiagramView.class);

    private double pressedX;
    private double pressedY;

    private SubstationDiagramView(Group svgImage) {
        super(svgImage);

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

    public static void installHandlers(Node node, GraphMetadata metadata) {
        List<WireHandler> wireHandlers = new ArrayList<>();
        Map<String, NodeHandler> nodeHandlers = new HashMap<>();

        installHandlers(node, metadata, wireHandlers, nodeHandlers);

        // resolve links
        for (WireHandler wireHandler : wireHandlers) {
            wireHandler.getNodeHandler1().addWire(wireHandler);
            wireHandler.getNodeHandler2().addWire(wireHandler);
        }
    }

    private static void installHandlers(Node node, GraphMetadata metadata, List<WireHandler> wireHandlers,
                                        Map<String, NodeHandler> nodeHandlers) {
        if ((node.getId() != null) && !node.getId().isEmpty()) {
            GraphMetadata.NodeMetadata nodeMetadata = metadata.getNodeMetadata(node.getId());
            if (nodeMetadata != null) {
                if (node instanceof Group && (nodeMetadata.getComponentType().equals(ComponentType.BREAKER) || nodeMetadata.getComponentType().equals(ComponentType.DISCONNECTOR) || nodeMetadata.getComponentType().equals(ComponentType.LOAD_BREAK_SWITCH))) {
                    Group group = (Group) node;
                    for (Node child : group.getChildren()) {
                        if ((nodeMetadata.isOpen() && child.getId().equals("closed")) || (!nodeMetadata.isOpen() && child.getId().equals("open"))) {
                            child.setVisible(false);
                        }
                    }
                }
                NodeHandler nodeHandler = new NodeHandler(node, nodeMetadata.getComponentType(), nodeMetadata.isRotated(), metadata);
                LOGGER.trace("Add handler to node {}", node.getId());
                nodeHandlers.put(node.getId(), nodeHandler);
            } else {
                GraphMetadata.WireMetadata wireMetadata = metadata.getWireMetadata(node.getId());
                if (wireMetadata != null) {
                    NodeHandler nodeHandler1 = nodeHandlers.get(wireMetadata.getNodeId1());
                    if (nodeHandler1 == null) {
                        throw new PowsyblException("Node 1 " + wireMetadata.getNodeId1() + " not found");
                    }
                    NodeHandler nodeHandler2 = nodeHandlers.get(wireMetadata.getNodeId2());
                    if (nodeHandler2 == null) {
                        throw new PowsyblException("Node 2 " + wireMetadata.getNodeId2() + " not found");
                    }
                    WireHandler wireHandler = new WireHandler((Polyline) node, nodeHandler1, nodeHandler2, metadata);
                    LOGGER.trace(" Added handler to wire between {} and {}", wireMetadata.getNodeId1(), wireMetadata.getNodeId2());
                    wireHandlers.add(wireHandler);
                }
            }
        }

        // propagate to children
        if (node instanceof Group) {
            Group group = (Group) node;
            for (Node child : group.getChildren()) {
                installHandlers(child, metadata, wireHandlers, nodeHandlers);
            }
        }
    }

    public static SubstationDiagramView load(InputStream svgInputStream, InputStream metadataInputStream) {
        Objects.requireNonNull(svgInputStream);
        Objects.requireNonNull(metadataInputStream);

        // convert svg file to JavaFX components
        Group svgImage = new SvgLoader().loadSvg(svgInputStream);

        // load metadata
        GraphMetadata metadata = GraphMetadata.parseJson(metadataInputStream);

        // install node and wire handlers to allow diagram edition
        installHandlers(svgImage, metadata);

        return new SubstationDiagramView(svgImage);
    }

    public static SubstationDiagramView load(Path svgFile) {
        Objects.requireNonNull(svgFile);

        Path dir = svgFile.toAbsolutePath().getParent();
        String svgFileName = svgFile.getFileName().toString();
        Path metadataFile = dir.resolve(svgFileName.replace(".svg", "_metadata.json"));

        LOGGER.info("Load substation diagram: {} and {}", svgFile.toAbsolutePath(), metadataFile);

        try (InputStream svgInputStream = Files.newInputStream(svgFile);
             InputStream metadataInputStream = Files.newInputStream(metadataFile)) {
            return load(svgInputStream, metadataInputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
