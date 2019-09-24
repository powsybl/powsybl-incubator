/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import com.powsybl.commons.exceptions.UncheckedTransformerException;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.library.*;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.*;
import com.powsybl.substationdiagram.svg.GraphMetadata.ArrowMetadata;
import com.powsybl.substationdiagram.svg.SubstationDiagramInitialValueProvider.Direction;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.w3c.dom.svg.SVGElement;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.escapeClassName;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.escapeId;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class SVGWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SVGWriter.class);

    private static final String CLASS = "class";
    private static final String TRANSFORM = "transform";
    private static final String TRANSLATE = "translate";
    private static final int FONT_SIZE = 8;
    private static final String FONT_FAMILY = "Verdana";
    private static final double LABEL_OFFSET = 5d;
    private static final int FONT_VOLTAGE_LEVEL_LABEL_SIZE = 12;
    private static final String POLYLINE = "polyline";
    private static final String POINTS = "points";
    private static final String STROKE = "stroke";

    private final ComponentLibrary componentLibrary;

    private final LayoutParameters layoutParameters;

    public SVGWriter(ComponentLibrary componentLibrary, LayoutParameters layoutParameters) {
        this.componentLibrary = Objects.requireNonNull(componentLibrary);
        this.layoutParameters = Objects.requireNonNull(layoutParameters);
    }

    /**
     * Create the SVGDocument corresponding to the graph
     *
     * @param graph   graph
     * @param svgFile file
     */
    public GraphMetadata write(Graph graph, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider, Path svgFile) {
        try (Writer writer = Files.newBufferedWriter(svgFile)) {
            return write(graph, initProvider, styleProvider, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create the SVGDocument corresponding to the graph
     *
     * @param graph  graph
     * @param writer writer
     */
    public GraphMetadata write(Graph graph, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider, Writer writer) {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

        Document document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        Element style = document.createElement("style");

        StringBuilder graphStyle = new StringBuilder();
        Optional<String> globalStyle = styleProvider.getGlobalStyle(graph);
        globalStyle.ifPresent(graphStyle::append);
        graphStyle.append(componentLibrary.getStyleSheet());

        graph.getNodes().forEach(n -> {
            Optional<String> nodeStyle = styleProvider.getNodeStyle(n);
            nodeStyle.ifPresent(graphStyle::append);
        });
        graph.getEdges().forEach(e -> {
            Optional<String> wireStyle = styleProvider.getWireStyle(e);
            wireStyle.ifPresent(graphStyle::append);
        });
        CDATASection cd = document.createCDATASection(graphStyle.toString());
        style.appendChild(cd);

        document.adoptNode(style);
        document.getDocumentElement().appendChild(style);

        GraphMetadata metadata = writegraph(graph, document, initProvider, styleProvider);

        try {
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(writer);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new UncheckedTransformerException(e);
        }

        return metadata;
    }

    /**
     * Create the SVGDocument corresponding to the graph
     */
    private GraphMetadata writegraph(Graph graph, Document document, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider) {
        GraphMetadata metadata = new GraphMetadata();

        Element root = document.createElement("g");
        root.setAttribute(CLASS, SubstationDiagramStyles.SUBSTATION_STYLE_CLASS);

        if (layoutParameters.isShowGrid()) {
            root.appendChild(drawGrid(graph, document, metadata));
        }

        AnchorPointProvider anchorPointProvider = (type, id) -> {
            if (type == ComponentType.BUSBAR_SECTION) {
                BusNode busbarSectionNode = (BusNode) graph.getNode(id);
                List<AnchorPoint> result = new ArrayList<>();
                result.add(new AnchorPoint(0, 0, AnchorOrientation.HORIZONTAL));
                for (int i = 1; i < 2 * busbarSectionNode.getPosition().getHSpan(); i++) {
                    result.add(new AnchorPoint(
                            ((double) i / 2) * layoutParameters.getCellWidth() - layoutParameters.getHorizontalBusPadding() / 2,
                            0, AnchorOrientation.VERTICAL));
                }
                result.add(new AnchorPoint(busbarSectionNode.getPxWidth(), 0, AnchorOrientation.HORIZONTAL));
                return result;
            }
            return componentLibrary.getAnchorPoints(type);
        };

        drawNodes(root, graph, metadata, anchorPointProvider, initProvider, styleProvider);
        drawEdges(root, graph, metadata, anchorPointProvider, initProvider, styleProvider);

        // the drawing of the voltageLevel graph label is done at the end in order to
        // facilitate the move of a voltageLevel in the diagram
        drawGraphLabel(root, graph, metadata);
        document.adoptNode(root);
        document.getDocumentElement().appendChild(root);

        return metadata;
    }

    /**
     * Create the SVGDocument corresponding to the substation graph
     *
     * @param graph   substation graph
     * @param svgFile file
     */
    public GraphMetadata write(SubstationGraph graph, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider,
                               Path svgFile) {
        try (Writer writer = Files.newBufferedWriter(svgFile)) {
            return write(graph, initProvider, styleProvider, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create the SVGDocument corresponding to the substation graph
     *
     * @param graph  substation graph
     * @param writer writer
     */
    public GraphMetadata write(SubstationGraph graph, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider,
                               Writer writer) {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

        Document document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);

        GraphMetadata metadata = writegraph(graph, document, initProvider, styleProvider);

        try {
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(writer);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new UncheckedTransformerException(e);
        }

        return metadata;
    }

    /**
     * Create the SVGDocument corresponding to the substation graph
     */
    private GraphMetadata writegraph(SubstationGraph graph, Document document, SubstationDiagramInitialValueProvider initProvider,
                                     SubstationDiagramStyleProvider styleProvider) {
        GraphMetadata metadata = new GraphMetadata();

        Element root = document.createElement("g");
        root.setAttribute(CLASS, SubstationDiagramStyles.SUBSTATION_STYLE_CLASS);

        // Drawing grid lines
        if (layoutParameters.isShowGrid()) {
            for (Graph vlGraph : graph.getNodes()) {
                root.appendChild(drawGrid(vlGraph, document, metadata));
            }
        }

        // Drawing the voltageLevels
        for (Graph vlGraph : graph.getNodes()) {
            AnchorPointProvider anchorPointProvider = (type, id) -> {
                if (type == ComponentType.BUSBAR_SECTION) {
                    BusNode busbarSectionNode = (BusNode) vlGraph.getNode(id);
                    List<AnchorPoint> result = new ArrayList<>();
                    result.add(new AnchorPoint(0, 0, AnchorOrientation.HORIZONTAL));
                    for (int i = 1; i < 2 * busbarSectionNode.getPosition().getHSpan(); i++) {
                        result.add(new AnchorPoint(
                                ((double) i / 2) * layoutParameters.getCellWidth() - layoutParameters.getHorizontalBusPadding() / 2,
                                0, AnchorOrientation.VERTICAL));
                    }
                    result.add(new AnchorPoint(busbarSectionNode.getPxWidth(), 0, AnchorOrientation.HORIZONTAL));
                    return result;
                } else {
                    return componentLibrary.getAnchorPoints(type);
                }
            };
            drawNodes(root, vlGraph, metadata, anchorPointProvider, initProvider, styleProvider);
            drawEdges(root, vlGraph, metadata, anchorPointProvider, initProvider, styleProvider);
        }

        drawSnakeLines(root, graph, metadata);

        // the drawing of the voltageLevel graph labels is done at the end in order to
        // facilitate the move of a voltageLevel in the diagram
        for (Graph vlGraph : graph.getNodes()) {
            drawGraphLabel(root, vlGraph, metadata);
        }

        Element style = document.createElement("style");

        StringBuilder graphStyle = new StringBuilder();
        for (Graph vlGraph : graph.getNodes()) {
            Optional<String> globalStyle = styleProvider.getGlobalStyle(vlGraph);
            globalStyle.ifPresent(graphStyle::append);
        }
        graphStyle.append(componentLibrary.getStyleSheet());

        for (Graph vlGraph : graph.getNodes()) {
            vlGraph.getNodes().forEach(n -> {
                Optional<String> nodeStyle = styleProvider.getNodeStyle(n);
                nodeStyle.ifPresent(graphStyle::append);
            });
            vlGraph.getEdges().forEach(e -> {
                Optional<String> wireStyle = styleProvider.getWireStyle(e);
                wireStyle.ifPresent(graphStyle::append);
            });
        }

        CDATASection cd = document.createCDATASection(graphStyle.toString());
        style.appendChild(cd);

        document.adoptNode(style);
        document.getDocumentElement().appendChild(style);

        document.adoptNode(root);
        document.getDocumentElement().appendChild(root);

        return metadata;
    }

    /*
     * Drawing the grid lines (if required)
     */
    private Element drawGrid(Graph graph, Document document, GraphMetadata metadata) {
        int maxH = graph.getNodeBuses().stream()
                .mapToInt(nodeBus -> nodeBus.getPosition().getH() + nodeBus.getPosition().getHSpan())
                .max().orElse(0);
        int maxV = graph.getNodeBuses().stream()
                .mapToInt(nodeBus -> nodeBus.getPosition().getV())
                .max().orElse(1) - 1;

        Element gridRoot = document.createElement("g");
        String gridId = "GRID_" + graph.getVoltageLevel().getId();
        gridRoot.setAttribute("id", gridId);
        gridRoot.setAttribute(TRANSFORM,
                TRANSLATE + "(" + layoutParameters.getTranslateX() + "," + layoutParameters.getTranslateY() + ")");
        // vertical lines
        for (int i = 0; i < maxH + 1; i++) {
            gridRoot.appendChild(drawGridVerticalLine(document, graph, maxV,
                    graph.getX() + layoutParameters.getInitialXBus() + i * layoutParameters.getCellWidth()));
        }

        // StackHeight Horizontal lines
        gridRoot.appendChild(drawGridHorizontalLine(document, graph, maxH,
                graph.getY() + layoutParameters.getInitialYBus() - layoutParameters.getStackHeight()));
        gridRoot.appendChild(drawGridHorizontalLine(document, graph, maxH,
                graph.getY() + layoutParameters.getInitialYBus() + layoutParameters.getStackHeight()
                        + layoutParameters.getVerticalSpaceBus() * maxV));

        // internCellHeight Horizontal lines
        gridRoot.appendChild(drawGridHorizontalLine(document, graph, maxH,
                graph.getY() + layoutParameters.getInitialYBus() - layoutParameters.getInternCellHeight()));
        gridRoot.appendChild(drawGridHorizontalLine(document, graph, maxH,
                graph.getY() + layoutParameters.getInitialYBus() + layoutParameters.getInternCellHeight()
                        + layoutParameters.getVerticalSpaceBus() * maxV));

        metadata.addNodeMetadata(new GraphMetadata.NodeMetadata(gridId,
                graph.getVoltageLevel().getId(),
                null,
                null,
                null,
                false,
                BusCell.Direction.UNDEFINED,
                false));

        return gridRoot;
    }

    private Element drawGridHorizontalLine(Document document, Graph graph, int maxH, double y) {
        return drawGridLine(document,
                layoutParameters.getInitialXBus() + graph.getX(), y,
                layoutParameters.getInitialXBus() + maxH * layoutParameters.getCellWidth() + graph.getX(), y);
    }

    private Element drawGridVerticalLine(Document document, Graph graph, int maxV, double x) {
        return drawGridLine(document,
                x, layoutParameters.getInitialYBus()
                        - layoutParameters.getStackHeight() - layoutParameters.getExternCellHeight() + graph.getY(),
                x, layoutParameters.getInitialYBus()
                        + layoutParameters.getStackHeight() + layoutParameters.getExternCellHeight()
                        + layoutParameters.getVerticalSpaceBus() * maxV + graph.getY());
    }

    private Element drawGridLine(Document document, double x1, double y1, double x2, double y2) {
        Element line = document.createElement("line");
        line.setAttribute("x1", Double.toString(x1));
        line.setAttribute("x2", Double.toString(x2));
        line.setAttribute("y1", Double.toString(y1));
        line.setAttribute("y2", Double.toString(y2));
        line.setAttribute(CLASS, SubstationDiagramStyles.GRID_STYLE_CLASS);
        return line;
    }


    /*
     * Drawing the voltageLevel graph nodes
     */
    private void drawNodes(Element root, Graph graph, GraphMetadata metadata,
                           AnchorPointProvider anchorPointProvider, SubstationDiagramInitialValueProvider initProvider,
                           SubstationDiagramStyleProvider styleProvider) {
        graph.getNodes().forEach(node -> {
            try {
                String nodeId = SubstationDiagramStyles.escapeId(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name()));
                Element g = root.getOwnerDocument().createElement("g");
                g.setAttribute("id", nodeId);

                g.setAttribute(CLASS, SubstationDiagramStyles.SUBSTATION_STYLE_CLASS + " " + node.getComponentType() + " " + SubstationDiagramStyles.escapeId(nodeId));

                if (node.getType() == Node.NodeType.BUS) {
                    drawBus((BusNode) node, g);
                } else {
                    incorporateComponents(node, g, styleProvider);
                }

                BusCell.Direction direction = (node instanceof FeederNode && node.getCell() != null) ? ((ExternCell) node.getCell()).getDirection() : BusCell.Direction.UNDEFINED;

                if (!node.isFictitious()) {
                    drawNodeLabel(g, node, initProvider, direction);
                }
                root.appendChild(g);

                setMetadata(metadata, node, nodeId, graph, direction, anchorPointProvider);

            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void setMetadata(GraphMetadata metadata, Node node, String nodeId, Graph graph, BusCell.Direction direction, AnchorPointProvider anchorPointProvider) {
        String nextVId = null;
        if (node instanceof FeederBranchNode) {
            nextVId = ((FeederBranchNode) node).getVlOtherSide().getId();
        }

        metadata.addNodeMetadata(
                new GraphMetadata.NodeMetadata(nodeId, graph.getVoltageLevel().getId(), nextVId,
                        node.getComponentType(), node.getRotationAngle(),
                        node.isOpen(), direction, false));
        if (node.getType() == Node.NodeType.BUS) {
            metadata.addComponentMetadata(new ComponentMetadata(ComponentType.BUSBAR_SECTION,
                    nodeId,
                    anchorPointProvider.getAnchorPoints(ComponentType.BUSBAR_SECTION, node.getId()),
                    new ComponentSize(0, 0)));
        } else {
            if (metadata.getComponentMetadata(node.getComponentType()) == null) {
                metadata.addComponentMetadata(new ComponentMetadata(node.getComponentType(),
                        null,
                        componentLibrary.getAnchorPoints(node.getComponentType()),
                        componentLibrary.getSize(node.getComponentType())));
            }
        }
    }

    private void drawNodeLabel(Element g, Node node, SubstationDiagramInitialValueProvider initProvider, BusCell.Direction direction) {
        if (node instanceof FeederNode) {
            double yShift = -LABEL_OFFSET;
            if (node.getCell() != null) {
                yShift = direction == BusCell.Direction.TOP
                        ? -LABEL_OFFSET
                        : ((int) (componentLibrary.getSize(node.getComponentType()).getHeight()) + FONT_SIZE + LABEL_OFFSET);
            }
            drawLabel(node.getLabel(), node.isRotated(), -LABEL_OFFSET, yShift, g, FONT_SIZE);
        } else if (node instanceof BusNode) {
            InitialValue val = initProvider.getInitialValue(node);
            double d = ((BusNode) node).getPxWidth();
            Optional<String> label1 = val.getLabel1();
            if (label1.isPresent()) {
                drawLabel(label1.get(), false, -LABEL_OFFSET, -LABEL_OFFSET, g, FONT_SIZE);
            }
            Optional<String> label2 = val.getLabel2();
            if (label2.isPresent()) {
                drawLabel(label2.get(), false, d - LABEL_OFFSET, -LABEL_OFFSET, g, FONT_SIZE);
            }
            Optional<String> label3 = val.getLabel3();
            if (label3.isPresent()) {
                drawLabel(label3.get(), false, -LABEL_OFFSET, LABEL_OFFSET + (double) FONT_SIZE / 2, g, FONT_SIZE);
            }
            Optional<String> label4 = val.getLabel4();
            if (label4.isPresent()) {
                drawLabel(label4.get(), false, d - LABEL_OFFSET, LABEL_OFFSET + (double) FONT_SIZE / 2, g, FONT_SIZE);
            }
        }
    }

    /*
     * Drawing the graph label
     */
    private void drawGraphLabel(Element root, Graph graph, GraphMetadata metadata) {
        // drawing the label of the voltageLevel
        String idLabelVoltageLevel = "LABEL_VL_" + graph.getVoltageLevel().getId();
        Element gLabel = root.getOwnerDocument().createElement("g");
        gLabel.setAttribute("id", idLabelVoltageLevel);

        drawLabel(graph.isUseName()
                        ? graph.getVoltageLevel().getName()
                        : graph.getVoltageLevel().getId(),
                false, graph.getX(), graph.getY(), gLabel, FONT_VOLTAGE_LEVEL_LABEL_SIZE);
        root.appendChild(gLabel);

        metadata.addNodeMetadata(new GraphMetadata.NodeMetadata(idLabelVoltageLevel,
                graph.getVoltageLevel().getId(),
                null,
                null,
                null,
                false,
                BusCell.Direction.UNDEFINED,
                true));
    }

    /*
     * Drawing the voltageLevel graph busbar sections
     */
    private void drawBus(BusNode node, Element g) {
        Element line = g.getOwnerDocument().createElement("line");
        line.setAttribute("x1", "0");
        line.setAttribute("y1", "0");
        if (node.isRotated()) {
            line.setAttribute("x2", "0");
            line.setAttribute("y2", String.valueOf(node.getPxWidth()));
        } else {
            line.setAttribute("x2", String.valueOf(node.getPxWidth()));
            line.setAttribute("y2", "0");
        }
        line.setAttribute(CLASS, SubstationDiagramStyles.BUS_STYLE_CLASS + "_" + escapeClassName(node.getGraph().getVoltageLevel().getId()));

        g.appendChild(line);

        g.setAttribute(TRANSFORM, TRANSLATE + "(" + (layoutParameters.getTranslateX() + node.getX()) + ","
                + (layoutParameters.getTranslateY() + node.getY()) + ")");
    }

    /*
     * Drawing the voltageLevel graph busbar section names and feeder names
     */
    private void drawLabel(String str, boolean rotated, double xShift, double yShift, Element g,
                           int fontSize) {
        Element label = g.getOwnerDocument().createElement("text");
        label.setAttribute("x", String.valueOf(xShift));
        label.setAttribute("y", String.valueOf(yShift));
        label.setAttribute("font-family", FONT_FAMILY);
        label.setAttribute("font-size", Integer.toString(fontSize));
        label.setAttribute(CLASS, SubstationDiagramStyles.LABEL_STYLE_CLASS);
        Text text = g.getOwnerDocument().createTextNode(str);
        label.setAttribute(TRANSFORM, "rotate(" + (rotated ? -90 : 0) + "," + 0 + "," + 0 + ")");
        label.appendChild(text);
        g.appendChild(label);
    }

    private boolean canInsertComponentSVG(Node node) {
        return layoutParameters.isShowInternalNodes() ||
                ((!node.isFictitious() && node.getType() != Node.NodeType.SHUNT) ||
                        (node.isFictitious() && node.getComponentType() == ComponentType.THREE_WINDINGS_TRANSFORMER));
    }

    private void incorporateComponents(Node node, Element g, SubstationDiagramStyleProvider styleProvider) {
        SVGOMDocument obj = componentLibrary.getSvgDocument(node.getComponentType());
        transformComponent(node, g);
        if (obj != null && canInsertComponentSVG(node)) {
            insertComponentSVGIntoDocumentSVG(obj, g, node, styleProvider, componentLibrary.getSize(node.getComponentType()));
        }
    }

    /*
     * Handling the transformer SVG part (rotation, colorization)
     */
    private void handleTransformerSvgDocument(Node node, SubstationDiagramStyleProvider styleProvider,
                                              ComponentSize size, org.w3c.dom.Node n) {
        Optional<String> color = Optional.empty();

        VoltageLevel vl = node.getGraph().getVoltageLevel();

        // We will rotate the 3WT SVG, if cell orientation is BOTTOM
        boolean rotateSVG = node instanceof Fictitious3WTNode
                && node.getCell() != null
                && ((ExternCell) node.getCell()).getDirection() == BusCell.Direction.BOTTOM;

        if (((SVGElement) n).getId().equals("WINDING1")) {  // first winding
            if (node instanceof Fictitious3WTNode) {
                ThreeWindingsTransformer.Side otherSide = ThreeWindingsTransformer.Side.ONE;

                if (((Fictitious3WTNode) node).getTransformer().getLeg1().getTerminal().getVoltageLevel() == vl) {
                    otherSide = !rotateSVG ? ThreeWindingsTransformer.Side.TWO : ThreeWindingsTransformer.Side.THREE;
                } else if (((Fictitious3WTNode) node).getTransformer().getLeg2().getTerminal().getVoltageLevel() == vl) {
                    otherSide = !rotateSVG ? ThreeWindingsTransformer.Side.ONE : ThreeWindingsTransformer.Side.THREE;
                } else if (((Fictitious3WTNode) node).getTransformer().getLeg3().getTerminal().getVoltageLevel() == vl) {
                    otherSide = !rotateSVG ? ThreeWindingsTransformer.Side.ONE : ThreeWindingsTransformer.Side.TWO;
                }
                color = styleProvider.getNode3WTStyle((Fictitious3WTNode) node, otherSide);
            } else {
                color = styleProvider.getNode2WTStyle((Feeder2WTNode) node, TwoWindingsTransformer.Side.ONE);
            }
        } else if (((SVGElement) n).getId().equals("WINDING2")) {  // second winding
            if (node instanceof Fictitious3WTNode) {
                ThreeWindingsTransformer.Side otherSide = ThreeWindingsTransformer.Side.ONE;

                if (((Fictitious3WTNode) node).getTransformer().getLeg1().getTerminal().getVoltageLevel() == vl) {
                    otherSide = !rotateSVG ? ThreeWindingsTransformer.Side.THREE : ThreeWindingsTransformer.Side.TWO;
                } else if (((Fictitious3WTNode) node).getTransformer().getLeg2().getTerminal().getVoltageLevel() == vl) {
                    otherSide = !rotateSVG ? ThreeWindingsTransformer.Side.THREE : ThreeWindingsTransformer.Side.ONE;
                } else if (((Fictitious3WTNode) node).getTransformer().getLeg3().getTerminal().getVoltageLevel() == vl) {
                    otherSide = !rotateSVG ? ThreeWindingsTransformer.Side.TWO : ThreeWindingsTransformer.Side.ONE;
                }
                color = styleProvider.getNode3WTStyle((Fictitious3WTNode) node, otherSide);
            } else {
                color = styleProvider.getNode2WTStyle((Feeder2WTNode) node, TwoWindingsTransformer.Side.TWO);
            }
        } else if (((SVGElement) n).getId().equals("WINDING3") && node instanceof Fictitious3WTNode) {  // third winding
            if (((Fictitious3WTNode) node).getTransformer().getLeg1().getTerminal().getVoltageLevel() == vl) {
                color = styleProvider.getNode3WTStyle((Fictitious3WTNode) node, ThreeWindingsTransformer.Side.ONE);
            } else if (((Fictitious3WTNode) node).getTransformer().getLeg2().getTerminal().getVoltageLevel() == vl) {
                color = styleProvider.getNode3WTStyle((Fictitious3WTNode) node, ThreeWindingsTransformer.Side.TWO);
            } else if (((Fictitious3WTNode) node).getTransformer().getLeg3().getTerminal().getVoltageLevel() == vl) {
                color = styleProvider.getNode3WTStyle((Fictitious3WTNode) node, ThreeWindingsTransformer.Side.THREE);
            }
        }

        // Setting the stroke color for SVG element
        if (color.isPresent()) {
            ((Element) n).removeAttribute(STROKE);
            ((Element) n).setAttribute(STROKE, color.get());
        }

        if (rotateSVG) {  // SVG element rotation
            ((Element) n).setAttribute(TRANSFORM, "rotate(" + 180 + "," + size.getWidth() / 2 + "," + size.getHeight() / 2 + ")");
            // We store the rotation angle in order to transform correctly the anchor points when further drawing the edges
            node.setRotationAngle(180.);
        }
    }

    /*
     * Handling the inductor SVG part (colorization)
     */
    private void handleInductorSvgDocument(Node node, SubstationDiagramStyleProvider styleProvider, org.w3c.dom.Node n) {
        Optional<String> color = styleProvider.getColor(((Feeder2WTNode) node).getVlOtherSide());

        if (color.isPresent()) {
            ((Element) n).removeAttribute(STROKE);
            ((Element) n).setAttribute(STROKE, color.get());
        }
    }

    private void insertComponentSVGIntoDocumentSVG(SVGOMDocument obj, Element g, Node node,
                                                   SubstationDiagramStyleProvider styleProvider,
                                                   ComponentSize size) {
        // The following code work correctly considering SVG part describing the component is the first child of "obj" the SVGDocument.
        // If SVG are written otherwise, it will not work correctly.
        for (int i = 0; i < obj.getChildNodes().item(0).getChildNodes().getLength(); i++) {
            org.w3c.dom.Node n = obj.getChildNodes().item(0).getChildNodes().item(i).cloneNode(true);

            if (n instanceof SVGElement) {
                if (node instanceof Fictitious3WTNode ||
                        (node instanceof Feeder2WTNode && node.getComponentType() == ComponentType.TWO_WINDINGS_TRANSFORMER)) {
                    handleTransformerSvgDocument(node, styleProvider, size, n);
                } else if (node instanceof Feeder2WTNode && node.getComponentType() == ComponentType.INDUCTOR) {
                    handleInductorSvgDocument(node, styleProvider, n);
                }
            }

            g.getOwnerDocument().adoptNode(n);
            g.appendChild(n);
        }
    }

    private void insertRotatedComponentSVGIntoDocumentSVG(SVGOMDocument obj, Element g, double angle, double cx, double cy) {
        // The following code work correctly considering SVG part describing the component is the first child of "obj" the SVGDocument.
        // If SVG are written otherwise, it will not work correctly.

        for (int i = 0; i < obj.getChildNodes().item(0).getChildNodes().getLength(); i++) {
            org.w3c.dom.Node n = obj.getChildNodes().item(0).getChildNodes().item(i).cloneNode(true);
            if (n.getNodeName().equals("g") && n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                Element e = (Element) n;
                e.setAttribute(TRANSFORM, "rotate(" + angle + "," + cx + "," + cy + ")");
            }
            g.getOwnerDocument().adoptNode(n);
            g.appendChild(n);
        }
    }

    private void transformComponent(Node node, Element g) {
        ComponentSize componentSize = componentLibrary.getSize(node.getComponentType());

        if (!node.isRotated()) {
            g.setAttribute(TRANSFORM,
                    TRANSLATE + "(" + (layoutParameters.getTranslateX() + node.getX() - componentSize.getWidth() / 2) + ","
                            + (layoutParameters.getTranslateY() + node.getY() - componentSize.getHeight() / 2) + ")");
            return;
        }

/*
        afester javafx library does not handle more than one transformation, yet, so
        combine the couple of transformations, translation+rotation, in a single matrix transformation
*/
        int precision = 4;

        double angle = Math.toRadians(node.isRotated() ? 90 : 0);
        double cosRo = Math.cos(angle);
        double sinRo = Math.sin(angle);
        double cdx = componentSize.getWidth() / 2;
        double cdy = componentSize.getHeight() / 2;

        double e1 = layoutParameters.getTranslateX() - cdx * cosRo + cdy * sinRo + node.getX();
        double f1 = layoutParameters.getTranslateY() - cdx * sinRo - cdy * cosRo + node.getY();

        g.setAttribute(TRANSFORM,
                "matrix(" + Precision.round(cosRo, precision) + "," + Precision.round(sinRo, precision)
                        + "," + Precision.round(-sinRo, precision) + "," + Precision.round(cosRo,
                        precision) + ","
                        + Precision.round(e1, precision) + "," + Precision.round(f1, precision) + ")");
    }

    private void transformArrow(List<Double> points, ComponentSize componentSize, double shift, Element g) {

        double x1 = points.get(0);
        double y1 = points.get(1);
        double x2 = points.get(2);
        double y2 = points.get(3);

        if (points.size() > 4 && Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) < 3 * componentSize.getHeight()) {
            double x3 = points.get(4);
            double y3 = points.get(5);
            if (Math.sqrt((x3 - x2) * (x3 - x2) + (y3 - y2) * (y3 - y2)) > 3 * componentSize.getHeight()) {
                x1 = x2;
                y1 = y2;
                x2 = x3;
                y2 = y3;
            }
        }
        double dx = x2 - x1;
        double dy = y2 - y1;

        double angle = Math.atan(dx / dy);
        if (!Double.isNaN(angle)) {
            double cosRo = Math.cos(angle);
            double sinRo = Math.sin(angle);
            double cdx = componentSize.getWidth() / 2;
            double cdy = componentSize.getHeight() / 2;

            double dist = this.layoutParameters.getArrowDistance();

            double x = x1 + sinRo * (dist + shift);
            double y = y1 + cosRo * (y1 > y2 ? -(dist + shift) : (dist + shift));

            double e1 = layoutParameters.getTranslateX() - cdx * cosRo + cdy * sinRo + x;
            double f1 = layoutParameters.getTranslateY() - cdx * sinRo - cdy * cosRo + y;

            int precision = 4;
            g.setAttribute(TRANSFORM,
                    "matrix(" + Precision.round(cosRo, precision) + "," + Precision.round(sinRo, precision)
                            + "," + Precision.round(-sinRo, precision) + "," + Precision.round(cosRo,
                            precision) + ","
                            + Precision.round(e1, precision) + "," + Precision.round(f1, precision) + ")");
        }
    }

    private void insertArrowsAndLabels(String wireId, List<Double> points, Element root, Node n, GraphMetadata metadata, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider) {
        InitialValue init = initProvider.getInitialValue(n);
        ComponentMetadata cd = metadata.getComponentMetadata(ComponentType.ARROW);

        double shX = cd.getSize().getWidth() + LABEL_OFFSET;
        double shY = cd.getSize().getHeight() - LABEL_OFFSET + (double) FONT_SIZE / 2;

        Element g1 = root.getOwnerDocument().createElement("g");
        g1.setAttribute("id", wireId + "_ARROW1");
        SVGOMDocument arr = componentLibrary.getSvgDocument(ComponentType.ARROW);
        transformArrow(points, cd.getSize(), 0, g1);
        double y1 = points.get(1);
        double y2 = points.get(3);
        if (y1 > y2) {
            insertRotatedComponentSVGIntoDocumentSVG(arr, g1, 180, cd.getSize().getWidth() / 2, cd.getSize().getHeight() / 2);
        } else {
            insertComponentSVGIntoDocumentSVG(arr, g1, n, styleProvider, componentLibrary.getSize(n.getComponentType()));
        }
        Optional<String> label1 = init.getLabel1();
        if (label1.isPresent()) {
            drawLabel(label1.get(), false, shX, shY, g1, FONT_SIZE);
        }
        Optional<Direction> dir1 = init.getArrowDirection1();
        if (dir1.isPresent()) {
            try {
                g1.setAttribute(CLASS, SubstationDiagramStyles.SUBSTATION_STYLE_CLASS + " " + "ARROW1_" + escapeId(URLEncoder.encode(n.getId(), StandardCharsets.UTF_8.name())) + "_" + dir1.get());
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
        root.appendChild(g1);
        metadata.addArrowMetadata(new ArrowMetadata(wireId + "_ARROW1", wireId, layoutParameters.getArrowDistance()));

        Element g2 = root.getOwnerDocument().createElement("g");
        g2.setAttribute("id", wireId + "_ARROW2");
        transformArrow(points, cd.getSize(), 2 * cd.getSize().getHeight(), g2);
        if (y1 > y2) {
            insertRotatedComponentSVGIntoDocumentSVG(arr, g2, 180, 5, 5);
        } else {
            insertComponentSVGIntoDocumentSVG(arr, g2, n, styleProvider, componentLibrary.getSize(n.getComponentType()));
        }
        Optional<String> label2 = init.getLabel2();
        if (label2.isPresent()) {
            drawLabel(label2.get(), false, shX, shY, g2, FONT_SIZE);
        }
        Optional<Direction> dir2 = init.getArrowDirection2();
        if (dir2.isPresent()) {
            try {
                g2.setAttribute(CLASS, SubstationDiagramStyles.SUBSTATION_STYLE_CLASS + " " + "ARROW2_" + escapeClassName(URLEncoder.encode(n.getId(), StandardCharsets.UTF_8.name())) + "_" + dir2.get());
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
        Optional<String> label3 = init.getLabel3();
        if (label3.isPresent()) {
            drawLabel(label3.get(), false, -(label3.get().length() * (double) FONT_SIZE / 2 + LABEL_OFFSET), shY, g1, FONT_SIZE);
        }
        Optional<String> label4 = init.getLabel4();
        if (label4.isPresent()) {
            drawLabel(label4.get(), false, -(label4.get().length() * (double) FONT_SIZE / 2 + LABEL_OFFSET), shY, g2, FONT_SIZE);
        }

        root.appendChild(g2);
        metadata.addArrowMetadata(new ArrowMetadata(wireId + "_ARROW2", wireId, layoutParameters.getArrowDistance()));
    }

    /*
     * Drawing the voltageLevel graph edges
     */
    private void drawEdges(Element root, Graph graph, GraphMetadata metadata, AnchorPointProvider anchorPointProvider, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider) {
        String vId = graph.getVoltageLevel().getId();
        try {
            for (Edge edge : graph.getEdges()) {
                // for unicity purpose (in substation diagram), we prefix the id of the WireMetadata with the voltageLevel id and "_"
                String wireId = escapeId(URLEncoder.encode(vId + "_Wire" + graph.getEdges().indexOf(edge), StandardCharsets.UTF_8.name()));

                Element g = root.getOwnerDocument().createElement(POLYLINE);
                g.setAttribute("id", wireId);

                WireConnection anchorPoints = WireConnection.searchBetterAnchorPoints(anchorPointProvider, edge.getNode1(), edge.getNode2());

                // Determine points of the polyline
                List<Double> pol = anchorPoints.calculatePolylinePoints(edge.getNode1(), edge.getNode2(),
                        layoutParameters.isDrawStraightWires());

                g.setAttribute(POINTS, pointsListToString(pol));
                g.setAttribute(CLASS, styleProvider.getIdWireStyle(edge));
                root.appendChild(g);

                metadata.addWireMetadata(new GraphMetadata.WireMetadata(wireId,
                        escapeClassName(URLEncoder.encode(edge.getNode1().getId(), StandardCharsets.UTF_8.name())),
                        escapeClassName(URLEncoder.encode(edge.getNode2().getId(), StandardCharsets.UTF_8.name())),
                        layoutParameters.isDrawStraightWires(),
                        false));

                if (metadata.getComponentMetadata(ComponentType.ARROW) == null) {
                    metadata.addComponentMetadata(new ComponentMetadata(ComponentType.ARROW,
                            null,
                            componentLibrary.getAnchorPoints(ComponentType.ARROW),
                            componentLibrary.getSize(ComponentType.ARROW)));
                }

                if (edge.getNode1() instanceof FeederNode) {
                    if (!(edge.getNode2() instanceof FeederNode)) {
                        insertArrowsAndLabels(wireId, pol, root, edge.getNode1(), metadata, initProvider, styleProvider);
                    }
                } else if (edge.getNode2() instanceof FeederNode) {
                    insertArrowsAndLabels(wireId, pol, root, edge.getNode2(), metadata, initProvider, styleProvider);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /*
     * Drawing the substation graph edges (snakelines between voltageLevel diagram)
     */
    private void drawSnakeLines(Element root, SubstationGraph graph, GraphMetadata metadata) {

        for (TwtEdge edge : graph.getEdges()) {
            String vId1 = edge.getNode1().getGraph().getVoltageLevel().getId();
            String vId2 = edge.getNode2().getGraph().getVoltageLevel().getId();
            try {
                String wireId = escapeId(URLEncoder.encode(vId1 + "_" + vId2 + "_" + "Wire" + graph.getEdges().indexOf(edge), StandardCharsets.UTF_8.name()));
                Element g = root.getOwnerDocument().createElement(POLYLINE);
                g.setAttribute("id", wireId);

                // Get points of the snakeLine
                List<Double> pol = edge.getSnakeLine();

                g.setAttribute(POINTS, pointsListToString(pol));

                String vId;
                if (edge.getNode1().getGraph().getVoltageLevel().getNominalV() > edge.getNode2().getGraph().getVoltageLevel().getNominalV()) {
                    vId = vId1;
                } else {
                    vId = vId2;
                }

                g.setAttribute(CLASS, SubstationDiagramStyles.WIRE_STYLE_CLASS + "_" + escapeClassName(vId));
                root.appendChild(g);

                metadata.addWireMetadata(new GraphMetadata.WireMetadata(wireId,
                        escapeClassName(URLEncoder.encode(edge.getNode1().getId(), StandardCharsets.UTF_8.name())),
                        escapeClassName(URLEncoder.encode(edge.getNode2().getId(), StandardCharsets.UTF_8.name())),
                        layoutParameters.isDrawStraightWires(),
                        true));
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }

            if (metadata.getComponentMetadata(ComponentType.ARROW) == null) {
                metadata.addComponentMetadata(new ComponentMetadata(ComponentType.ARROW,
                        null,
                        componentLibrary.getAnchorPoints(ComponentType.ARROW),
                        componentLibrary.getSize(ComponentType.ARROW)));
            }
        }
    }

    private String pointsListToString(List<Double> pol) {

        return IntStream.range(0, pol.size())
                .mapToObj(n -> n % 2 == 0 ? pol.get(n) + layoutParameters.getTranslateX() : pol.get(n) + layoutParameters.getTranslateY())
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

}
