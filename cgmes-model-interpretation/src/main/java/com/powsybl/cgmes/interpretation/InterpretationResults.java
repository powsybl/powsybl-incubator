/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.powsybl.cgmes.interpretation.model.interpreted.DetectedEquipmentModel;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretationResults {

    public InterpretationResults(String cgmesName) {
        this.cgmesName = cgmesName;
        this.bestInterpretationError = Double.POSITIVE_INFINITY;
    }

    public String cgmesName() {
        return cgmesName;
    }

    public static class InterpretationAlternativeResults {

        InterpretationAlternativeResults(InterpretationAlternative alternative) {
            this.alternative = alternative;
            error = 0.0;
            detectedModels = new HashMap<>();
            nodesResults = new ArrayList<>();
        }

        public InterpretationAlternative alternative() {
            return alternative;
        }

        public double error() {
            return error;
        }

        public Collection<NodeInterpretationResult> nodesResults() {
            return nodesResults;
        }

        public Map<String, DetectedEquipmentModel> detectedModels() {
            return detectedModels;
        }

        public void addDetectedModels(NodeInterpretationResult nodeResult) {
            nodeResult.detectedModels().keySet().forEach(eqm -> {
                DetectedEquipmentModel eqModel = nodeResult.detectedModels().get(eqm);
                DetectedEquipmentModel aggregateEqModel = detectedModels.computeIfAbsent(eqm,
                    id -> new DetectedEquipmentModel(eqModel.detectedBranchModels()));
                if (nodeResult.isCalculated() && !nodeResult.isIsolated()) {
                    if (nodeResult.isErrorOk()) {
                        aggregateEqModel.incTotal(eqModel.total());
                        aggregateEqModel.incCalculated(eqModel.total());
                        aggregateEqModel.incOk(eqModel.total());
                    } else {
                        aggregateEqModel.incTotal(eqModel.total());
                        aggregateEqModel.incCalculated(eqModel.total());
                    }
                } else {
                    aggregateEqModel.incTotal(eqModel.total());
                }
                detectedModels.put(eqm, aggregateEqModel);
            });
        }

        private final InterpretationAlternative alternative;
        double error;
        List<NodeInterpretationResult> nodesResults;
        Map<String, DetectedEquipmentModel> detectedModels;
    }

    public double error() {
        return bestInterpretationError;
    }

    public Map<InterpretationAlternative, InterpretationAlternativeResults> interpretationAlternativeResults() {
        return interpretationAlternativeResults;
    }

    public Exception exception() {
        return exception;
    }

    public long countBadNodes() {
        return interpretationAlternativeResults.values().stream()
            .map(alternativeResults -> alternativeResults.nodesResults().stream()
                .filter(b -> b.isCalculated() && !b.isBadVoltage() && !b.isErrorOk())
                .count())
            .mapToLong(Long::longValue)
            .sum();
    }

    private final String cgmesName;
    double bestInterpretationError;
    Map<InterpretationAlternative, InterpretationAlternativeResults> interpretationAlternativeResults = new HashMap<>();
    Exception exception;
}
