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
        LineParameters lineParams = interpretLineParameters(line);
        InterpretedBranch.ShuntAdmittances ysh = interpretAsShuntAdmittances(lineParams, alternative);
        InterpretedBranch.Ratios structuralRatios = interpretAsStructuralRatio(node1, node2, alternative);
        branchModel = new DetectedBranchModel(ysh.ysh1, ysh.ysh2);
        admittanceMatrix = new BranchAdmittanceMatrix(
            lineParams.r, lineParams.x,
            structuralRatios.a1, 0.0, ysh.ysh1,
            structuralRatios.a2, 0.0, ysh.ysh2);
    }

    private InterpretedBranch.Ratios interpretAsStructuralRatio(
        CgmesNode node1, CgmesNode node2,
        InterpretationAlternative alternative) {
        InterpretedBranch.Ratios structuralRatios = new InterpretedBranch.Ratios();
        if (alternative.isLineRatio0()
            && node1 != null
            && node2 != null) {
            double nominalV1 = node1.nominalV();
            double nominalV2 = node2.nominalV();
            if (Math.abs(nominalV1 - nominalV2) > 0
                && nominalV1 != 0.0
                && !Double.isNaN(nominalV1)
                && !Double.isNaN(nominalV2)) {
                structuralRatios.a1 = nominalV1 / nominalV2;
            }
        }
        return structuralRatios;
    }

    private InterpretedBranch.ShuntAdmittances interpretAsShuntAdmittances(
        LineParameters lineParams,
        InterpretationAlternative alternative) {
        InterpretedBranch.ShuntAdmittances ysh = new InterpretedBranch.ShuntAdmittances();
        switch (alternative.getLineBshunt()) {
            case END1:
                ysh.ysh1 = new Complex(lineParams.gch, lineParams.bch);
                break;
            case END2:
                ysh.ysh2 = new Complex(lineParams.gch, lineParams.bch);
                break;
            case SPLIT:
                ysh.ysh1 = new Complex(lineParams.gch * 0.5, lineParams.bch * 0.5);
                ysh.ysh2 = new Complex(lineParams.gch * 0.5, lineParams.bch * 0.5);
                break;
        }
        return ysh;
    }

    private LineParameters interpretLineParameters(CgmesLine line) {
        LineParameters p = new LineParameters();
        p.r = line.r();
        p.x = line.x();
        p.gch = line.gch();
        p.bch = line.bch();
        if (!Configuration.CONSIDER_GCH_FOR_LINES) {
            p.gch = 0;
        }
        return p;
    }

    static class LineParameters {
        double r = 0.0;
        double x = 0.0;
        double gch = 0.0;
        double bch = 0.0;
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
