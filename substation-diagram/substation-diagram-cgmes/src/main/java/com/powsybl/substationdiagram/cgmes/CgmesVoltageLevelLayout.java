/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.iidm.extensions.dl.CouplingDeviseDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.DiagramPoint;
import com.powsybl.cgmes.iidm.extensions.dl.InjectionDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.LineDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.NodeDiagramData;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.VoltageLevelLayout;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.BusNode;
import com.powsybl.substationdiagram.model.FeederNode;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.Node.NodeType;
import com.powsybl.substationdiagram.model.SwitchNode;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class CgmesVoltageLevelLayout implements VoltageLevelLayout {

    private static final Logger LOG = LoggerFactory.getLogger(CgmesVoltageLevelLayout.class);
    private static final double X_MARGIN = 20;
    private static final double Y_MARGIN = 10;
    private static final double LINE_OFFSET = 20;

    private final Graph graph;
    private double minX = 0;
    private double minY = 0;
    private boolean rotatedBus = false;

    public CgmesVoltageLevelLayout(Graph graph) {
        Objects.requireNonNull(graph);
        // remove fictitious nodes (no CGMES DL data available for them)
        graph.removeUnnecessaryFictitiousNodes();
        graph.removeFictitiousSwitchNodes();
        // set label using name (CGMES import uses RDF ids)
        graph.getNodes().forEach(node -> {
            if (node.getIdentifiable() != null) {
                node.setLabel(node.getIdentifiable().getName());
            }
        });
        this.graph = graph;
    }

    private void setMin(double x, double y) {
        if (minX == 0 || x < minX) {
            minX = x;
        }
        if (minY == 0 || y < minY) {
            minY = y;
        }
    }

    @Override
    public void run(LayoutParameters layoutParam) {
        // skip line nodes: I need the coordinates of the adjacent node to know which side of the line belongs to this voltage level
        graph.getNodes().stream().filter(node -> !isLineNode(node)).forEach(node -> {
            setNodeCoordinates(node);
        });
        // set line nodes coordinates: I use the coordinates of the adjacent node to know which side of the line belongs to this voltage level
        graph.getNodes().stream().filter(node -> isLineNode(node)).forEach(node -> {
            setLineNodeCoordinates(node);
        });
        graph.getNodes().forEach(node -> {
            shiftNodeCoordinates(node, layoutParam.getScaleFactor());
        });
        if (layoutParam.getScaleFactor() != 1) {
            graph.getNodes().forEach(node -> {
                scaleNodeCoordinates(node, layoutParam.getScaleFactor());
            });
        }
    }

    private boolean isLineNode(Node node) {
        return Arrays.asList(ComponentType.LINE, ComponentType.DANGLING_LINE, ComponentType.VSC_CONVERTER_STATION).contains(node.getComponentType());
    }

    private void setNodeCoordinates(Node node) {
        LOG.info("Setting coordinates of node {}, type {}, component type {}", node.getId(), node.getType(), node.getComponentType());
        switch (node.getType()) {
            case BUS:
                BusNode busNode = (BusNode) node;
                if (TopologyKind.NODE_BREAKER.equals(graph.getTopologyKind())) {
                    BusbarSection busbar = (BusbarSection) busNode.getIdentifiable();
                    NodeDiagramData<BusbarSection> busbarDiagramData = busbar.getExtension(NodeDiagramData.class);
                    setBusNodeCoordinates(busNode, busbarDiagramData, busbar.getName());
                } else {
                    Bus bus = (Bus) busNode.getIdentifiable();
                    NodeDiagramData<Bus> busDiagramData = bus.getExtension(NodeDiagramData.class);
                    setBusNodeCoordinates(busNode, busDiagramData, bus.getName());
                }
                break;
            case SWITCH:
                SwitchNode switchNode = (SwitchNode) node;
                Switch sw = (Switch) switchNode.getIdentifiable();
                CouplingDeviseDiagramData<Switch> switchDiagramData = sw.getExtension(CouplingDeviseDiagramData.class);
                if (switchDiagramData != null) {
                    switchNode.setX(switchDiagramData.getPoint().getX());
                    switchNode.setY(switchDiagramData.getPoint().getY());
                    if (switchDiagramData.getRotation() == 90 || switchDiagramData.getRotation() == 270) {
                        switchNode.setRotated(true);
                    }
                    setMin(switchDiagramData.getPoint().getX(), switchDiagramData.getPoint().getY());
                } else {
                    LOG.warn("No CGMES-DL data for {} node {}, switch {}", node.getType(), node.getId(), sw.getName());
                }
                break;
            case FEEDER:
                setFeederNodeCoordinates(node);
                break;
            default:
                break;
        }
    }

    private void setBusNodeCoordinates(BusNode node, NodeDiagramData<?> diagramData, String name) {
        if (diagramData != null) {
            node.setX(diagramData.getPoint1().getX());
            node.setY(diagramData.getPoint1().getY());
            node.setPxWidth(computeBusWidth(diagramData));
            rotatedBus = diagramData.getPoint1().getX() == diagramData.getPoint2().getX();
            node.setRotated(rotatedBus);
            setMin(diagramData.getPoint1().getX(), diagramData.getPoint1().getY());
        } else {
            LOG.warn("No CGMES-DL data for {} node {}, bus {}", node.getType(), node.getId(), name);
        }
    }

    private double computeBusWidth(NodeDiagramData<?> diagramData) {
        if (diagramData.getPoint1().getX() == diagramData.getPoint2().getX()) {
            return Math.abs(diagramData.getPoint1().getY() - diagramData.getPoint2().getY());
        } else {
            return Math.abs(diagramData.getPoint1().getX() - diagramData.getPoint2().getX());
        }
    }

    private void setFeederNodeCoordinates(Node node) {
        switch (node.getComponentType()) {
            case LOAD:
                FeederNode loadNode = (FeederNode) node;
                Load load = (Load) loadNode.getIdentifiable();
                InjectionDiagramData<Load> loadDiagramData = load.getExtension(InjectionDiagramData.class);
                setInjectionCoordinates(loadNode, loadDiagramData, load.getName(), true);
                break;
            case GENERATOR:
                FeederNode generatorNode = (FeederNode) node;
                Generator generator = (Generator) generatorNode.getIdentifiable();
                InjectionDiagramData<Generator> generatorDiagramData = generator.getExtension(InjectionDiagramData.class);
                setInjectionCoordinates(generatorNode, generatorDiagramData, generator.getName(), false);
                break;
            case CAPACITOR:
            case INDUCTOR:
                FeederNode shuntNode = (FeederNode) node;
                ShuntCompensator shunt = (ShuntCompensator) shuntNode.getIdentifiable();
                InjectionDiagramData<ShuntCompensator> shuntDiagramData = shunt.getExtension(InjectionDiagramData.class);
                setInjectionCoordinates(shuntNode, shuntDiagramData, shunt.getName(), true);
                break;
            case STATIC_VAR_COMPENSATOR:
                FeederNode svcNode = (FeederNode) node;
                StaticVarCompensator svc = (StaticVarCompensator) svcNode.getIdentifiable();
                InjectionDiagramData<StaticVarCompensator> svcDiagramData = svc.getExtension(InjectionDiagramData.class);
                setInjectionCoordinates(svcNode, svcDiagramData, svc.getName(), true);
                break;
            case TWO_WINDINGS_TRANSFORMER:
                FeederNode transformerNode = (FeederNode) node;
                TwoWindingsTransformer transformer = (TwoWindingsTransformer) transformerNode.getIdentifiable();
                CouplingDeviseDiagramData<TwoWindingsTransformer> transformerDiagramData = transformer.getExtension(CouplingDeviseDiagramData.class);
                if (transformerDiagramData != null) {
                    transformerNode.setX(transformerDiagramData.getPoint().getX());
                    transformerNode.setY(transformerDiagramData.getPoint().getY());
                    setMin(transformerDiagramData.getPoint().getX(), transformerDiagramData.getPoint().getY());
                } else {
                    LOG.warn("No CGMES-DL data for {} {} node {}, transformer {}", node.getType(), node.getComponentType(), node.getId(), transformer.getName());
                }
                break;
            default:
                break;
        }
    }

    private void setInjectionCoordinates(FeederNode node, InjectionDiagramData<?> diagramData, String name, boolean rotate) {
        if (diagramData != null) {
            node.setX(diagramData.getPoint().getX());
            node.setY(diagramData.getPoint().getY());
            node.setRotated(rotate && (diagramData.getRotation() == 90 || diagramData.getRotation() == 270));
            setMin(diagramData.getPoint().getX(), diagramData.getPoint().getY());
        } else {
            LOG.warn("No CGMES-DL data for {} {} node {}, injection {}", node.getType(), node.getComponentType(), node.getId(), name);
        }
    }

    private void setLineNodeCoordinates(Node node) {
        LOG.info("Setting coordinates of node {}, type {}, component type {}", node.getId(), node.getType(), node.getComponentType());
        switch (node.getComponentType()) {
            case LINE:
                FeederNode lineNode = (FeederNode) node;
                Line line = (Line) lineNode.getIdentifiable();
                LineDiagramData<Line> lineDiagramData = line.getExtension(LineDiagramData.class);
                setLineNodeCoordinates(lineNode, lineDiagramData, line.getName());
                break;
            case DANGLING_LINE:
                FeederNode danglingLineNode = (FeederNode) node;
                DanglingLine danglingLine = (DanglingLine) danglingLineNode.getIdentifiable();
                LineDiagramData<DanglingLine> danglingLineDiagramData = danglingLine.getExtension(LineDiagramData.class);
                setLineNodeCoordinates(danglingLineNode, danglingLineDiagramData, danglingLine.getName());
                break;
            default:
                break;
        }
    }

    private void setLineNodeCoordinates(FeederNode node, LineDiagramData<?> diagramData, String name) {
        if (diagramData != null) {
            DiagramPoint linePoint = getLinePoint(diagramData, node);
            node.setX(linePoint.getX());
            node.setY(linePoint.getY());
            node.setRotated(rotatedBus);
            setMin(linePoint.getX(), linePoint.getY());
        } else {
            LOG.warn("No CGMES-DL data for {} {} node {}, line {}", node.getType(), node.getComponentType(), node.getId(), name);
        }
    }

    private <T> DiagramPoint getLinePoint(LineDiagramData<?> lineDiagramData, Node lineNode) {
        DiagramPoint adjacentNodePoint = getLineAdjacentNodePoint(lineNode);
        if (adjacentNodePoint == null) {
            return getLinePoint(lineDiagramData, true);
        }
        double firstPointDistance = Math.hypot(lineDiagramData.getFirstPoint().getX() - adjacentNodePoint.getX(),
                                               lineDiagramData.getFirstPoint().getY() - adjacentNodePoint.getY());
        double lastPointDistance = Math.hypot(lineDiagramData.getLastPoint().getX() - adjacentNodePoint.getX(),
                                              lineDiagramData.getLastPoint().getY() - adjacentNodePoint.getY());
        return getLinePoint(lineDiagramData, firstPointDistance > lastPointDistance);
    }

    private DiagramPoint getLineAdjacentNodePoint(Node branchNode) {
        List<Node> adjacentNodes = branchNode.getAdjacentNodes();
        if (adjacentNodes == null || adjacentNodes.isEmpty()) {
            return null;
        }
        Node adjacentNode = adjacentNodes.get(0); // as we are working on a single voltage level a line node should be connected to only 1 node
        // a line should not be connected to another line, so I should already have the coordinates of the adjacent node
        return new DiagramPoint(adjacentNode.getX(), adjacentNode.getY(), 0);
    }

    private <T> DiagramPoint getLinePoint(LineDiagramData<?> lineDiagramData, boolean isLastPointCloser) {
        if (TopologyKind.NODE_BREAKER.equals(graph.getTopologyKind())) {
            return isLastPointCloser ? lineDiagramData.getLastPoint() : lineDiagramData.getFirstPoint();
        }
        return isLastPointCloser ? lineDiagramData.getLastPoint(LINE_OFFSET) : lineDiagramData.getFirstPoint(LINE_OFFSET);
    }

    private void shiftNodeCoordinates(Node node, double scaleFactor) {
        node.setX(node.getX() - minX + (X_MARGIN / scaleFactor));
        node.setY(node.getY() - minY + (Y_MARGIN / scaleFactor));
    }

    private void scaleNodeCoordinates(Node node, double scaleFactor) {
        node.setX(node.getX() * scaleFactor);
        node.setY(node.getY() * scaleFactor);
        if (node.getType() == NodeType.BUS) {
            BusNode nodeBus = (BusNode) node;
            nodeBus.setPxWidth(nodeBus.getPxWidth() * scaleFactor);
        }
    }

}
