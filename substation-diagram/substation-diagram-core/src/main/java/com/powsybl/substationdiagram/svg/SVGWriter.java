/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.powsybl.commons.exceptions.UncheckedTransformerException;
import com.powsybl.substationdiagram.layout.HorizontalSubstationLayout;
import com.powsybl.substationdiagram.layout.HorizontalSubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.layout.SubstationLayout;
import com.powsybl.substationdiagram.layout.SubstationLayoutFactory;
import com.powsybl.substationdiagram.layout.VerticalSubstationLayout;
import com.powsybl.substationdiagram.layout.VoltageLevelLayout;
import com.powsybl.substationdiagram.layout.VoltageLevelLayoutFactory;
import com.powsybl.substationdiagram.library.AnchorOrientation;
import com.powsybl.substationdiagram.library.AnchorPoint;
import com.powsybl.substationdiagram.library.AnchorPointProvider;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ComponentMetadata;
import com.powsybl.substationdiagram.library.ComponentSize;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.BusNode;
import com.powsybl.substationdiagram.model.Cell;
import com.powsybl.substationdiagram.model.Coord;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.FeederNode;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.SubstationGraph;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SVGWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SVGWriter.class);

    private static final String CLASS = "class";
    private static final String TRANSFORM = "transform";
    private static final String TRANSLATE = "translate";
    private static final int FONT_SIZE = 8;
    private static final String FONT_FAMILY = "Verdana";
    private static final int LABEL_OFFSET = 5;

    private final ComponentLibrary componentLibrary;

    private final LayoutParameters layoutParameters;

    public SVGWriter(ComponentLibrary componentLibrary, LayoutParameters layoutParameters) {
        this.componentLibrary = Objects.requireNonNull(componentLibrary);
        this.layoutParameters = Objects.requireNonNull(layoutParameters);
    }

    /**
     * Create the SVGDocument corresponding to the graph
     *
     * @param graph  graph
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

        GraphMetadata metadata = writegraph(graph, document, initProvider);

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
    private GraphMetadata writegraph(Graph graph, Document document, SubstationDiagramInitialValueProvider initProvider) {
        GraphMetadata metadata = new GraphMetadata();

        Element root = document.createElement("g");
        root.setAttribute(CLASS, SubstationDiagramStyles.SUBSTATION_STYLE_CLASS);

        if (layoutParameters.isShowGrid()) {
            root.appendChild(drawGrid(graph, document));
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

        drawNodes(root, graph, metadata, anchorPointProvider, initProvider);
        drawEdges(root, graph, metadata, anchorPointProvider, initProvider);

        document.adoptNode(root);
        document.getDocumentElement().appendChild(root);

        return metadata;
    }

    /**
     * Create the SVGDocument corresponding to the substation graph
     *
     * @param graph  substation graph
     * @param svgFile file
     */
    public GraphMetadata write(SubstationGraph graph, SubstationDiagramInitialValueProvider initProvider, SubstationDiagramStyleProvider styleProvider,
                               Path svgFile) {
        try (Writer writer = Files.newBufferedWriter(svgFile)) {
            return write(graph, initProvider, styleProvider, writer, new HorizontalSubstationLayoutFactory(), new PositionVoltageLevelLayoutFactory());
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
                               Writer writer, SubstationLayoutFactory sLayoutFactory,
                               VoltageLevelLayoutFactory vLayoutFactory) {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

        Document document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);

        Element style = document.createElement("style");

        StringBuilder graphStyle = new StringBuilder();
        Optional<String> globalStyle = styleProvider.getGlobalStyle(graph.getNodes().get(0));
        globalStyle.ifPresent(graphStyle::append);
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

        GraphMetadata metadata = writegraph(graph, document, sLayoutFactory, vLayoutFactory, initProvider);

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
    private GraphMetadata writegraph(SubstationGraph graph,
                                     Document document, SubstationLayoutFactory sLayoutFactory,
                                     VoltageLevelLayoutFactory vLayoutFactory,  SubstationDiagramInitialValueProvider initProvider) {
        GraphMetadata metadata = new GraphMetadata();

        SubstationLayout sLayout = sLayoutFactory.create(graph);

        Element root = document.createElement("g");
        root.setAttribute(CLASS, SubstationDiagramStyles.SUBSTATION_STYLE_CLASS);

        double graphX = layoutParameters.getHorizontalSubstationPadding();
        double graphY = layoutParameters.getVerticalSubstationPadding();

        for (Graph vlGraph : graph.getNodes()) {
            vlGraph.setX(graphX);
            vlGraph.setY(graphY);

            // Calculate the objects coordinates inside the voltageLevel graph
            VoltageLevelLayout vLayout = vLayoutFactory.create(vlGraph);
            vLayout.run(layoutParameters);

            // Calculate the coordinate of the voltageLevel graph inside the substation graph
            Coord posVLGraph = sLayout.run(layoutParameters, vlGraph);

            graphX += posVLGraph.getX() + (sLayout instanceof HorizontalSubstationLayout ? layoutParameters.getHorizontalSubstationPadding() : 0);
            graphY += posVLGraph.getY() + (sLayout instanceof VerticalSubstationLayout ? layoutParameters.getVerticalSubstationPadding() : 0);
        }

        // Drawing grid lines
        if (layoutParameters.isShowGrid()) {
            for (Graph vlGraph : graph.getNodes()) {
                root.appendChild(drawGrid(vlGraph, document));
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
            drawNodes(root, vlGraph, metadata, anchorPointProvider, initProvider);
            drawEdges(root, vlGraph, metadata, anchorPointProvider, initProvider);
        }

        // Drawing the lines between the voltageLevels
        AnchorPointProvider anchorPointProvider = (type, id) ->
            componentLibrary.getAnchorPoints(type);

        drawEdges(root, graph, metadata, anchorPointProvider, initProvider);

        document.adoptNode(root);
        document.getDocumentElement().appendChild(root);

        return metadata;
    }

    /*
     * Drawing the grid lines (if required)
     */
    private Element drawGrid(Graph graph, Document document) {
        int maxH = graph.getNodeBuses().stream()
                .mapToInt(nodeBus -> nodeBus.getPosition().getH() + nodeBus.getPosition().getHSpan())
                .max().orElse(0);
        int maxV = graph.getNodeBuses().stream()
                .mapToInt(nodeBus -> nodeBus.getPosition().getV())
                .max().orElse(0);
        Element gridRoot = document.createElement("g");
        for (int i = 0; i < maxH + 1; i++) {
            Element line = document.createElement("line");
            line.setAttribute("x1",
                              Double.toString(layoutParameters.getInitialXBus() + i * layoutParameters.getCellWidth() + graph.getX()));
            line.setAttribute("x2",
                              Double.toString(layoutParameters.getInitialXBus() + i * layoutParameters.getCellWidth() + graph.getX()));
            line.setAttribute("y1",
                              Double.toString(layoutParameters.getInitialYBus() - layoutParameters.getStackHeight()
                                                      - layoutParameters.getExternCellHeight() + graph.getY()));
            line.setAttribute("y2", Double.toString(
                    layoutParameters.getInitialYBus() + layoutParameters.getStackHeight() + layoutParameters.getExternCellHeight()
                            + layoutParameters.getVerticalSpaceBus() * maxV + graph.getY()));
            line.setAttribute(CLASS, SubstationDiagramStyles.GRID_STYLE_CLASS);

            line.setAttribute(TRANSFORM,
                              TRANSLATE + "(" + layoutParameters.getTranslateX() + "," + layoutParameters.getTranslateY() + ")");
            gridRoot.appendChild(line);
        }
        return gridRoot;
    }

    /*
     * Drawing the graph nodes
     */
    private void drawNodes(Element root, Graph graph, GraphMetadata metadata, AnchorPointProvider anchorPointProvider, SubstationDiagramInitialValueProvider initProvider) {
        graph.getNodes().forEach(node -> {
            try {
                String nodeId = URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name());
                Element g = root.getOwnerDocument().createElement("g");
                g.setAttribute("id", nodeId);

                g.setAttribute(CLASS, SubstationDiagramStyles.SUBSTATION_STYLE_CLASS + " " + node.getComponentType() + " " + SubstationDiagramStyles.escapeClassName(nodeId));

                if (node.getType() == Node.NodeType.BUS) {
                    drawBus((BusNode) node, g);
                } else {
                    incorporateComponents(node, g);
                }
                if (!node.isFictitious()) {
                    if (node instanceof FeederNode) {
                        int yShift = -LABEL_OFFSET;
                        if (node.getCell() != null) {
                            yShift = node.getCell().getDirection() == Cell.Direction.TOP
                                    ? -LABEL_OFFSET
                                    : ((int) (componentLibrary.getSize(node.getComponentType()).getHeight()) + FONT_SIZE + LABEL_OFFSET);
                        }
                        drawLabel(node.getLabel(), node.isRotated(), -LABEL_OFFSET, yShift, g);
                    } else if (node instanceof BusNode) {
                        InitialValue val = initProvider.getInitialValue(node);
                        int d = (int) ((BusNode) node).getPxWidth();
                        if (val.getLabel1().isPresent()) {
                            drawLabel(val.getLabel1().get(), false, -LABEL_OFFSET, -LABEL_OFFSET, g);
                        }
                        if (val.getLabel2().isPresent()) {
                            drawLabel(val.getLabel2().get(), false,  d - LABEL_OFFSET, -LABEL_OFFSET, g);
                        }
                        if (val.getLabel3().isPresent()) {
                            drawLabel(val.getLabel3().get(), false, -LABEL_OFFSET, LABEL_OFFSET + FONT_SIZE / 2, g);
                        }
                        if (val.getLabel4().isPresent()) {
                            drawLabel(val.getLabel4().get(), false, d - LABEL_OFFSET, LABEL_OFFSET + FONT_SIZE / 2, g);
                        }
                    }
                }
                root.appendChild(g);

                metadata.addNodeMetadata(
                        new GraphMetadata.NodeMetadata(nodeId, node.getComponentType(), node.isRotated(), node.isOpen()));
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
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

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
        line.setAttribute(CLASS, SubstationDiagramStyles.BUS_STYLE_CLASS);

        g.appendChild(line);

        g.setAttribute(TRANSFORM, TRANSLATE + "(" + (layoutParameters.getTranslateX() + node.getX()) + ","
                + (layoutParameters.getTranslateY() + node.getY()) + ")");
    }

    private void drawLabel(String str, boolean rotated, int xShift, int yShift, Element g) {
        Element label = g.getOwnerDocument().createElement("text");
        label.setAttribute("x", Integer.toString(xShift));
        label.setAttribute("y", Integer.toString(yShift));
        label.setAttribute("font-family", FONT_FAMILY);
        label.setAttribute("font-size", Integer.toString(FONT_SIZE));
        label.setAttribute(CLASS, SubstationDiagramStyles.LABEL_STYLE_CLASS);
        Text text = g.getOwnerDocument().createTextNode(str);
        label.setAttribute(TRANSFORM, "rotate(" + (rotated ? -90 : 0) + "," + 0 + "," + 0 + ")");
        label.appendChild(text);
        g.appendChild(label);
    }

    private boolean canInsertComponentSVG(Node node) {
        return layoutParameters.isShowInternalNodes() ||
                (!node.isFictitious() && node.getType() != Node.NodeType.SHUNT);
    }

    private void incorporateComponents(Node node, Element g) {
        SVGOMDocument obj = componentLibrary.getSvgDocument(node.getComponentType());
        transformComponent(node, g);
        if (obj != null && canInsertComponentSVG(node)) {
            insertComponentSVGIntoDocumentSVG(obj, g);
        }
    }

    private void insertComponentSVGIntoDocumentSVG(SVGOMDocument obj, Element g) {
        // The following code work correctly considering SVG part describing the component is the first child of "obj" the SVGDocument.
        // If SVG are written otherwise, it will not work correctly.

        for (int i = 0; i < obj.getChildNodes().item(0).getChildNodes().getLength(); i++) {
            org.w3c.dom.Node n = obj.getChildNodes().item(0).getChildNodes().item(i).cloneNode(true);
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

        double dx = points.get(2) - points.get(0);
        double dy = points.get(3) - points.get(1);

        double angle = Math.atan(dx / dy);

        double cosRo = Math.cos(angle);
        double sinRo = Math.sin(angle);
        double cdx = componentSize.getWidth() / 2;
        double cdy = componentSize.getHeight() / 2;

        double x = shift * sinRo + (points.get(0) + points.get(2)) / 2;
        double y = shift * cosRo + (points.get(1) + points.get(3)) / 2;

        double e1 = layoutParameters.getTranslateX() - cdx * cosRo + cdy * sinRo + x;
        double f1 = layoutParameters.getTranslateY() - cdx * sinRo - cdy * cosRo + y;

        int precision = 4;
        g.setAttribute(TRANSFORM,
                       "matrix(" + Precision.round(cosRo, precision) + "," + Precision.round(sinRo, precision)
                               + "," + Precision.round(-sinRo, precision) + "," + Precision.round(cosRo,
                                                                                                  precision) + ","
                               + Precision.round(e1, precision) + "," + Precision.round(f1, precision) + ")");
    }

    private void insertArrowsAndLabels(String wireId, List<Double> points, Element root, Node n, GraphMetadata metadata, SubstationDiagramInitialValueProvider initProvider) {
        InitialValue init = initProvider.getInitialValue(n);
        ComponentMetadata cd = metadata.getComponentMetadata(ComponentType.ARROW);

        int shX = (int) cd.getSize().getWidth()  + LABEL_OFFSET;
        int shY = (int) (cd.getSize().getHeight() - LABEL_OFFSET + FONT_SIZE / 2);

        Element g1 = root.getOwnerDocument().createElement("g");
        g1.setAttribute("id", wireId + "_ARROW1");
        SVGOMDocument arr = componentLibrary.getSvgDocument(ComponentType.ARROW);
        transformArrow(points, cd.getSize(), -cd.getSize().getHeight(), g1);
        insertComponentSVGIntoDocumentSVG(arr, g1);
        if (init.getLabel1().isPresent()) {
            drawLabel(init.getLabel1().get(), false, shX, shY, g1);
        }
        if (init.getArrowDirection1().isPresent()) {
            try {
                g1.setAttribute(CLASS, SubstationDiagramStyles.SUBSTATION_STYLE_CLASS + " "  + "ARROW1_" + SubstationDiagramStyles.escapeClassName(URLEncoder.encode(n.getId(), StandardCharsets.UTF_8.name())) + "_" +  init.getArrowDirection1().get());
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
        root.appendChild(g1);

        Element g2 = root.getOwnerDocument().createElement("g");
        g2.setAttribute("id", wireId + "_ARROW2");
        transformArrow(points, cd.getSize(), cd.getSize().getHeight(), g2);
        insertComponentSVGIntoDocumentSVG(arr, g2);
        if (init.getLabel2().isPresent()) {
            drawLabel(init.getLabel2().get(), false, shX, shY, g2);
        }
        if (init.getArrowDirection2().isPresent()) {
            try {
                g2.setAttribute("class", SubstationDiagramStyles.SUBSTATION_STYLE_CLASS + " "  + "ARROW2_" + SubstationDiagramStyles.escapeClassName(URLEncoder.encode(n.getId(), StandardCharsets.UTF_8.name())) + "_" +  init.getArrowDirection2().get());
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (init.getLabel3().isPresent()) {
            drawLabel(init.getLabel3().get(), false, -(init.getLabel3().get().length() * FONT_SIZE / 2 + LABEL_OFFSET), shY, g1);
        }
        if (init.getLabel4().isPresent()) {
            drawLabel(init.getLabel4().get(), false, -(init.getLabel4().get().length() * FONT_SIZE / 2 + LABEL_OFFSET), shY, g2);
        }

        root.appendChild(g2);
    }

    /*
     * Drawing the graph edges
     */
    private void drawEdges(Element root, Graph graph, GraphMetadata metadata, AnchorPointProvider anchorPointProvider, SubstationDiagramInitialValueProvider initProvider) {
        String vId = graph.getVoltageLevel().getId();
        for (Edge edge : graph.getEdges()) {
            // for unicity purpose (in substation diagram), we prefix the id of the WireMetadata with the voltageLevel id and "_"
            String wireId = vId + "_Wire" + graph.getEdges().indexOf(edge);
            Element g = root.getOwnerDocument().createElement("polyline");
            g.setAttribute("id", wireId);

            WireConnection anchorPoints = WireConnection.searchBetterAnchorPoints(anchorPointProvider, edge.getNode1(),
                                                                                  edge.getNode2());

            // Determine points of the polyline
            List<Double> pol = calculatePolylinePoints(edge, anchorPoints.getAnchorPoint1(),
                                                       anchorPoints.getAnchorPoint2());

            StringBuilder polPoints = new StringBuilder();
            for (int i = 0; i < pol.size(); i++) {
                if (i != 0) {
                    if (i % 2 == 0) {
                        polPoints.append(" ");
                    } else {
                        polPoints.append(",");
                    }
                }
                if (i % 2 == 0) {
                    double x = pol.get(i) + layoutParameters.getTranslateX();
                    polPoints.append(x);
                } else {
                    double y = pol.get(i) + layoutParameters.getTranslateY();
                    polPoints.append(y);
                }
            }

            g.setAttribute("points", polPoints.toString());
            g.setAttribute(CLASS, SubstationDiagramStyles.WIRE_STYLE_CLASS);
            root.appendChild(g);

            try {
                metadata.addWireMetadata(new GraphMetadata.WireMetadata(wireId,
                        URLEncoder.encode(edge.getNode1().getId(), StandardCharsets.UTF_8.name()),
                        URLEncoder.encode(edge.getNode2().getId(), StandardCharsets.UTF_8.name())));

            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }

            if (metadata.getComponentMetadata(ComponentType.ARROW) == null) {
                metadata.addComponentMetadata(new ComponentMetadata(ComponentType.ARROW,
                                                                    null,
                                                                    componentLibrary.getAnchorPoints(ComponentType.ARROW),
                                                                    componentLibrary.getSize(ComponentType.ARROW)));
            }

            if (edge.getNode1() instanceof FeederNode) {
                insertArrowsAndLabels(wireId, pol, root, edge.getNode1(), metadata, initProvider);
            } else if  (edge.getNode2() instanceof FeederNode) {
                insertArrowsAndLabels(wireId, pol, root, edge.getNode2(), metadata, initProvider);
            }
        }
    }

    /*
     * Drawing the substation graph edges
     */
    private void drawEdges(Element root, SubstationGraph graph, GraphMetadata metadata, AnchorPointProvider anchorPointProvider,  SubstationDiagramInitialValueProvider initProvider) {
        for (Edge edge : graph.getEdges()) {
            String vId1 = edge.getNode1().getGraph().getVoltageLevel().getId();
            String vId2 = edge.getNode2().getGraph().getVoltageLevel().getId();

            String wireId = vId1 + "_" + vId2 + "_" + "Wire" + graph.getEdges().indexOf(edge);
            Element g = root.getOwnerDocument().createElement("polyline");
            g.setAttribute("id", wireId);

            WireConnection anchorPoints = WireConnection.searchBetterAnchorPoints(anchorPointProvider, edge.getNode1(), edge.getNode2());

            // Determine points of the polyline
            List<Double> pol = calculatePolylinePoints(edge, anchorPoints.getAnchorPoint1(),
                    anchorPoints.getAnchorPoint2());

            StringBuilder polPoints = new StringBuilder();
            for (int i = 0; i < pol.size(); i++) {
                if (i != 0) {
                    if (i % 2 == 0) {
                        polPoints.append(" ");
                    } else {
                        polPoints.append(",");
                    }
                }
                if (i % 2 == 0) {
                    double x = pol.get(i) + layoutParameters.getTranslateX();
                    polPoints.append(x);
                } else {
                    double y = pol.get(i) + layoutParameters.getTranslateY();
                    polPoints.append(y);
                }
            }

            g.setAttribute("points", polPoints.toString());
            g.setAttribute(CLASS, SubstationDiagramStyles.WIRE_STYLE_CLASS);
            root.appendChild(g);

            try {
                metadata.addWireMetadata(new GraphMetadata.WireMetadata(wireId,
                    URLEncoder.encode(edge.getNode1().getId(), StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(edge.getNode2().getId(), StandardCharsets.UTF_8.name())));
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }

            if (metadata.getComponentMetadata(ComponentType.ARROW) == null) {
                metadata.addComponentMetadata(new ComponentMetadata(ComponentType.ARROW,
                                                                    null,
                                                                    componentLibrary.getAnchorPoints(ComponentType.ARROW),
                                                                    componentLibrary.getSize(ComponentType.ARROW)));
            }

            if (edge.getNode1() instanceof FeederNode) {
                insertArrowsAndLabels(wireId, pol, root, edge.getNode1(), metadata, initProvider);
            } else if  (edge.getNode2() instanceof FeederNode) {
                insertArrowsAndLabels(wireId, pol, root, edge.getNode2(), metadata, initProvider);
            }
        }
    }

    private List<Double> calculatePolylinePoints(Edge edge, AnchorPoint anchorPoint1, AnchorPoint anchorPoint2) {
        double x1 = edge.getNode1().getX() + anchorPoint1.getX();
        double y1 = edge.getNode1().getY() + anchorPoint1.getY();
        double x2 = edge.getNode2().getX() + anchorPoint2.getX();
        double y2 = edge.getNode2().getY() + anchorPoint2.getY();

        if (x1 == x2 || y1 == y2) {
            return Arrays.asList(x1, y1, x2, y2);
        }
        List<Double> pol = new ArrayList<>();
        switch (anchorPoint1.getOrientation()) {
            case VERTICAL:
                if (anchorPoint2.getOrientation() == AnchorOrientation.VERTICAL) {
                    double mid = (y1 + y2) / 2;
                    pol.addAll(Arrays.asList(x1, y1, x1, mid, x2, mid, x2, y2));
                } else {
                    pol.addAll(Arrays.asList(x1, y1, x1, y2, x2, y2));
                }
                break;
            case HORIZONTAL:
                if (anchorPoint2.getOrientation() == AnchorOrientation.HORIZONTAL) {
                    double mid = (x1 + x2) / 2;
                    pol.addAll(Arrays.asList(x1, y1, mid, y1, mid, y2, x2, y2));
                } else {
                    pol.addAll(Arrays.asList(x2, y2, x2, y1, x1, y1));
                }
                break;
            case NONE:
                // Case none-none is not handled, it never happens (even if it happen it will execute another case)
                if (anchorPoint2.getOrientation() == AnchorOrientation.HORIZONTAL) {
                    pol.addAll(Arrays.asList(x1, y1, x1, y2, x2, y2));
                } else {
                    pol.addAll(Arrays.asList(x2, y2, x2, y1, x1, y1));
                }
                break;
            default:
                break;
        }
        return pol;
    }
}
