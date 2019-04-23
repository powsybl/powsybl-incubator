/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation;

import java.util.HashMap;
import java.util.Map;

import com.powsybl.cgmes.interpretation.model.cgmes.CgmesZ0Node;
import com.powsybl.cgmes.interpretation.model.interpreted.DetectedEquipmentModel;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class NodeInterpretationResult {

    public NodeInterpretationResult(CgmesZ0Node node) {
        this.z0node = node;
    }

    public static NodeInterpretationResult forIsolatedNode(CgmesZ0Node node) {
        NodeInterpretationResult b = new NodeInterpretationResult(node);
        b.isolated = true;
        b.calculated = false;
        b.badVoltage = false;
        return b;
    }

    public CgmesZ0Node z0node() {
        return z0node;
    }

    public boolean isCalculated() {
        return calculated;
    }

    public boolean isIsolated() {
        return isolated;
    }

    public boolean isBadVoltage() {
        return badVoltage;
    }

    public double error() {
        return Math.abs(p) + Math.abs(q);
    }

    // TODO isBad and isOk are preserved from previous expressions
    // TODO but we should have a single way of querying node has error
    public boolean isBad() {
        return calculated
            && !badVoltage
            && error() > Configuration.ERROR_TOLERANCE;
    }

    public boolean isOk() {
        return error() <= Configuration.ERROR_TOLERANCE;
    }

    public void setBadVoltage(boolean b) {
        badVoltage = b;
    }

    public void setCalculated(boolean b) {
        calculated = b;
    }

    public double p() {
        return p;
    }

    public double q() {
        return q;
    }

    public void addP(double p) {
        this.p += p;
    }

    public void addQ(double q) {
        this.q += q;
    }

    public void incLines() {
        numLines++;
    }

    public void incTransformers2() {
        numTransformers2++;
    }

    public void incTransformers3() {
        numTransformers3++;
    }

    public int numLines() {
        return numLines;
    }

    public int numTransformers2() {
        return numTransformers2;
    }

    public int numTransformers3() {
        return numTransformers3;
    }

    public Map<String, DetectedEquipmentModel> detectedModels() {
        return detectedModels;
    }

    public void addDetectedModel(DetectedEquipmentModel eqModel) {
        if (eqModel == null) {
            return;
        }
        DetectedEquipmentModel aggregateEqModel = detectedModels.computeIfAbsent(
            eqModel.code(),
            id -> new DetectedEquipmentModel(eqModel.detectedBranchModels()));
        aggregateEqModel.incTotal(1);
    }

    private final CgmesZ0Node z0node;
    private final Map<String, DetectedEquipmentModel> detectedModels = new HashMap<>();

    // TODO This is a strange default value (calculated is true by default)?
    private boolean calculated = true;
    private boolean isolated;
    private boolean badVoltage;

    private double p;
    private double q;

    private int numLines;
    private int numTransformers2;
    private int numTransformers3;
}