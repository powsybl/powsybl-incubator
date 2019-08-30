/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.util;

import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.BUS_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.GRID_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.LABEL_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.SUBSTATION_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.WIRE_STYLE_CLASS;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.escapeClassName;
import static com.powsybl.substationdiagram.svg.SubstationDiagramStyles.escapeId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.google.common.util.concurrent.AtomicDouble;
import com.powsybl.basevoltage.BaseVoltageColor;
import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TopologyVisitor;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.Edge;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.Node.NodeType;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyles;

import javafx.scene.paint.Color;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class TopologicalStyleProvider extends DefaultSubstationDiagramStyleProvider {

    private BaseVoltageColor baseVoltageColor;
    private HashMap<String, HashMap<String, Color>> voltageLevelColorMap = new HashMap();
    private static final String DEFAULT_COLOR = "red";
    private static final String DISCONNECTED_COLOR = "grey";
    private String disconnectedColor;

    public TopologicalStyleProvider() {
        try {
            baseVoltageColor = new BaseVoltageColor();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        disconnectedColor = baseVoltageColor.getColor(0, "RTE") != null ? baseVoltageColor.getColor(0, "RTE")
                : DISCONNECTED_COLOR;
    }

    private String getBusColor(Node node) {
        String id = node.getId();
        VoltageLevel vl = node.getGraph().getVoltageLevel();

        HashMap<String, Color> colorMap = voltageLevelColorMap.computeIfAbsent(vl.getId(), k -> getColorMap(vl));

        Color c = colorMap.get(id);
        return c != null ? String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255)) : null;
    }

    private HashMap<String, Color> getColorMap(VoltageLevel vl) {
        String basecolor = baseVoltageColor.getColor(vl.getNominalV(), "RTE");
        Color c0 = Color.valueOf(basecolor != null ? basecolor : DEFAULT_COLOR);

        AtomicDouble idx = new AtomicDouble(0);
        long buses = vl.getBusView().getBusStream().count();

        HashMap<String, Color> colorMap = new HashMap();
        HashMap<String, Color> busColorMap = new HashMap();

        vl.getBusView().getBuses().forEach(b -> {

            idx.getAndAdd(1);
            double ratio = idx.get() / buses;

            Color c = c0.deriveColor(ratio, ratio, ratio, 1);
            busColorMap.put(b.getId(), c);

            vl.getBusView().getBus(b.getId()).visitConnectedEquipments(new TopologyVisitor() {
                @Override
                public void visitBusbarSection(BusbarSection e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitDanglingLine(DanglingLine e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitGenerator(Generator e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitLine(Line e, Side s) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitLoad(Load e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitShuntCompensator(ShuntCompensator e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitStaticVarCompensator(StaticVarCompensator e) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitThreeWindingsTransformer(ThreeWindingsTransformer e,
                        com.powsybl.iidm.network.ThreeWindingsTransformer.Side s) {
                    colorMap.put(e.getId(), c);
                }

                @Override
                public void visitTwoWindingsTransformer(TwoWindingsTransformer e, Side s) {
                    colorMap.put(e.getId(), c);
                }
            });
        });

        return colorMap;
    }

    @Override
    public Optional<String> getGlobalStyle(Graph graph) {
        String idVL = escapeClassName(graph.getVoltageLevel().getId());
        StringBuilder style = new StringBuilder();
        style.append(".").append(SUBSTATION_STYLE_CLASS)
                .append(" {fill:rgb(255,255,255);stroke-width:1;fill-opacity:0;}");
        style.append(".").append(WIRE_STYLE_CLASS).append("_").append(idVL)
                .append(" {stroke:rgb(200,0,0);stroke-width:1;}");
        style.append(".").append(GRID_STYLE_CLASS)
                .append(" {stroke:rgb(0,55,0);stroke-width:1;stroke-dasharray:1,10;}");
        style.append(".").append(BUS_STYLE_CLASS).append("_").append(idVL).append(" {stroke-width:3;}");
        style.append(".").append(LABEL_STYLE_CLASS).append(" {fill:black;color:black;stroke:none;fill-opacity:1;}");
        return Optional.of(style.toString());
    }

    @Override
    public Optional<String> getNodeStyle(Node node) {

        Optional<String> defaultStyle = super.getNodeStyle(node);
        if (node.getType() == NodeType.SWITCH || node.getComponentType() == ComponentType.TWO_WINDINGS_TRANSFORMER) {
            return defaultStyle;
        }

        try {
            String color = getBusColor(node);
            if (color == null) {
                color = disconnectedColor;
            }
            return Optional.of(defaultStyle.orElse("") + " #"
                    + escapeId(URLEncoder.encode(node.getId(), StandardCharsets.UTF_8.name())) + " {stroke:"
                    + color + ";stroke-width:1;fill-opacity:0;}");

        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<String> getWireStyle(Edge edge) {

        try {
            String wireId = SubstationDiagramStyles
                    .escapeId(URLEncoder.encode(
                            edge.getNode1().getGraph().getVoltageLevel().getId() + "_Wire"
                                    + edge.getNode1().getGraph().getEdges().indexOf(edge),
                            StandardCharsets.UTF_8.name()));
            Node bus = findConnectedBus(edge);
            String color = bus != null ? getBusColor(bus) : disconnectedColor;

            return Optional.of(" #" + wireId + " {stroke:" + color + ";stroke-width:1;fill-opacity:0;}");
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Node findConnectedBus(Edge edge) {
        Node n1 = edge.getNode1();
        if (n1.getType() == NodeType.BUS) {
            return n1;
        }
        Node n2 = edge.getNode2();
        if (n1.getType() == NodeType.BUS) {
            return n2;
        }
        Node n11 = findConnectedBus(n1, new ArrayList<Node>());
        if (n11 != null) {
            return n11;
        } else {
            return findConnectedBus(n2, new ArrayList<Node>());
        }
    }

    private Node findConnectedBus(Node node, List<Node> visitedNodes) {
        List<Node> nodesToVisit = new ArrayList<>(node.getAdjacentNodes());
        if (!visitedNodes.contains(node)) {
            visitedNodes.add(node);
            if (node.getType().equals(NodeType.SWITCH) && node.isOpen()) {
                return null;
            }
            for (Node n : nodesToVisit) {
                if (n.getType().equals(NodeType.BUS)) {
                    return n;
                } else {
                    Node n1 = findConnectedBus(n, visitedNodes);
                    if (n1 != null) {
                        return n1;
                    }
                }
            }
        }
        return null;
    }

}
