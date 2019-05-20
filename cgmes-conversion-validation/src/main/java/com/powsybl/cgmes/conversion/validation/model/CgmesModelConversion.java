package com.powsybl.cgmes.conversion.validation.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesZ0Node;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.TripleStore;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class CgmesModelConversion extends CgmesModelTripleStore {

    private static final String V = "v";
    private static final String ANGLE = "angle";

    public CgmesModelConversion(String name, String nameSpace, TripleStore tripleStore) {
        super(nameSpace, tripleStore);
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void z0Nodes() {
        topologicalNodes = new HashMap<>();
        super.topologicalNodes().forEach(tn -> topologicalNodes.put(tn.getId("TopologicalNode"), tn));

        CgmesModelForInterpretation modelForInterpretation = new CgmesModelForInterpretation("topological", this, true);
        Set<CgmesZ0Node> z0Nodes = modelForInterpretation.getZ0Nodes();
        z0Nodes.forEach(z0Node -> {
            if (z0Node.nodeIds().size() < 2) {
                return;
            }
            String nodeId = getZ0Node(z0Node.nodeIds(), topologicalNodes);
            if (nodeId == null) {
                return;
            }
            double v = topologicalNodes.get(nodeId).asDouble(V);
            double angle = topologicalNodes.get(nodeId).asDouble(ANGLE);
            z0Node.nodeIds().forEach(node -> {
                if (topologicalNodes.containsKey(node)) {
                    PropertyBag n = topologicalNodes.get(node);
                    n.put(V, Double.toString(v));
                    n.put(ANGLE, Double.toString(angle));
                }
            });
        });
    }

    private String getZ0Node(List<String> nodeIds, Map<String, PropertyBag> nodes) {
        for (String nodeId : nodeIds) {
            double v = nodes.get(nodeId).asDouble(V);
            double angle = nodes.get(nodeId).asDouble(ANGLE);
            if (!Double.isNaN(v) && v != 0.0 && !Double.isNaN(angle)) {
                return nodeId;
            }
        }
        return null;
    }

    @Override
    public boolean isNodeBreaker() {
        return false;
    }

    @Override
    public PropertyBags topologicalNodes() {
        return new PropertyBags(topologicalNodes.values());
    }

    private final String name;
    private Map<String, PropertyBag> topologicalNodes;
}
