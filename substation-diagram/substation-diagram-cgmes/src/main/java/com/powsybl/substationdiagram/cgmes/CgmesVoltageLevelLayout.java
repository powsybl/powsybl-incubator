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

    private Graph graph;
    private double minX = 0;
    private double minY = 0;
    private final double marginX = 20;
    private final double marginY = 10;

    public CgmesVoltageLevelLayout(Graph graph) {
        Objects.requireNonNull(graph);
        // remove fictitious nodes (no CGMES DL data available for them)
        graph.removeUnnecessaryFictitiousNodes();
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
            shiftNodeCoordinates(node);
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
                BusNode nodeBus = (BusNode) node;
                if (nodeBus.getIdentifiable() instanceof BusbarSection) {
                    BusbarSection busbar = (BusbarSection) nodeBus.getIdentifiable();
                    NodeDiagramData<BusbarSection> busbarDiagramData = busbar.getExtension(NodeDiagramData.class);
                    if (busbarDiagramData != null) {
                        nodeBus.setX(busbarDiagramData.getPoint1().getX());
                        nodeBus.setY(busbarDiagramData.getPoint1().getY());
                        nodeBus.setPxWidth(computeBusbarWidth(busbarDiagramData));
                        if (busbarDiagramData.getPoint1().getX() == busbarDiagramData.getPoint2().getX()) {
                            nodeBus.setRotated(true);
                        }
                        setMin(busbarDiagramData.getPoint1().getX(), busbarDiagramData.getPoint1().getY());
                    } else {
                        LOG.warn("No CGMES-DL data for {} node {}, busbar {}", node.getType(), node.getId(), busbar.getName());
                    }
                } else {
                    Bus bus = (Bus) nodeBus.getIdentifiable();
                    NodeDiagramData<Bus> busDiagramData = bus.getExtension(NodeDiagramData.class);
                    if (busDiagramData != null) {
                        nodeBus.setX(busDiagramData.getPoint1().getX());
                        nodeBus.setY(busDiagramData.getPoint1().getY());
                        nodeBus.setPxWidth(computeBusbarWidth(busDiagramData));
                        if (busDiagramData.getPoint1().getX() == busDiagramData.getPoint2().getX()) {
                            nodeBus.setRotated(true);
                        }
                        setMin(busDiagramData.getPoint1().getX(), busDiagramData.getPoint1().getY());
                    } else {
                        LOG.warn("No CGMES-DL data for {} node {}, bus {}", node.getType(), node.getId(), bus.getName());
                    }
                }
                break;
            case SWITCH:
                SwitchNode nodeSwitch = (SwitchNode) node;
                Switch sw = (Switch) nodeSwitch.getIdentifiable();
                if (sw != null) {
                    CouplingDeviseDiagramData<Switch> switchDiagramData = sw.getExtension(CouplingDeviseDiagramData.class);
                    if (switchDiagramData != null) {
                        nodeSwitch.setX(switchDiagramData.getPoint().getX());
                        nodeSwitch.setY(switchDiagramData.getPoint().getY());
                        if (switchDiagramData.getRotation() == 90 || switchDiagramData.getRotation() == 270) {
                            nodeSwitch.setRotated(true);
                        }
                        setMin(switchDiagramData.getPoint().getX(), switchDiagramData.getPoint().getY());
                    } else {
                        LOG.warn("No CGMES-DL data for {} node {}, switch {}", node.getType(), node.getId(), sw.getName());
                    }
                }
                break;
            case FEEDER:
                setFeederNodeCoordinates(node);
                break;
            default:
                break;
        }
    }

    private double computeBusbarWidth(NodeDiagramData<?> busbarDiagramData) {
        if (busbarDiagramData.getPoint1().getX() == busbarDiagramData.getPoint2().getX()) {
            return Math.abs(busbarDiagramData.getPoint1().getY() - busbarDiagramData.getPoint2().getY());
        } else {
            return Math.abs(busbarDiagramData.getPoint1().getX() - busbarDiagramData.getPoint2().getX());
        }
    }

    private void setFeederNodeCoordinates(Node node) {
        switch (node.getComponentType()) {
            case LOAD:
                FeederNode loadNode = (FeederNode) node;
                Load load = (Load) loadNode.getIdentifiable();
                InjectionDiagramData<Load> loadDiagramData = load.getExtension(InjectionDiagramData.class);
                if (loadDiagramData != null) {
                    loadNode.setX(loadDiagramData.getPoint().getX());
                    loadNode.setY(loadDiagramData.getPoint().getY());
                    if (loadDiagramData.getRotation() == 90 || loadDiagramData.getRotation() == 270) {
                        loadNode.setRotated(true);
                    }
                    setMin(loadDiagramData.getPoint().getX(), loadDiagramData.getPoint().getY());
                } else {
                    LOG.warn("No CGMES-DL data for {} {} node {}, load {}", node.getType(), node.getComponentType(), node.getId(), load.getName());
                }
                break;
            case GENERATOR:
                FeederNode generatorNode = (FeederNode) node;
                Generator generator = (Generator) generatorNode.getIdentifiable();
                InjectionDiagramData<Generator> generatorDiagramData = generator.getExtension(InjectionDiagramData.class);
                if (generatorDiagramData != null) {
                    generatorNode.setX(generatorDiagramData.getPoint().getX());
                    generatorNode.setY(generatorDiagramData.getPoint().getY());
                    setMin(generatorDiagramData.getPoint().getX(), generatorDiagramData.getPoint().getY());
                } else {
                    LOG.warn("No CGMES-DL data for {} {} node {}, generator {}", node.getType(), node.getComponentType(), node.getId(), generator.getName());
                }
                break;
            case CAPACITOR:
            case INDUCTOR:
                FeederNode shuntNode = (FeederNode) node;
                ShuntCompensator shunt = (ShuntCompensator) shuntNode.getIdentifiable();
                InjectionDiagramData<ShuntCompensator> shuntDiagramData = shunt.getExtension(InjectionDiagramData.class);
                if (shuntDiagramData != null) {
                    shuntNode.setX(shuntDiagramData.getPoint().getX());
                    shuntNode.setY(shuntDiagramData.getPoint().getY());
                    if (shuntDiagramData.getRotation() == 90 || shuntDiagramData.getRotation() == 270) {
                        shuntNode.setRotated(true);
                    }
                    setMin(shuntDiagramData.getPoint().getX(), shuntDiagramData.getPoint().getY());
                } else {
                    LOG.warn("No CGMES-DL data for {} {} node {}, shunt {}", node.getType(), node.getComponentType(), node.getId(), shunt.getName());
                }
                break;
            case STATIC_VAR_COMPENSATOR:
                FeederNode svcNode = (FeederNode) node;
                StaticVarCompensator svc = (StaticVarCompensator) svcNode.getIdentifiable();
                InjectionDiagramData<StaticVarCompensator> svcDiagramData = svc.getExtension(InjectionDiagramData.class);
                if (svcDiagramData != null) {
                    svcNode.setX(svcDiagramData.getPoint().getX());
                    svcNode.setY(svcDiagramData.getPoint().getY());
                    if (svcDiagramData.getRotation() == 90 || svcDiagramData.getRotation() == 270) {
                        svcNode.setRotated(true);
                    }
                    setMin(svcDiagramData.getPoint().getX(), svcDiagramData.getPoint().getY());
                } else {
                    LOG.warn("No CGMES-DL data for {} {} node {}, svc {}", node.getType(), node.getComponentType(), node.getId(), svc.getName());
                }
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

    private void setLineNodeCoordinates(Node node) {
        LOG.info("Setting coordinates of node {}, type {}, component type {}", node.getId(), node.getType(), node.getComponentType());
        switch (node.getComponentType()) {
            case LINE:
                FeederNode lineNode = (FeederNode) node;
                Line line = (Line) lineNode.getIdentifiable();
                LineDiagramData<Line> lineDiagramData = line.getExtension(LineDiagramData.class);
                if (lineDiagramData != null) {
                    DiagramPoint linePoint = getLinePoint(lineDiagramData, lineNode);
                    lineNode.setX(linePoint.getX());
                    lineNode.setY(linePoint.getY());
                    setMin(linePoint.getX(), linePoint.getY());
                } else {
                    LOG.warn("No CGMES-DL data for {} {} node {}, line {}", node.getType(), node.getComponentType(), node.getId(), line.getName());
                }
                break;
            case DANGLING_LINE:
                FeederNode danglingLineNode = (FeederNode) node;
                DanglingLine danglingLine = (DanglingLine) danglingLineNode.getIdentifiable();
                LineDiagramData<DanglingLine> danglingLineDiagramData = danglingLine.getExtension(LineDiagramData.class);
                if (danglingLineDiagramData != null) {
                    DiagramPoint danglingLinePoint = getLinePoint(danglingLineDiagramData, danglingLineNode);
                    danglingLineNode.setX(danglingLinePoint.getX());
                    danglingLineNode.setY(danglingLinePoint.getY());
                    setMin(danglingLinePoint.getX(), danglingLinePoint.getY());
                } else {
                    LOG.warn("No CGMES-DL data for {} {} node {}, dangling line {}", node.getType(), node.getComponentType(), node.getId(), danglingLine.getName());
                }
                break;
            default:
                break;
        }
    }

    private <T> DiagramPoint getLinePoint(LineDiagramData<?> lineDiagramData, Node lineNode) {
        DiagramPoint adjacentNodePoint = getLineAdjacentNodePoint(lineNode);
        if (adjacentNodePoint != null) {
            double firstPointDistance = Math.hypot(lineDiagramData.getFirstPoint().getX() - adjacentNodePoint.getX(),
                                                   lineDiagramData.getFirstPoint().getY() - adjacentNodePoint.getY());
            double lastPointDistance = Math.hypot(lineDiagramData.getLastPoint().getX() - adjacentNodePoint.getX(),
                                                  lineDiagramData.getLastPoint().getY() - adjacentNodePoint.getY());
            if (firstPointDistance > lastPointDistance) {
                return lineDiagramData.getLastPoint();
            } else {
                return lineDiagramData.getFirstPoint();
            }
        }
        return lineDiagramData.getFirstPoint();
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

    private void shiftNodeCoordinates(Node node) {
        node.setX(node.getX() - minX + marginX);
        node.setY(node.getY() - minY + marginY);
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
