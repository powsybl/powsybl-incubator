/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.cgmes;

import com.powsybl.cgmes.dl.conversion.CgmesDLUtils;
import com.powsybl.cgmes.iidm.extensions.dl.*;
import com.powsybl.iidm.network.*;
import com.powsybl.substationdiagram.SubstationDiagram;
import com.powsybl.substationdiagram.layout.*;
import com.powsybl.substationdiagram.library.ComponentLibrary;
import com.powsybl.substationdiagram.library.ComponentType;
import com.powsybl.substationdiagram.library.ResourcesComponentLibrary;
import com.powsybl.substationdiagram.model.*;
import com.powsybl.substationdiagram.svg.DefaultSubstationDiagramStyleProvider;
import com.powsybl.substationdiagram.svg.SubstationDiagramStyleProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.eu>
 */
public class LayoutToCgmesExtensionsConverter {

    private static final Logger LOG = LoggerFactory.getLogger(LayoutToCgmesExtensionsConverter.class);

    private static final double OFFSET_MULTIPLIER_X = 2.0;

    private final LayoutParameters lparams;
    private final ComponentLibrary convergenceComponentLibrary;
    private final SubstationDiagramStyleProvider sProvider;
    private final SubstationLayoutFactory sFactory;
    private final VoltageLevelLayoutFactory vFactory;
    private final boolean showNames;

    public LayoutToCgmesExtensionsConverter(SubstationLayoutFactory sFactory, VoltageLevelLayoutFactory vFactory, LayoutParameters lparams, ComponentLibrary convergenceComponentLibrary, SubstationDiagramStyleProvider sProvider, boolean showNames) {
        this.sFactory = Objects.requireNonNull(sFactory);
        this.vFactory = Objects.requireNonNull(vFactory);
        this.lparams = Objects.requireNonNull(lparams);
        this.convergenceComponentLibrary = Objects.requireNonNull(convergenceComponentLibrary);
        this.sProvider = Objects.requireNonNull(sProvider);
        this.showNames = showNames;
    }

    public LayoutToCgmesExtensionsConverter() {
        //set a default source layout: PositionVoltageLevelLayout
        lparams = new LayoutParameters();
        convergenceComponentLibrary = new ResourcesComponentLibrary("/ConvergenceLibrary");
        sProvider = new DefaultSubstationDiagramStyleProvider();
        sFactory = new HorizontalSubstationLayoutFactory();
        vFactory = new PositionVoltageLevelLayoutFactory(new PositionFree());
        showNames = true;
    }

    private boolean isLineNode(Node node) {
        return Arrays.asList(ComponentType.LINE, ComponentType.DANGLING_LINE, ComponentType.VSC_CONVERTER_STATION).contains(node.getComponentType());
    }

    private String getBranchId(String branchNodeId) {
        return branchNodeId.substring(0, branchNodeId.lastIndexOf('_'));
    }

    private int getMaxSeq(List<DiagramPoint> diagramPoints) {
        Objects.requireNonNull(diagramPoints);
        return diagramPoints.stream().sorted(Comparator.reverseOrder()).findFirst().orElse(new DiagramPoint(0, 0, 0)).getSeq();
    }

    private NodeDiagramData setNodeDiagramPoints(NodeDiagramData diagramData, BusNode busNode, OffsetPoint offsetPoint, String diagramName) {
        double x1 = busNode.getX();
        double y1 = busNode.getY();
        double x2 = x1;
        double y2 = y1;
        double pxWidth = busNode.getPxWidth();
        boolean rotatedBus = busNode.isRotated();
        if (!rotatedBus) {
            x2 = x1 + pxWidth;
        } else {
            y2 = y1 + pxWidth;
        }

        NodeDiagramData.NodeDiagramDataDetails diagramDetails = diagramData.new NodeDiagramDataDetails();
        DiagramPoint p1 = offsetPoint.newDiagramPoint(x1, y1, 1);
        DiagramPoint p2 = offsetPoint.newDiagramPoint(x2, y2, 2);
        diagramDetails.setPoint1(p1);
        diagramDetails.setPoint2(p2);
        diagramData.addData(diagramName, diagramDetails);
        return diagramData;
    }

