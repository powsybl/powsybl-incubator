/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.interpretation.InterpretationResults.InterpretationAlternativeResults;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesNode;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesZ0Node;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class Interpretation {

    public Interpretation(CgmesModelForInterpretation cgmes) {
        this.cgmes = cgmes;
    }

    public InterpretationResults interpret(List<InterpretationAlternative> alternatives) {
        InterpretationResults i = new InterpretationResults(cgmes.name());
        alternatives.forEach(alternative -> {
            InterpretationAlternativeResults ia = interpret(alternative);
            LOG.info("    {} {}", alternative, ia.error);
            i.interpretationAlternativeResults.put(alternative, ia);
            if (ia.error < i.bestInterpretationError) {
                i.bestInterpretationError = ia.error;
            }
        });
        return i;
    }

    public InterpretationAlternativeResults interpret(InterpretationAlternative alternative) {
        InterpretationAlternativeResults i = new InterpretationAlternativeResults(alternative);

        // Interpret all nodes
        cgmes.getZ0Nodes().forEach(z0node -> {
            NodeInterpretationResult nr = interpret(z0node, alternative);
            i.addDetectedModels(nr);
            i.nodesResults.add(nr);
        });

        // Sort node results by error
        Comparator<NodeInterpretationResult> byError = (
            NodeInterpretationResult nr1,
            NodeInterpretationResult nr2) -> Double.compare(
                nr1.error(),
                nr2.error());
        i.nodesResults.sort(byError.reversed());

        // Sum all node errors to obtain interpretation error
        i.error = i.nodesResults.stream()
            .filter(b -> b.isCalculated() && !b.isIsolated())
            .map(b -> Math.abs(b.p()) + Math.abs(b.q()))
            .mapToDouble(Double::doubleValue)
            .sum();

        return i;
    }

    public NodeInterpretationResult interpret(CgmesZ0Node z0node, InterpretationAlternative alternative) {
        if (cgmes.isIsolated(z0node)) {
            return NodeInterpretationResult.forIsolatedNode(z0node);
        } else {
            return interpretAndComputeBalance(z0node, alternative);
        }
    }

    private NodeInterpretationResult interpretAndComputeBalance(
        CgmesZ0Node z0node,
        InterpretationAlternative alternative) {

        NodeInterpretationResult z0nodeResult = new NodeInterpretationResult(z0node);
        z0node.nodeIds().forEach(nodeId -> {
            // For every node inside the Z0 node
            CgmesNode node = cgmes.getNode(nodeId);
            Objects.requireNonNull(node, "No information available for node id " + nodeId);

            // Add node injection to the Z0 node balance
            z0nodeResult.addP(node.p());
            z0nodeResult.addQ(node.q());

            // Add flow from interpreted connected equipment
            if (cgmes.equipmentAtNode().containsKey(nodeId)) {
                cgmes.equipmentAtNode().get(nodeId).forEach(equipmentId -> {
                    InterpretedComputedFlow flow;
                    flow = InterpretedComputedFlow.forEquipment(equipmentId, nodeId, alternative, cgmes);
                    z0nodeResult.addDetectedModel(flow.detectedEquipmentModel());
                    updateZ0flags(z0nodeResult, flow);
                });
            }
        });
        return z0nodeResult;
    }

    private void updateZ0flags(NodeInterpretationResult z0nodeResult, InterpretedComputedFlow flow) {
        if (flow.isCalculated()) {
            // We do not set b.calculated because it is the default value
            // I would have preferred seeing an explicit assignment always
            z0nodeResult.addP(flow.p());
            z0nodeResult.addQ(flow.q());
            // If we get a badVoltage signaled from any of the flows,
            // the whole results at node are labeled as badVoltage
            if (flow.isBadVoltage()) {
                z0nodeResult.setBadVoltage(true);
            }
            z0nodeResult.setCalculated(flow.isCalculated());
        }
        if (flow.isLine()) {
            z0nodeResult.incLines();
        }
        if (flow.isTransformer2()) {
            z0nodeResult.incTransformers2();
        }
        if (flow.isTransformer3()) {
            z0nodeResult.incTransformers3();
        }
    }

    private CgmesModelForInterpretation cgmes;

    private static final Logger LOG = LoggerFactory.getLogger(Interpretation.class);
}
