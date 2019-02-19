/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import com.powsybl.commons.exceptions.UncheckedTransformerException;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.library.*;
import com.powsybl.substationdiagram.model.*;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SVGWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SVGWriter.class);

    private static final String STYLE = "style";
    private static final String TRANSFORM = "transform";
    private static final String TRANSLATE = "translate";

    private final ComponentLibrary componentLibrary;

    private final LayoutParameters layoutParameters;

    public SVGWriter(ComponentLibrary componentLibrary, LayoutParameters layoutParameters) {
        this.componentLibrary = Objects.requireNonNull(componentLibrary);
        this.layoutParameters = Objects.requireNonNull(layoutParameters);
    }

    public GraphMetadata write(Graph graph, Path svgFile) {
        try (Writer writer = Files.newBufferedWriter(svgFile)) {
            return write(graph, writer);
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
    public GraphMetadata write(Graph graph, Writer writer) {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

        Document document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);

        GraphMetadata metadata = writegraph(graph, document);

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
    private GraphMetadata writegraph(Graph graph, Document document) {
        GraphMetadata metadata = new GraphMetadata();

        Element root = document.createElement("g");

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

        drawNodes(root, graph, metadata, anchorPointProvider);
        drawEdges(root, graph, metadata, anchorPointProvider);

        document.adoptNode(root);
        document.getDocumentElement().appendChild(root);

        return metadata;
    }

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
                              Double.toString(layoutParameters.getInitialXBus() + i * layoutParameters.getCellWidth()));
            line.setAttribute("x2",
                              Double.toString(layoutParameters.getInitialXBus() + i * layoutParameters.getCellWidth()));
            line.setAttribute("y1",
                              Double.toString(layoutParameters.getInitialYBus() - layoutParameters.getStackHeight()
                                                      - layoutParameters.getExternCellHeight()));
            line.setAttribute("y2", Double.toString(
                    layoutParameters.getInitialYBus() + layoutParameters.getStackHeight() + layoutParameters.getExternCellHeight()
                            + layoutParameters.getVerticalSpaceBus() * maxV));
            line.setAttribute(STYLE, "stroke:rgb(0,55,0);stroke-width:1;stroke-dasharray:1,10");
            line.setAttribute(TRANSFORM,
                              TRANSLATE + "(" + layoutParameters.getTranslateX() + "," + layoutParameters.getTranslateY() + ")");
            gridRoot.appendChild(line);
        }
        return gridRoot;
    }

    private void drawNodes(Element root, Graph graph, GraphMetadata metadata, AnchorPointProvider anchorPointProvider) {
        graph.getNodes().forEach(node -> {
            Element g = root.getOwnerDocument().createElement("g");
            g.setAttribute("id", node.getId());

            if (node.getType() == Node.NodeType.BUS) {
                drawBus((BusNode) node, g);
            } else {
                incorporateComponents(node, g);
            }
            if (node instanceof FeederNode || node instanceof BusNode) {
                drawLabel(node.getLabel(), node.isRotated(), g);
            }
            root.appendChild(g);

            metadata.addNodeMetadata(
                    new GraphMetadata.NodeMetadata(node.getId(), node.getComponentType(), node.isRotated()));
            if (node.getType() == Node.NodeType.BUS) {
                metadata.addComponentMetadata(new ComponentMetadata(ComponentType.BUSBAR_SECTION,
                                                                    node.getId(),
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
        });
    }

    private void drawBus(BusNode node, Element g) {
        Element line = g.getOwnerDocument().createElement("line");
        line.setAttribute("x1", "0");
        line.setAttribute("y1", "0");
        line.setAttribute("x2", String.valueOf(node.getPxWidth()));
        line.setAttribute("y2", "0");
        line.setAttribute(STYLE, "stroke:rgb(0,0,0);stroke-width:3");

        g.appendChild(line);

        g.setAttribute(TRANSFORM, TRANSLATE + "(" + (layoutParameters.getTranslateX() + node.getX()) + ","
                + (layoutParameters.getTranslateY() + node.getY()) + ")");
    }

    private void drawLabel(String str, boolean rotated, Element g) {
        Element label = g.getOwnerDocument().createElement("text");
        label.setAttribute("x", "-5");
        label.setAttribute("y", "-5");
        label.setAttribute("font-family", "Verdana");
        label.setAttribute("font-size", "8");
        Text text = g.getOwnerDocument().createTextNode(str);
        label.setAttribute(TRANSFORM, "rotate(" + (rotated ? -90 : 0) + "," + 0 + "," + 0 + ")");
        label.appendChild(text);
        g.appendChild(label);
    }

    private void incorporateComponents(Node node, Element g) {
        SVGOMDocument obj = componentLibrary.getSvgDocument(node.getComponentType());
        if (obj != null) {
            transformComponent(node, g);
            if (layoutParameters.isShowInternalNodes()
                    || (node.getType() != Node.NodeType.FICTITIOUS
                    && node.getType() != Node.NodeType.SHUNT
                    && node.getType() != Node.NodeType.FICTITIOUS_SWITCH)) {
                insertComponentSVGIntoDocumentSVG(obj, g);
            }
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

    private void drawEdges(Element root, Graph graph, GraphMetadata metadata, AnchorPointProvider anchorPointProvider) {
        for (Edge edge : graph.getEdges()) {
            Element g = root.getOwnerDocument().createElement("polyline");
            g.setAttribute("id", "Wire" + graph.getEdges().indexOf(edge));

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
            g.setAttribute(STYLE, "stroke:rgb(200,0,0);stroke-width:1");
            g.setAttribute("fill", "none");
            root.appendChild(g);

            metadata.addWireMetadata(new GraphMetadata.WireMetadata("Wire" + graph.getEdges().indexOf(edge),
                                                                    edge.getNode1().getId(),
                                                                    edge.getNode2().getId()));
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
