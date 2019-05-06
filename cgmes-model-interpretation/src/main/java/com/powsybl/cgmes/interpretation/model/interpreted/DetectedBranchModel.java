/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.interpretation.model.interpreted.BranchInterpretedTapChangers.InterpretedPhaseTapChanger;
import com.powsybl.cgmes.interpretation.model.interpreted.BranchInterpretedTapChangers.InterpretedRatioTapChanger;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class DetectedBranchModel {

    public DetectedBranchModel(Complex ysh1, Complex ysh2, InterpretedRatioTapChanger ratio1,
        InterpretedPhaseTapChanger phase1,
        InterpretedRatioTapChanger ratio2, InterpretedPhaseTapChanger phase2) {
        this.ratio1 = xfmrRatioModel(ratio1);
        this.phase1 = xfmrPhaseModel(phase1);
        this.shunt1 = isShuntModel(ysh1);
        this.shunt2 = isShuntModel(ysh2);
        this.ratio2 = xfmrRatioModel(ratio2);
        this.phase2 = xfmrPhaseModel(phase2);
    }

    public DetectedBranchModel(Complex ysh1, Complex ysh2) {
        this.shunt1 = isShuntModel(ysh1);
        this.shunt2 = isShuntModel(ysh2);
        this.ratio1 = null;
        this.phase1 = null;
        this.ratio2 = null;
        this.phase2 = null;
    }

    enum DetectedTapChanger {
        ABSENT, FIXED, CHANGEABLE_AT_NEUTRAL, CHANGEABLE_AT_NON_NEUTRAL, REGULATING_CONTROL
    }

    public String code() {
        StringBuilder code = new StringBuilder();
        if (ratio1 != null) {
            code.append(ratioCode(ratio1));
        }
        if (phase1 != null) {
            code.append(phaseCode(phase1));
        }
        code.append(shuntCode(shunt1));
        code.append(shuntCode(shunt2));
        if (ratio2 != null) {
            code.append(ratioCode(ratio2));
        }
        if (phase2 != null) {
            code.append(phaseCode(phase2));
        }
        return code.toString();
    }

    private DetectedTapChanger xfmrRatioModel(InterpretedRatioTapChanger ratio) {
        if (ratio.regulatingControl && ratio.changeable) {
            return DetectedTapChanger.REGULATING_CONTROL;
        }
        if (ratio.ratio == 1.0) {
            if (ratio.changeable) {
                return DetectedTapChanger.CHANGEABLE_AT_NEUTRAL;
            } else {
                return DetectedTapChanger.ABSENT;
            }
        } else {
            if (ratio.changeable) {
                return DetectedTapChanger.CHANGEABLE_AT_NON_NEUTRAL;
            } else {
                return DetectedTapChanger.FIXED;
            }
        }
    }

    private DetectedTapChanger xfmrPhaseModel(InterpretedPhaseTapChanger phase) {
        if (phase.regulatingControl && phase.changeable) {
            return DetectedTapChanger.REGULATING_CONTROL;
        }
        if (phase.ratio == 1.0 && phase.angle == 0.0) {
            if (phase.changeable) {
                return DetectedTapChanger.CHANGEABLE_AT_NEUTRAL;
            } else {
                return DetectedTapChanger.ABSENT;
            }
        } else {
            if (phase.changeable) {
                return DetectedTapChanger.CHANGEABLE_AT_NON_NEUTRAL;
            } else {
                return DetectedTapChanger.FIXED;
            }
        }
    }

    private static boolean isShuntModel(Complex ysh) {
        return !ysh.equals(Complex.ZERO);
    }

    private String shuntCode(boolean shunt) {
        StringBuilder code = new StringBuilder();
        code.append(shunt ? "Y" : "N");
        return code.toString();
    }

    private String ratioCode(DetectedTapChanger ratio) {
        StringBuilder code = new StringBuilder();
        switch (ratio) {
            case ABSENT:
                code.append("_");
                break;
            case FIXED:
                code.append("x");
                break;
            case CHANGEABLE_AT_NEUTRAL:
                code.append("n");
                break;
            case CHANGEABLE_AT_NON_NEUTRAL:
                code.append("r");
                break;
            case REGULATING_CONTROL:
                code.append("R");
                break;
        }
        return code.toString();
    }

    private String phaseCode(DetectedTapChanger phase) {
        StringBuilder code = new StringBuilder();
        switch (phase) {
            case ABSENT:
                code.append("_");
                break;
            case FIXED:
                code.append("x");
                break;
            case CHANGEABLE_AT_NEUTRAL:
                code.append("m");
                break;
            case CHANGEABLE_AT_NON_NEUTRAL:
                code.append("p");
                break;
            case REGULATING_CONTROL:
                code.append("P");
                break;
        }
        return code.toString();
    }

    final DetectedTapChanger ratio1;
    final DetectedTapChanger phase1;
    final boolean shunt1;
    final boolean shunt2;
    final DetectedTapChanger ratio2;
    final DetectedTapChanger phase2;
}