    private LayoutInfo applyLayout(Substation substation, double xoffset, double yoffset, String diagramName) {
        //make a diagram for the substation
        SubstationDiagram diagram = SubstationDiagram.build(substation, sFactory, vFactory, showNames);
        OffsetPoint offsetPoint = new OffsetPoint(xoffset, yoffset);

        try (StringWriter svgWriter = new StringWriter();
             StringWriter metadataWriter = new StringWriter()) {

            //apply the specified layout and retrieve the computed graph
            diagram.writeSvg(convergenceComponentLibrary, lparams, sProvider, svgWriter, metadataWriter, null);
            SubstationGraph sgraph = diagram.getGraph();

            LayoutInfo subsBoundary = new LayoutInfo(0.0, 0.0);
            substation.getVoltageLevelStream().forEach(voltageLevel -> {
                Graph vlGraph = sgraph.getNode(voltageLevel.getId());

                // remove fictitious nodes&switches (no CGMES DL data available for them)
                vlGraph.removeUnnecessaryFictitiousNodes();
                vlGraph.removeFictitiousSwitchNodes();

                double vlNodeMaxX = vlGraph.getNodes().stream().map(Node::getX).sorted(Collections.reverseOrder()).findFirst().orElse(0.0);
                double vlNodeMaxY = vlGraph.getNodes().stream().map(Node::getY).sorted(Collections.reverseOrder()).findFirst().orElse(0.0);
                subsBoundary.update(vlNodeMaxX, vlNodeMaxY);

                List<ComponentType> componentTypeList = vlGraph.getNodes().stream().map(Node::getComponentType).collect(Collectors.toList());
                LOG.debug("Voltage level id: {} ({}); {} ;component types: {}; max x,y: {}, {}", voltageLevel.getId(), voltageLevel.getName(), voltageLevel.getTopologyKind(), componentTypeList, vlNodeMaxX, vlNodeMaxY);

                //iterate over the voltage level's equipments, and fill the IIDM CGMES DL extensions with the computed layout info
                voltageLevel.getLoadStream().forEach(load -> {
                    Node node = vlGraph.getNode(load.getId());
                    if (node != null) {
                        DiagramPoint lDiagramPoint = offsetPoint.newDiagramPoint(node.getX(), node.getY(), 0);
                        InjectionDiagramData<Load> loadIidmDiagramData = new InjectionDiagramData<>(load);
                        InjectionDiagramData.InjectionDiagramDetails diagramDetails = loadIidmDiagramData.new InjectionDiagramDetails(lDiagramPoint, 0);
                        loadIidmDiagramData.addData(diagramName, diagramDetails);
                        LOG.debug("setting CGMES DL IIDM extensions for Load: {}, {}", load.getId(), lDiagramPoint);
                        load.addExtension(InjectionDiagramData.class, loadIidmDiagramData);
                    }
                });

                voltageLevel.getGeneratorStream().forEach(generator -> {
                    Node node = vlGraph.getNode(generator.getId());
                    if (node != null) {
                        DiagramPoint gDiagramPoint = offsetPoint.newDiagramPoint(node.getX(), node.getY(), 0);
                        InjectionDiagramData<Generator> gIidmDiagramData = new InjectionDiagramData<>(generator);
                        InjectionDiagramData.InjectionDiagramDetails diagramDetails = gIidmDiagramData.new InjectionDiagramDetails(gDiagramPoint, 0);
                        gIidmDiagramData.addData(diagramName, diagramDetails);
                        LOG.debug("setting CGMES DL IIDM extensions for Generator: {}, {}", generator.getId(), gDiagramPoint);
                        generator.addExtension(InjectionDiagramData.class, gIidmDiagramData);
                    }
                });

                voltageLevel.getShuntCompensatorStream().forEach(shuntCompensator -> {
                    Node node = vlGraph.getNode(shuntCompensator.getId());
                    if (node != null) {
                        DiagramPoint scDiagramPoint = offsetPoint.newDiagramPoint(node.getX(), node.getY(), 0);
                        InjectionDiagramData<ShuntCompensator> scDiagramData = new InjectionDiagramData<>(shuntCompensator);
                        InjectionDiagramData.InjectionDiagramDetails diagramDetails = scDiagramData.new InjectionDiagramDetails(scDiagramPoint, 0);
                        scDiagramData.addData(diagramName, diagramDetails);
                        LOG.debug("setting CGMES DL IIDM extensions for ShuntCompensator: {}, {}", shuntCompensator.getId(), scDiagramPoint);
                        shuntCompensator.addExtension(InjectionDiagramData.class, scDiagramData);
                    }
                });

                voltageLevel.getStaticVarCompensatorStream().forEach(staticVarCompensator -> {
                    Node node = vlGraph.getNode(staticVarCompensator.getId());
                    if (node != null) {
                        DiagramPoint svcDiagramPoint = offsetPoint.newDiagramPoint(node.getX(), node.getY(), 0);
                        InjectionDiagramData<StaticVarCompensator> svcDiagramData = new InjectionDiagramData<>(staticVarCompensator);
                        InjectionDiagramData.InjectionDiagramDetails diagramDetails = svcDiagramData.new InjectionDiagramDetails(svcDiagramPoint, 0);
                        svcDiagramData.addData(diagramName, diagramDetails);
                        LOG.debug("setting CGMES DL IIDM extensions for StaticVarCompensator: {}, {}", staticVarCompensator.getId(), svcDiagramPoint);
                        staticVarCompensator.addExtension(InjectionDiagramData.class, svcDiagramData);
                    }
                });

                substation.getTwoWindingsTransformerStream().forEach(twoWindingsTransformer -> {
                    vlGraph.getNodes().stream().filter(node -> node.getId().startsWith(twoWindingsTransformer.getId())).findFirst().ifPresent(node -> {
                        if (node.getComponentType().equals(ComponentType.TWO_WINDINGS_TRANSFORMER)) {
                            FeederNode transformerNode = (FeederNode) node;
                            DiagramPoint tDiagramPoint = offsetPoint.newDiagramPoint(transformerNode.getX(), transformerNode.getY(), transformerNode.getOrder());
                            CouplingDeviceDiagramData<TwoWindingsTransformer> transformerIidmDiagramData = new CouplingDeviceDiagramData<>(twoWindingsTransformer);
                            CouplingDeviceDiagramData.CouplingDeviceDiagramDetails diagramDetails = transformerIidmDiagramData.new CouplingDeviceDiagramDetails(tDiagramPoint, transformerNode.isRotated() ? 0 : 180);
                            transformerIidmDiagramData.addData(diagramName, diagramDetails);
                            LOG.debug("setting CGMES DL IIDM extensions for TwoWindingTransformer: {}, {}", twoWindingsTransformer.getId(), tDiagramPoint);
                            twoWindingsTransformer.addExtension(CouplingDeviceDiagramData.class, transformerIidmDiagramData);
                        }
                    });
                });

                substation.getThreeWindingsTransformerStream().forEach(threeWindingsTransformer -> {
                    vlGraph.getNodes().stream().filter(node -> node.getId().startsWith(threeWindingsTransformer.getId())).findFirst().ifPresent(node -> {
                        if (node.getComponentType().equals(ComponentType.THREE_WINDINGS_TRANSFORMER)) {
                            FeederNode transformerNode = (FeederNode) node;
                            DiagramPoint tDiagramPoint = offsetPoint.newDiagramPoint(transformerNode.getX(), transformerNode.getY(), transformerNode.getOrder());
                            ThreeWindingsTransformerDiagramData transformerIidmDiagramData = new ThreeWindingsTransformerDiagramData(threeWindingsTransformer);
                            ThreeWindingsTransformerDiagramData.ThreeWindingsTransformerDiagramDataDetails diagramDetails = transformerIidmDiagramData.new ThreeWindingsTransformerDiagramDataDetails(tDiagramPoint, transformerNode.isRotated() ? 0 : 180);
                            transformerIidmDiagramData.addData(diagramName, diagramDetails);
                            LOG.debug("setting CGMES DL IIDM extensions for ThreeWindingTransformer: {}, {}", threeWindingsTransformer.getId(), tDiagramPoint);
                            threeWindingsTransformer.addExtension(ThreeWindingsTransformerDiagramData.class, transformerIidmDiagramData);
                        }
                    });
                });

                vlGraph.getNodes().stream().filter(this::isLineNode).forEach(node -> {
                    switch (node.getComponentType()) {
                        case LINE:
                            FeederNode lineNode = (FeederNode) node;
                            Line line = vlGraph.getVoltageLevel().getConnectable(getBranchId(lineNode.getId()), Line.class);

                            LineDiagramData<Line> lineDiagramData = line.getExtension(LineDiagramData.class);
                            if (lineDiagramData == null) {
                                lineDiagramData = new LineDiagramData<Line>(line);
                            }
                            int lineSeq = getMaxSeq(lineDiagramData.getPoints(diagramName)) + 1;
                            DiagramPoint linePoint = offsetPoint.newDiagramPoint(lineNode.getX(), lineNode.getY(), lineSeq);
                            lineDiagramData.addPoint(diagramName, linePoint);

                            LOG.debug("setting CGMES DL IIDM extensions for Line {} ({}), new point {}", line.getId(), line.getName(), linePoint);
                            line.addExtension(LineDiagramData.class, lineDiagramData);
                            break;
                        case DANGLING_LINE:
                            FeederNode danglingLineNode = (FeederNode) node;
                            DanglingLine danglingLine = vlGraph.getVoltageLevel().getConnectable(danglingLineNode.getId(), DanglingLine.class);

                            LineDiagramData<DanglingLine> danglingLineDiagramData = danglingLine.getExtension(LineDiagramData.class);
                            if (danglingLineDiagramData == null) {
                                danglingLineDiagramData = new LineDiagramData<DanglingLine>(danglingLine);
                            }
                            int danglingLineSeq = getMaxSeq(danglingLineDiagramData.getPoints(diagramName)) + 1;
                            DiagramPoint danglingLinePoint = offsetPoint.newDiagramPoint(danglingLineNode.getX(), danglingLineNode.getY(), danglingLineSeq);
                            danglingLineDiagramData.addPoint(diagramName, danglingLinePoint);

                            LOG.debug("setting CGMES DL IIDM extensions for Dangling line {} ({}),  point {}", danglingLine.getId(), danglingLine.getName(), danglingLinePoint);
                            danglingLine.addExtension(LineDiagramData.class, danglingLineDiagramData);
                            break;
                        default:
                            break;
                    }
                });

                if (TopologyKind.BUS_BREAKER.equals(voltageLevel.getTopologyKind())) {
                    voltageLevel.getBusBreakerView().getBusStream().forEach(bus -> {
                        vlGraph.getNodeBuses().stream().filter(busNode -> busNode.getId().equals(bus.getId())).findFirst().ifPresent(busNode -> {
                            NodeDiagramData<Bus> busDiagramData = bus.getExtension(NodeDiagramData.class) != null ? bus.getExtension(NodeDiagramData.class) : new NodeDiagramData<Bus>(bus);
                            setNodeDiagramPoints(busDiagramData, busNode, offsetPoint, diagramName);
                            LOG.debug("setting CGMES DL IIDM extensions for Bus {}, {} - {}", bus.getId(), busDiagramData.getData(diagramName).getPoint1(), busDiagramData.getData(diagramName).getPoint2());
                            bus.addExtension(NodeDiagramData.class, busDiagramData);
                        });
                    });

                } else {
                    voltageLevel.getNodeBreakerView().getBusbarSectionStream().forEach(busbarSection -> {
                        vlGraph.getNodeBuses().stream().filter(busNode -> busNode.getId().equals(busbarSection.getId())).findFirst().ifPresent(busNode -> {
                            NodeDiagramData<BusbarSection> busbarSectionDiagramData = busbarSection.getExtension(NodeDiagramData.class) != null ? busbarSection.getExtension(NodeDiagramData.class) : new NodeDiagramData<BusbarSection>(busbarSection);
                            setNodeDiagramPoints(busbarSectionDiagramData, busNode, offsetPoint, diagramName);
                            LOG.debug("setting CGMES DL IIDM extensions for BusbarSection {}, {} - {}", busbarSection.getId(), busbarSectionDiagramData.getData(diagramName).getPoint1(), busbarSectionDiagramData.getData(diagramName).getPoint2());
                            busbarSection.addExtension(NodeDiagramData.class, busbarSectionDiagramData);
                        });
                    });

                    voltageLevel.getNodeBreakerView().getSwitches().forEach(sw -> {
                        Node swNode = vlGraph.getNode(sw.getId());
                        if (swNode != null) {
                            double rot = swNode.isRotated() ? 90.0 : 0.0;
                            CouplingDeviceDiagramData<Switch> switchIidmDiagramData = new CouplingDeviceDiagramData<>(sw);
                            CouplingDeviceDiagramData.CouplingDeviceDiagramDetails diagramDetails = switchIidmDiagramData.new CouplingDeviceDiagramDetails(offsetPoint.newDiagramPoint(swNode.getX(), swNode.getY(), 0), rot);
                            switchIidmDiagramData.addData(diagramName, diagramDetails);
                            LOG.debug("setting CGMES DL IIDM extensions for Switch {}, {} - {}", sw.getId(), switchIidmDiagramData);
                            sw.addExtension(CouplingDeviceDiagramData.class, switchIidmDiagramData);
                        }
                    });
                }
            });

            return subsBoundary;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void convertLayout(Network network, Stream<Substation> subsStream, String diagramName) {
        //creates a single CGMES-DL diagram (named diagramName), where each substation
        NetworkDiagramData.addDiagramName(network, diagramName);
        final double[] xoffset = {0.0};
        subsStream.forEach(s -> {
            LOG.debug("Substation {}({} offset: {})", s.getId(), s.getName(), xoffset[0]);
            LayoutInfo li = applyLayout(s, xoffset[0], 0.0, diagramName);
            xoffset[0] += OFFSET_MULTIPLIER_X * li.getMaxX();
        });
    }

    private void convertLayout(Network network, Stream<Substation> subsStream) {
        // creates one CGMES-DL diagram for each substation (where each diagram name is the substation's name)
        subsStream.forEach(s -> {
            String subDiagramName = StringUtils.isEmpty(s.getName()) ? s.getId() : s.getName();
            NetworkDiagramData.addDiagramName(network, subDiagramName);
            LOG.debug("Substation {}", subDiagramName);
            applyLayout(s, 0.0, 0.0, subDiagramName);
        });
    }

    public void convertLayout(Network network, String diagramName) {
        Objects.requireNonNull(network);
        LOG.info("Converting layout {} to IIDM CGMES DL extensions for network: {}", sFactory.getClass(), network.getId());

        //Network could have already defined a set of iidm cgmes extensions, as loaded via the cgmes importer/cgmesDLImport postprocessor.
        //Also associated to the network, we have the triplestore with the DL related triples
        //clear the  CGMES DL profile data from the network's CGMES tiplestore, if it already exists
        //and remove any exising IIDM CGMES equipments' extensions
        CgmesDLUtils.clearCgmesDl(network);
        CgmesDLUtils.removeIidmCgmesExtensions(network);

        //A CGMES-DL diagram refers to a global coordinate system and  can include all the network equipments.
        // whereas Layouts are created per-substation (or per-voltage), using a coordinate system that is local to the specific substation.
        //If diagramName
        // - is null, this method creates one CGMES-DL diagram for each substation (where each diagram name is the substation's name)
        // - is not null, creates a single CGMES-DL diagram (named diagramName), where each substation
        // diagram is placed one a single row, one next to the other.

        if (diagramName != null) {
            convertLayout(network, network.getSubstationStream(), diagramName);
        } else {
            convertLayout(network, network.getSubstationStream());
        }
    }

    class LayoutInfo {
        double maxX;
        double maxY;

        LayoutInfo(double maxNodeX, double maxNodeY) {
            this.maxX = maxNodeX;
            this.maxY = maxNodeY;
        }

        double getMaxX() {
            return maxX;
        }

        double getMaxY() {
            return maxY;
        }

        void update(double maxX, double maxY) {
            if (maxX > this.maxX) {
                this.maxX = maxX;
            }
            if (maxY > this.maxY) {
                this.maxY = maxY;
            }
        }
    }

    class OffsetPoint {
        private final double dx;
        private final double dy;

        OffsetPoint(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }

        DiagramPoint newDiagramPoint(double x, double y, int seq) {
            return new DiagramPoint(x + dx, y + dy, seq);
        }
    }
}
