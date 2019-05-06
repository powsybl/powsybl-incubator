/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.interpretation.Configuration;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesLine;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesNode;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretedLine {

    public InterpretedLine(CgmesLine line, CgmesNode node1, CgmesNode node2, InterpretationAlternative alternative) {
        InitialParameters initialParameters = getInitialParameters(line);
        YShuntData yshuntData = interpretYShunt(initialParameters, alternative);
        RatioData structuralRatioData = interpretStructuralRatio(node1, node2, alternative);

        branchModel = new DetectedBranchModel(yshuntData.ysh1, yshuntData.ysh2);
        admittanceMatrix = new BranchAdmittanceMatrix(initialParameters.r, initialParameters.x,
            structuralRatioData.a1, 0.0, yshuntData.ysh1, structuralRatioData.a2, 0.0, yshuntData.ysh2);
    }

    private RatioData interpretStructuralRatio(CgmesNode node1, CgmesNode node2,
        InterpretationAlternative alternative) {
        RatioData structuralRatioData = new RatioData();

        if (alternative.isLineRatio0()
            && node1 != null
            && node2 != null) {
            double nominalV1 = node1.nominalV();
            double nominalV2 = node2.nominalV();
            if (Math.abs(nominalV1 - nominalV2) > 0
                && nominalV1 != 0.0
                && !Double.isNaN(nominalV1)
                && !Double.isNaN(nominalV2)) {
                structuralRatioData.a1 = nominalV1 / nominalV2;
            }
        }

        return structuralRatioData;
    }

    private YShuntData interpretYShunt(InitialParameters initialParameters, InterpretationAlternative alternative) {
        YShuntData yShuntData = new YShuntData();

        switch (alternative.getLineBshunt()) {
            case END1:
                yShuntData.ysh1 = new Complex(initialParameters.gch, initialParameters.bch);
                break;
            case END2:
                yShuntData.ysh2 = new Complex(initialParameters.gch, initialParameters.bch);
                break;
            case SPLIT:
                yShuntData.ysh1 = new Complex(initialParameters.gch * 0.5, initialParameters.bch * 0.5);
                yShuntData.ysh2 = new Complex(initialParameters.gch * 0.5, initialParameters.bch * 0.5);
                break;
        }

        return yShuntData;
    }

    private InitialParameters getInitialParameters(CgmesLine line) {
        InitialParameters initialParameters = new InitialParameters();

        initialParameters.r = line.r();
        initialParameters.x = line.x();
        initialParameters.gch = line.gch();
        initialParameters.bch = line.bch();

        if (!Configuration.CONSIDER_GCH_FOR_LINES) {
            initialParameters.gch = 0;
        }

        return initialParameters;
    }

    static class InitialParameters {
        double r = 0.0;
        double x = 0.0;
        double gch = 0.0;
        double bch = 0.0;
    }

    static class YShuntData {
        Complex ysh1 = Complex.ZERO;
        Complex ysh2 = Complex.ZERO;
    }

    static class RatioData {
        double a1 = 1.0;
        double a2 = 1.0;
    }

    public DetectedBranchModel getBranchModel() {
        return branchModel;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrix() {
        return admittanceMatrix;
    }

    private final BranchAdmittanceMatrix admittanceMatrix;
    private final DetectedBranchModel branchModel;
}
