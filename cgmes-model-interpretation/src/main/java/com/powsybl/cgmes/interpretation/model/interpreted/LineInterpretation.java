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
public class LineInterpretation extends Interpretation {

    public LineInterpretation(
        CgmesLine line,
        CgmesNode node1, CgmesNode node2,
        InterpretationAlternative alternative) {
        super(alternative);
        this.line = line;
        this.node1 = node1;
        this.node2 = node2;
    }

    @Override
    public InterpretedBranch interpret() {
        InterpretedBranch b = new InterpretedBranch();
        b.end1.r = line.r();
        b.end1.x = line.x();
        interpretShuntAdmittance(b);
        // Add a structural ratio after detected branch model
        // Lines with nominalV1 != nominalV2 (380, 400)
        interpretStructuralRatio(b);
        Complex ysh1 = new Complex(b.end1.g, b.end1.b);
        Complex ysh2 = new Complex(b.end2.g, b.end2.b);
        b.setDetectedBranchModel(DetectedBranchModel.forLine(ysh1, ysh2));
        return b;
    }

    private void interpretStructuralRatio(InterpretedBranch b) {
        double a = 1.0;
        if (alternative.isLineRatio0()
            && node1 != null
            && node2 != null) {
            double nominalV1 = node1.nominalV();
            double nominalV2 = node2.nominalV();
            if (Math.abs(nominalV1 - nominalV2) > 0
                && nominalV1 != 0.0
                && !Double.isNaN(nominalV1)
                && !Double.isNaN(nominalV2)) {
                a = nominalV2 / nominalV1;
            }
        }
        b.end1.a = a;
    }

    private void interpretShuntAdmittance(InterpretedBranch b) {
        double bch = line.bch();
        double gch = line.gch();

        if (!Configuration.CONSIDER_GCH_FOR_LINES) {
            gch = 0;
        }

        switch (alternative.getLineBshunt()) {
            case END1:
                b.end1.g = gch;
                b.end1.b = bch;
                b.end1.g = 0;
                b.end2.b = 0;
                break;
            case END2:
                b.end1.g = 0;
                b.end1.b = 0;
                b.end1.g = gch;
                b.end2.b = bch;
                break;
            case SPLIT:
                b.end1.g = gch * 0.5;
                b.end1.b = bch * 0.5;
                b.end2.g = gch * 0.5;
                b.end2.b = bch * 0.5;
                break;
        }
    }

    private final CgmesLine line;
    private final CgmesNode node1;
    private final CgmesNode node2;
}
