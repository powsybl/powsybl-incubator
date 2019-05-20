/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.cgmes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesNames;
import com.powsybl.cgmes.model.CgmesNames1;
import com.powsybl.cgmes.model.CgmesNamespace;
import com.powsybl.cgmes.model.CgmesTerminal;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class CgmesModelForInterpretation {

    public CgmesModelForInterpretation(String name, CgmesModel cgmes, boolean discreteStep) {
        this.name = name;
        this.cgmes = cgmes;
        this.discreteStep = discreteStep;
        prepareForInterpretation();
    }

    // Used only for unit tests
    public CgmesModelForInterpretation(
        String name,
        Map<String, CgmesNode> nodes,
        Map<String, CgmesLine> lines,
        Map<String, CgmesTransformer> transformers,
        Map<String, List<String>> equipmentAtNode) {
        this.name = name;
        this.cgmes = CGMES_EMPTY_MODEL;
        this.discreteStep = false;
        this.nodes = nodes;

        this.z0nodes = new HashSet<>();
        nodes.keySet().forEach(n -> z0nodes.add(new CgmesZ0Node(Collections.singletonList(n))));

        this.lines = lines;
        this.transformers = transformers;
        this.equipmentAtNode = equipmentAtNode;
        computeIsolatedNodes();
    }

    public void prepareForInterpretation() {
        prepareNodes();
        prepareVoltagesWithMissingNodesAsNodes();
        prepareNodesTotalInjection();

        equipmentAtNode = new HashMap<>();
        prepareLines();
        prepareTransformers();
        computeZ0Nodes();
        computeIsolatedNodes();

        if (LOG.isDebugEnabled()) {
            nodes.keySet().forEach(key -> LOG.debug("node {} ,  {}", key, nodes.get(key)));
            lines.keySet().forEach(key -> LOG.debug("line {} , {}", key, lines.get(key)));
            transformers.keySet()
                .forEach(key -> LOG.debug("transformer {} , {}", key, transformers.get(key)));
        }
    }

    public String name() {
        return name;
    }

    public CgmesModel cgmes() {
        return cgmes;
    }

    public CgmesNode getNode(String id) {
        return nodes.get(id);
    }

    public CgmesLine getLine(String id) {
        return lines.get(id);
    }

    public CgmesTransformer getTransformer(String id) {
        return transformers.get(id);
    }

    public Set<CgmesZ0Node> getZ0Nodes() {
        return z0nodes;
    }

    public boolean isIsolated(CgmesZ0Node z0Node) {
        return isolatedZ0Nodes.contains(z0Node);
    }

    public Map<String, List<String>> equipmentAtNode() {
        return equipmentAtNode;
    }

    public void computeIsolatedNodes() {
        isolatedZ0Nodes = new HashSet<>();
        z0nodes.forEach(z0Node -> {
            if (!hasEquipment(z0Node)) {
                isolatedZ0Nodes.add(z0Node);
            }
        });
    }

    private boolean hasEquipment(CgmesZ0Node z0Node) {
        for (String nodeId : z0Node.nodeIds()) {
            if (equipmentAtNode.containsKey(nodeId)) {
                return true;
            }
        }
        return false;
    }

    // Compute z0 nodes (nodes connected by zero-impedance branches)

    private void computeZ0Nodes() {
        Map<String, List<String>> z0Adjacencies = computeZ0Adjacencies();

        z0nodes = new HashSet<>();
        Set<String> visitedNodes = new HashSet<>();
        nodes.keySet().forEach(nodeId -> {
            if (visitedNodes.contains(nodeId)) {
                return;
            }
            visitedNodes.add(nodeId);
            List<String> z0AdjacentNodes = z0AdjacentNodes(nodeId, z0Adjacencies, visitedNodes);
            z0nodes.add(new CgmesZ0Node(z0AdjacentNodes));
        });
    }

    private List<String> z0AdjacentNodes(
        String nodeId,
        Map<String, List<String>> z0Adjacencies,
        Set<String> visitedNodes) {
        List<String> adjacents = new ArrayList<>();
        adjacents.add(nodeId);
        if (z0Adjacencies.containsKey(nodeId)) {
            int k = 0;
            while (k < adjacents.size()) {
                String adjacent = adjacents.get(k);
                z0Adjacencies.get(adjacent).forEach(otherAdjacent -> {
                    if (visitedNodes.contains(otherAdjacent)) {
                        return;
                    }
                    adjacents.add(otherAdjacent);
                    visitedNodes.add(otherAdjacent);
                });
                k++;
            }
        }
        return adjacents;
    }

    private Map<String, List<String>> computeZ0Adjacencies() {
        Map<String, List<String>> z0Adjacencies = new HashMap<>();
        endNodesOfRetainedSwitchesAsZ0Adjacencies(cgmes, z0Adjacencies);
        cgmes.acLineSegments().forEach(l -> considerZ0Adjacency(l, z0Adjacencies));
        cgmes.equivalentBranches().forEach(eb -> considerZ0Adjacency(eb, z0Adjacencies));
        cgmes.seriesCompensators().forEach(sc -> considerZ0Adjacency(sc, z0Adjacencies));
        cgmes.groupedTransformerEnds().values()
            .forEach(tends -> considerTransformerForZ0Adjacency(tends, z0Adjacencies));
        return z0Adjacencies;
    }

    private void endNodesOfRetainedSwitchesAsZ0Adjacencies(CgmesModel cgmes, Map<String, List<String>> z0Adjacencies) {
        String retainedSwitches = SELECT_WHERE
            + "{ GRAPH ?graph {"
            + "    ?Switch"
            + "        a ?type ;"
            + "        cim:IdentifiedObject.name ?name ;"
            + "        cim:Switch.retained ?retained ;"
            + "        cim:Equipment.EquipmentContainer ?EquipmentContainer ."
            + "    VALUES ?type { cim:Switch cim:Breaker cim:Disconnector } ."
            + "    ?Terminal1"
            + "        a cim:Terminal ;"
            + "        cim:Terminal.ConductingEquipment ?Switch ."
            + "    ?Terminal2"
            + "        a cim:Terminal ;"
            + "        cim:Terminal.ConductingEquipment ?Switch ."
            + "    FILTER ( STR(?Terminal1) < STR(?Terminal2) )"
            + "}} "
            + "OPTIONAL { GRAPH ?graphSSH {"
            + "    ?Switch cim:Switch.open ?open"
            + "}}"
            + "}";
        ((CgmesModelTripleStore) cgmes).query(retainedSwitches).forEach(rs -> {
            Boolean retained = rs.asBoolean("retained", false);
            Boolean open = rs.asBoolean("open", false);
            if (retained && !open) {
                CgmesTerminal t1 = cgmes.terminal(rs.getId(CgmesNames.TERMINAL + "1"));
                CgmesTerminal t2 = cgmes.terminal(rs.getId(CgmesNames.TERMINAL + "2"));
                String nodeId1 = t1.topologicalNode();
                String nodeId2 = t2.topologicalNode();
                addZ0Adjacency(nodeId1, nodeId2, z0Adjacencies);
            }
        });
    }

    private void considerZ0Adjacency(PropertyBag equipment, Map<String, List<String>> z0Adjacencies) {
        CgmesTerminal t1 = cgmes.terminal(equipment.getId(CgmesNames.TERMINAL + 1));
        CgmesTerminal t2 = cgmes.terminal(equipment.getId(CgmesNames.TERMINAL + 2));
        if (!t1.connected() || !t2.connected()) {
            return;
        }
        double r = equipment.asDouble("r");
        double x = equipment.asDouble("x");
        String nodeId1 = t1.topologicalNode();
        String nodeId2 = t2.topologicalNode();
        if (!isZ0(r, x, nodeId1, nodeId2)) {
            return;
        }
        addZ0Adjacency(nodeId1, nodeId2, z0Adjacencies);
    }

    private void considerTransformerForZ0Adjacency(PropertyBags ends, Map<String, List<String>> z0Adjacencies) {
        if (ends.size() != 2) {
            return;
        }
        CgmesTerminal t1 = cgmes.terminal(ends.get(0).getId(CgmesNames.TERMINAL));
        CgmesTerminal t2 = cgmes.terminal(ends.get(1).getId(CgmesNames.TERMINAL));
        if (!t1.connected() || !t2.connected()) {
            return;
        }

        double r1 = ends.get(0).asDouble("r");
        double x1 = ends.get(0).asDouble("x");
        double r2 = ends.get(1).asDouble("r");
        double x2 = ends.get(1).asDouble("x");
        String nodeId1 = t1.topologicalNode();
        String nodeId2 = t2.topologicalNode();
        if (!isZ0(r1 + r2, x1 + x2, nodeId1, nodeId2)) {
            return;
        }
        addZ0Adjacency(nodeId1, nodeId2, z0Adjacencies);
    }

    private void addZ0Adjacency(String nodeId1, String nodeId2, Map<String, List<String>> z0Adjacencies) {
        // nodeId2 is added to the list of z0 adjacent nodes of nodeId1
        // and nodeId1 is added to the list of z0 adjacent nodes of nodeId2
        z0Adjacencies.computeIfAbsent(nodeId1, k -> new ArrayList<>()).add(nodeId2);
        z0Adjacencies.computeIfAbsent(nodeId2, k -> new ArrayList<>()).add(nodeId1);
    }

    // Prepare nodes

    private void prepareNodes() {
        nodes = new HashMap<>();
        cgmes.topologicalNodes().forEach(tn -> {
            CgmesNode n = new CgmesNode(cgmes, tn);
            nodes.put(n.id(), n);
        });
    }

    private void prepareVoltagesWithMissingNodesAsNodes() {
        String svVoltages = SELECT_WHERE
            + "{ GRAPH ?graphSV {"
            + "    ?SvVoltage"
            + "        a cim:SvVoltage ;"
            + "        cim:SvVoltage.TopologicalNode ?TopologicalNode ;"
            + "        cim:SvVoltage.angle ?angle ;"
            + "        cim:SvVoltage.v ?v"
            + "}}"
            + "}";
        ((CgmesModelTripleStore) cgmes).query(svVoltages).forEach(vp -> {
            String nodeId = vp.getId(CgmesNames1.TOPOLOGICAL_NODE);
            double v = vp.asDouble("v", 0.0);
            double angle = vp.asDouble("angle", 0.0);
            nodes.computeIfAbsent(nodeId, id -> new CgmesNode(id, v, angle));
        });
    }

    private void prepareLines() {
        lines = new HashMap<>();
        cgmes.acLineSegments().forEach(l -> prepareLine(l, CgmesNames.AC_LINE_SEGMENT));
        cgmes.equivalentBranches().forEach(eb -> prepareLine(eb, "EquivalentBranch"));
        cgmes.seriesCompensators().forEach(sc -> prepareLine(sc, CgmesNames.SERIES_COMPENSATOR));
    }

    private void prepareLine(PropertyBag equipment, String idPropertyName) {
        CgmesLine line = new CgmesLine(cgmes, equipment, idPropertyName);
        if (!line.connected1() && !line.connected2()) {
            return;
        }
        // Connected z0 lines are used to compute z0 nodes
        // They are considered for flow computation and for
        // adjacencies of z0 nodes
        if (line.connected1()
            && line.connected2()
            && isZ0(line.r(), line.x(), line.nodeId1(), line.nodeId2())) {
            return;
        }

        lines.put(line.id(), line);
        if (line.connected1()) {
            addEquipmentAtNode(line.nodeId1(), line.id());
        }
        if (line.connected2()) {
            addEquipmentAtNode(line.nodeId2(), line.id());
        }
    }

    private void prepareTransformers() {
        Map<String, PropertyBag> allTapChangers = allTapChangers();
        transformers = new HashMap<>();
        cgmes.groupedTransformerEnds().entrySet().forEach(tends -> {
            String transformerId = tends.getKey();
            PropertyBags ends = tends.getValue();
            if (isZ0Transformer(ends)) {
                return;
            }
            if (isConnectedTransformer(ends)) {
                CgmesTransformer transformer = new CgmesTransformer(cgmes, transformerId, ends, allTapChangers, discreteStep);
                transformers.put(transformerId, transformer);
                for (CgmesTransformerEnd end : transformer.ends()) {
                    if (end.connected()) {
                        addEquipmentAtNode(end.nodeId(), transformerId);
                    }
                }
            }
        });
    }

    private void addEquipmentAtNode(String nodeId, String equipmentId) {
        equipmentAtNode.computeIfAbsent(nodeId, z -> new ArrayList<>()).add(equipmentId);
    }

    private Map<String, PropertyBag> allTapChangers() {
        Map<String, PropertyBag> allTapChangers = new HashMap<>();
        cgmes.ratioTapChangers().forEach(rtcp -> allTapChangers.put(rtcp.getId("RatioTapChanger"), rtcp));
        cgmes.phaseTapChangers().forEach(ptcp -> allTapChangers.put(ptcp.getId("PhaseTapChanger"), ptcp));
        return allTapChangers;
    }

    private void prepareNodesTotalInjection() {
        cgmes.energyConsumers().forEach(this::addTerminalFlowAsNodeInjection);
        cgmes.energySources().forEach(this::addTerminalFlowAsNodeInjection);
        cgmes.equivalentInjections().forEach(this::addTerminalFlowAsNodeInjection);
        cgmes.shuntCompensators().forEach(this::addShuntNodeInjection);
        cgmes.staticVarCompensators().forEach(this::addTerminalFlowAsNodeInjection);
        cgmes.asynchronousMachines().forEach(this::addTerminalFlowAsNodeInjection);
        cgmes.synchronousMachines().forEach(this::addTerminalFlowAsNodeInjection);
        cgmes.externalNetworkInjections().forEach(this::addTerminalFlowAsNodeInjection);
        cgmes.acDcConverters().forEach(this::addTerminalFlowAsNodeInjection);
        String svInjection = "SELECT * "
            + "WHERE { "
            + "{ GRAPH ?graphSV {"
            + "    ?SvInjection"
            + "        a cim:SvInjection ;"
            + "        cim:SvInjection.TopologicalNode ?TopologicalNode ;"
            + "        cim:SvInjection.pInjection ?p ;"
            + "        cim:SvInjection.qInjection ?q"
            + "}}"
            + "}";
        ((CgmesModelTripleStore) cgmes).query(svInjection).forEach(i -> {
            String nodeId = i.getId(CgmesNames1.TOPOLOGICAL_NODE);
            CgmesNode node = nodes.get(nodeId);
            node.addP(i.asDouble("p"));
            node.addQ(i.asDouble("q"));
        });
    }

    private void addTerminalFlowAsNodeInjection(PropertyBag equipment) {
        CgmesTerminal t = cgmes.terminal(equipment.getId(CgmesNames.TERMINAL));
        if (!t.connected()) {
            return;
        }
        String nodeId = t.topologicalNode();
        CgmesNode node = nodes.get(nodeId);

        double p = t.flow().p();
        if (Double.isNaN(p)) {
            p = 0.0;
            if (equipment.containsKey("p")) {
                p = equipment.asDouble("p");
            }
        }
        double q = t.flow().q();
        if (Double.isNaN(q)) {
            q = 0.0;
            if (equipment.containsKey("q")) {
                q = equipment.asDouble("q");
            }
        }

        node.addP(p);
        node.addQ(q);
    }

    private void addShuntNodeInjection(PropertyBag equipment) {
        CgmesTerminal t = cgmes.terminal(equipment.getId(CgmesNames.TERMINAL));
        if (!t.connected()) {
            return;
        }
        String nodeId = t.topologicalNode();
        CgmesNode node = nodes.get(nodeId);

        double bPerSection = equipment.asDouble(CgmesNames.B_PER_SECTION);
        double gPerSection = equipment.asDouble("gPerSection");
        int normalSections = equipment.asInt("normalSections", 0);
        double sections = equipment.asDouble("SVsections");
        if (Double.isNaN(sections)) {
            sections = equipment.asDouble("SSHsections", normalSections);
        }

        double pEquipment = gPerSection * sections * node.v() * node.v();
        if (Double.isNaN(gPerSection)) {
            pEquipment = t.flow().p();
            if (Double.isNaN(pEquipment)) {
                pEquipment = 0.0;
            }
        }
        double qEquipment = -bPerSection * sections * node.v() * node.v();
        if (Double.isNaN(bPerSection)) {
            qEquipment = t.flow().q();
            if (Double.isNaN(qEquipment)) {
                qEquipment = 0.0;
            }
        }
        node.addP(pEquipment);
        node.addQ(qEquipment);
    }

    private boolean isConnectedTransformer(PropertyBags ends) {
        boolean connected = false;
        for (PropertyBag end : ends) {
            CgmesTerminal t = cgmes.terminal(end.getId(CgmesNames.TERMINAL));
            if (t.connected()) {
                connected = true;
                break;
            }
        }
        return connected;
    }

    private boolean isZ0Transformer(PropertyBags ends) {
        if (ends.size() == 2) {
            PropertyBag end1 = ends.get(0);
            PropertyBag end2 = ends.get(1);
            CgmesTerminal t1 = cgmes.terminal(end1.getId(CgmesNames.TERMINAL));
            CgmesTerminal t2 = cgmes.terminal(end2.getId(CgmesNames.TERMINAL));

            double r1 = end1.asDouble("r");
            double x1 = end1.asDouble("x");
            double r2 = end2.asDouble("r");
            double x2 = end2.asDouble("x");
            String nodeId1 = t1.topologicalNode();
            String nodeId2 = t2.topologicalNode();
            return t1.connected() && t2.connected() && isZ0(r1 + r2, x1 + x2, nodeId1, nodeId2);
        }
        return false;
    }

    private boolean isZ0(double r, double x, String nodeId1, String nodeId2) {
        double z0Threshold = 0.00025;

        CgmesNode node1 = nodes.get(nodeId1);
        CgmesNode node2 = nodes.get(nodeId2);
        if (node1 == null && node2 == null) {
            return false;
        }

        double v1 = 0.0;
        double vNominal1 = 0.0;
        double angleDegrees1 = 0.0;
        if (node1 != null) {
            v1 = node1.v();
            angleDegrees1 = node1.angle();
            vNominal1 = node1.nominalV();
        }
        double v2 = 0.0;
        double vNominal2 = 0.0;
        double angleDegrees2 = 0.0;
        if (node2 != null) {
            v2 = node2.v();
            angleDegrees2 = node2.angle();
            vNominal2 = node2.nominalV();
        }
        double baseMVA = 100.0;
        double vNominal = 1.0;
        if (!Double.isNaN(vNominal1) && vNominal1 > vNominal) {
            vNominal = vNominal1;
        }
        if (!Double.isNaN(vNominal2) && vNominal2 > vNominal) {
            vNominal = vNominal2;
        }
        if (convertToPerUnit(r, baseMVA, vNominal) <= z0Threshold
            && convertToPerUnit(x, baseMVA, vNominal) <= z0Threshold) {
            return true;
        }
        return v1 == v2 && angleDegrees1 == angleDegrees2;
    }

    private double convertToPerUnit(double impedance, double baseMVA, double vNominal) {
        return impedance * baseMVA / Math.pow(vNominal, 2.0);
    }

    private final String name;
    private final CgmesModel cgmes;
    private final boolean discreteStep;

    private Map<String, CgmesNode> nodes;
    private Map<String, CgmesLine> lines;
    private Map<String, CgmesTransformer> transformers;
    private Map<String, List<String>> equipmentAtNode;
    private Set<CgmesZ0Node> z0nodes;
    private Set<CgmesZ0Node> isolatedZ0Nodes;

    private static final String SELECT_WHERE = "SELECT * WHERE { ";
    private static final Logger LOG = LoggerFactory.getLogger(CgmesModelForInterpretation.class);

    // This is only required for Kron reduction units tests
    public static final CgmesModel CGMES_EMPTY_MODEL = new CgmesModelTripleStore(CgmesNamespace.CIM_16_NAMESPACE,
        TripleStoreFactory.create());
}
