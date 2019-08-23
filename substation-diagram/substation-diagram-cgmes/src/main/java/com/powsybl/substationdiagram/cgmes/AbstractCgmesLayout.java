/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.iidm.extensions.dl.CouplingDeviceDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.DiagramPoint;
import com.powsybl.cgmes.iidm.extensions.dl.InjectionDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.LineDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.NodeDiagramData;
import com.powsybl.cgmes.iidm.extensions.dl.ThreeWindingsTransformerDiagramData;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.model.BusNode;
import com.powsybl.substationdiagram.model.FeederNode;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.model.Node;
import com.powsybl.substationdiagram.model.SwitchNode;
import com.powsybl.substationdiagram.model.Node.NodeType;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public abstract class AbstractCgmesLayout {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCgmesLayout.class);
    protected static final double X_MARGIN = 20;
    protected static final double Y_MARGIN = 10;
    protected static final double LINE_OFFSET = 20;

    protected double minX = 0;
    protected double minY = 0;
    protected boolean rotatedBus = false;
    protected boolean isNodeBreaker = true;
    protected boolean fixTransformersLabel = false;

    protected void setMin(double x, double y) {
        if (minX == 0 || x < minX) {
            minX = x;
        }
        if (minY == 0 || y < minY) {
            minY = y;
        }
    }

    protected Graph removeFictitiousNodes(Graph graph) {
        graph.removeUnnecessaryFictitiousNodes();
        graph.removeFictitiousSwitchNodes();
        return graph;
    }

    protected void setNodeCoordinates(Graph graph) {
        isNodeBreaker = TopologyKind.NODE_BREAKER.equals(graph.getVoltageLevel().getTopologyKind());
        // skip line nodes: I need the coordinates of the adjacent node to know which side of the line belongs to this voltage level
        graph.getNodes().stream().filter(node -> !isLineNode(node)).forEach(node -> setNodeCoordinates(graph, node));
        // set line nodes coordinates: I use the coordinates of the adjacent node to know which side of the line belongs to this voltage level
        graph.getNodes().stream().filter(this::isLineNode).forEach(node -> setLineNodeCoordinates(graph, node));
    }

    protected boolean isLineNode(Node node) {
        return Arrays.asList(ComponentType.LINE, ComponentType.DANGLING_LINE, ComponentType.VSC_CONVERTER_STATION).contains(node.getComponentType());
    }

    protected void setNodeCoordinates(Graph graph, Node node) {
        LOG.info("Setting coordinates of node {}, type {}, component type {}", node.getId(), node.getType(), node.getComponentType());
        switch (node.getType()) {
            case BUS:
                BusNode busNode = (BusNode) node;
                if (TopologyKind.NODE_BREAKER.equals(graph.getVoltageLevel().getTopologyKind())) {
                    BusbarSection busbar = graph.getVoltageLevel().getConnectable(busNode.getId(), BusbarSection.class);
                    NodeDiagramData<BusbarSection> busbarDiagramData = busbar != null ? busbar.getExtension(NodeDiagramData.class) : null;
                    setBusNodeCoordinates(busNode, busbarDiagramData);
                } else {
                    Bus bus = graph.getVoltageLevel().getBusBreakerView().getBus(busNode.getId());
                    NodeDiagramData<Bus> busDiagramData =  bus != null ? bus.getExtension(NodeDiagramData.class) : null;
                    setBusNodeCoordinates(busNode, busDiagramData);
                }
                break;
            case SWITCH:
                SwitchNode switchNode = (SwitchNode) node;
                Switch sw = TopologyKind.NODE_BREAKER.equals(graph.getVoltageLevel().getTopologyKind()) ?
                            graph.getVoltageLevel().getNodeBreakerView().getSwitch(switchNode.getId()) :
                            graph.getVoltageLevel().getBusBreakerView().getSwitch(switchNode.getId());
                CouplingDeviceDiagramData<Switch> switchDiagramData =  sw != null ? sw.getExtension(CouplingDeviceDiagramData.class) : null;
                setCouplingDeviceNodeCoordinates(switchNode, switchDiagramData, true);
                break;
            case FEEDER:
                setFeederNodeCoordinates(graph, node);
                break;
            default:
                break;
        }
    }

    protected void setBusNodeCoordinates(BusNode node, NodeDiagramData<?> diagramData) {
        if (diagramData != null) {
            node.setX(diagramData.getPoint1().getX());
            node.setY(diagramData.getPoint1().getY());
            node.setPxWidth(computeBusWidth(diagramData));
            rotatedBus = diagramData.getPoint1().getX() == diagramData.getPoint2().getX();
            node.setRotated(rotatedBus);
            setMin(diagramData.getPoint1().getX(), diagramData.getPoint1().getY());
        } else {
            LOG.warn("No CGMES-DL data for {} node {}, bus {}", node.getType(), node.getId(), node.getName());
        }
    }

    protected void setCouplingDeviceNodeCoordinates(Node node, CouplingDeviceDiagramData<?> diagramData, boolean rotate) {
        if (diagramData != null) {
            node.setX(diagramData.getPoint().getX());
            node.setY(diagramData.getPoint().getY());
            node.setRotated(rotate && diagramData.getRotation() == 90 || diagramData.getRotation() == 270);
            setMin(diagramData.getPoint().getX(), diagramData.getPoint().getY());
        } else {
            LOG.warn("No CGMES-DL data for {} node {}, name {}", node.getType(), node.getId(), node.getName());
        }
    }

    protected double computeBusWidth(NodeDiagramData<?> diagramData) {
        if (diagramData.getPoint1().getX() == diagramData.getPoint2().getX()) {
            return Math.abs(diagramData.getPoint1().getY() - diagramData.getPoint2().getY());
        } else {
            return Math.abs(diagramData.getPoint1().getX() - diagramData.getPoint2().getX());
        }
    }

    protected void setFeederNodeCoordinates(Graph graph, Node node) {
        switch (node.getComponentType()) {
            case LOAD:
                FeederNode loadNode = (FeederNode) node;
                Load load = graph.getVoltageLevel().getConnectable(loadNode.getId(), Load.class);
                InjectionDiagramData<Load> loadDiagramData = load != null ? load.getExtension(InjectionDiagramData.class) : null;
                setInjectionNodeCoordinates(loadNode, loadDiagramData, true);
                break;
            case GENERATOR:
                FeederNode generatorNode = (FeederNode) node;
                Generator generator = graph.getVoltageLevel().getConnectable(generatorNode.getId(), Generator.class);
                InjectionDiagramData<Generator> generatorDiagramData = generator != null ? generator.getExtension(InjectionDiagramData.class) : null;
                setInjectionNodeCoordinates(generatorNode, generatorDiagramData, false);
                break;
            case CAPACITOR:
            case INDUCTOR:
                FeederNode shuntNode = (FeederNode) node;
                ShuntCompensator shunt = graph.getVoltageLevel().getConnectable(shuntNode.getId(), ShuntCompensator.class);
                InjectionDiagramData<ShuntCompensator> shuntDiagramData = shunt != null ? shunt.getExtension(InjectionDiagramData.class) : null;
                setInjectionNodeCoordinates(shuntNode, shuntDiagramData, true);
                break;
            case STATIC_VAR_COMPENSATOR:
                FeederNode svcNode = (FeederNode) node;
                StaticVarCompensator svc = graph.getVoltageLevel().getConnectable(svcNode.getId(), StaticVarCompensator.class);
                InjectionDiagramData<StaticVarCompensator> svcDiagramData = svc != null ? svc.getExtension(InjectionDiagramData.class) : null;
                setInjectionNodeCoordinates(svcNode, svcDiagramData, true);
                break;
            case TWO_WINDINGS_TRANSFORMER:
            case PHASE_SHIFT_TRANSFORMER:
                FeederNode transformerNode = (FeederNode) node;
                TwoWindingsTransformer transformer = graph.getVoltageLevel().getConnectable(getBranchId(transformerNode.getId()), TwoWindingsTransformer.class);
                CouplingDeviceDiagramData<TwoWindingsTransformer> transformerDiagramData = null;
                if (transformer != null) {
                    transformerDiagramData = transformer.getExtension(CouplingDeviceDiagramData.class);
                    setTransformersLabel(transformerNode, graph.isUseName(), transformer.getName(), transformer.getId());
                }
                setCouplingDeviceNodeCoordinates(transformerNode, transformerDiagramData, false);
                break;
            case THREE_WINDINGS_TRANSFORMER:
                FeederNode transformer3wNode = (FeederNode) node;
                ThreeWindingsTransformer transformer3w = graph.getVoltageLevel().getConnectable(getBranchId(transformer3wNode.getId()), ThreeWindingsTransformer.class);
                ThreeWindingsTransformerDiagramData transformer3wDiagramData = null;
                if (transformer3w != null) {
                    transformer3wDiagramData = transformer3w.getExtension(ThreeWindingsTransformerDiagramData.class);
                    setTransformersLabel(transformer3wNode, graph.isUseName(), transformer3w.getName(), transformer3w.getId());
                }
                setThreeWindingsTransformerNodeCoordinates(transformer3wNode, transformer3wDiagramData);
                break;
            default:
                break;
        }
    }

    protected void setTransformersLabel(FeederNode node, boolean useNames, String name, String id) {
        if (fixTransformersLabel) {
            String label = useNames ? name : id;
            node.setLabel(label);
        }
    }

    protected String getBranchId(String branchNodeId) {
        return branchNodeId.substring(0, branchNodeId.lastIndexOf('_'));
    }

    protected void setInjectionNodeCoordinates(FeederNode node, InjectionDiagramData<?> diagramData, boolean rotate) {
        if (diagramData != null) {
            node.setX(diagramData.getPoint().getX());
            node.setY(diagramData.getPoint().getY());
            node.setRotated(rotate && (diagramData.getRotation() == 90 || diagramData.getRotation() == 270));
            setMin(diagramData.getPoint().getX(), diagramData.getPoint().getY());
        } else {
            LOG.warn("No CGMES-DL data for {} {} node {}, injection {}", node.getType(), node.getComponentType(), node.getId(), node.getName());
        }
    }

    protected void setThreeWindingsTransformerNodeCoordinates(FeederNode node, ThreeWindingsTransformerDiagramData diagramData) {
        if (diagramData != null) {
            node.setX(diagramData.getPoint().getX());
            node.setY(diagramData.getPoint().getY());
            setMin(diagramData.getPoint().getX(), diagramData.getPoint().getY());
        } else {
            LOG.warn("No CGMES-DL data for {} {} node {}, transformer {}", node.getType(), node.getComponentType(), node.getId(), node.getName());
        }
    }

    protected void setLineNodeCoordinates(Graph graph, Node node) {
        LOG.info("Setting coordinates of node {}, type {}, component type {}", node.getId(), node.getType(), node.getComponentType());
        switch (node.getComponentType()) {
            case LINE:
                FeederNode lineNode = (FeederNode) node;
                Line line = graph.getVoltageLevel().getConnectable(getBranchId(lineNode.getId()), Line.class);
                LineDiagramData<Line> lineDiagramData = line != null ? line.getExtension(LineDiagramData.class) : null;
                setLineNodeCoordinates(lineNode, lineDiagramData);
                break;
            case DANGLING_LINE:
                FeederNode danglingLineNode = (FeederNode) node;
                DanglingLine danglingLine = graph.getVoltageLevel().getConnectable(danglingLineNode.getId(), DanglingLine.class);
                LineDiagramData<DanglingLine> danglingLineDiagramData = danglingLine != null ? danglingLine.getExtension(LineDiagramData.class) : null;
                setLineNodeCoordinates(danglingLineNode, danglingLineDiagramData);
                break;
            default:
                break;
        }
    }

    protected void setLineNodeCoordinates(FeederNode node, LineDiagramData<?> diagramData) {
        if (diagramData != null) {
            DiagramPoint linePoint = getLinePoint(diagramData, node);
            node.setX(linePoint.getX());
            node.setY(linePoint.getY());
            node.setRotated(rotatedBus);
            setMin(linePoint.getX(), linePoint.getY());
        } else {
            LOG.warn("No CGMES-DL data for {} {} node {}, line {}", node.getType(), node.getComponentType(), node.getId(), node.getName());
        }
    }

    protected DiagramPoint getLinePoint(LineDiagramData<?> lineDiagramData, Node lineNode) {
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

    protected DiagramPoint getLineAdjacentNodePoint(Node branchNode) {
        List<Node> adjacentNodes = branchNode.getAdjacentNodes();
        if (adjacentNodes == null || adjacentNodes.isEmpty()) {
            return null;
        }
        Node adjacentNode = adjacentNodes.get(0); // as we are working on a single voltage level a line node should be connected to only 1 node
        // a line should not be connected to another line, so I should already have the coordinates of the adjacent node
        return new DiagramPoint(adjacentNode.getX(), adjacentNode.getY(), 0);
    }

    protected DiagramPoint getLinePoint(LineDiagramData<?> lineDiagramData, boolean isLastPointCloser) {
        if (isNodeBreaker) {
            return isLastPointCloser ? lineDiagramData.getLastPoint() : lineDiagramData.getFirstPoint();
        }
        return isLastPointCloser ? lineDiagramData.getLastPoint(LINE_OFFSET) : lineDiagramData.getFirstPoint(LINE_OFFSET);
    }

    protected void shiftNodeCoordinates(Node node, double scaleFactor) {
        node.setX(node.getX() - minX + (X_MARGIN / scaleFactor));
        node.setY(node.getY() - minY + (Y_MARGIN / scaleFactor));
    }

    protected void scaleNodeCoordinates(Node node, double scaleFactor) {
        node.setX(node.getX() * scaleFactor);
        node.setY(node.getY() * scaleFactor);
        if (node.getType() == NodeType.BUS) {
            BusNode nodeBus = (BusNode) node;
            nodeBus.setPxWidth(nodeBus.getPxWidth() * scaleFactor);
        }
    }

}
