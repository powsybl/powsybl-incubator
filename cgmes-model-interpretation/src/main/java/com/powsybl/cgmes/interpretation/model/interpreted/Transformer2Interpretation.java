/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.interpretation.model.cgmes.CgmesPhaseTapChanger;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesRatioTapChanger;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTapChanger;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTapChangerStatus;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformer;
import com.powsybl.cgmes.interpretation.model.interpreted.BranchInterpretedTapChangers.InterpretedPhaseTapChanger;
import com.powsybl.cgmes.interpretation.model.interpreted.BranchInterpretedTapChangers.InterpretedRatioTapChanger;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr2PhaseAngleClockAlternative;
import com.powsybl.commons.PowsyblException;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class Transformer2Interpretation {

    public Transformer2Interpretation(CgmesTransformer transformer, InterpretationAlternative alternative) {
        this.transformer = transformer;
        this.alternative = alternative;
    }

    public InterpretedBranch interpret() {
        // TODO Instead of accumulating changes on the interpreted branch
        // follow the same schema that is implemented for Transformer3:
        // have a set of interpret... functions that return a small object
        // and, at the end, combine all these small objects into the interpreted branch
        InterpretedBranch branch = new InterpretedBranch();

        // Series impedance is not interpreted
        branch.end1.r = transformer.end1().r();
        branch.end1.x = transformer.end1().x();
        branch.end2.r = transformer.end2().r();
        branch.end2.x = transformer.end2().x();

        interpretShuntAdmittance(branch);

        // After the shunt admittance has been interpreted,
        // apply corrections based on current tap changer step
        applyCorrections(branch);

        BranchInterpretedTapChangers tcs = interpretTapChangers();
        // Combine the ratios from the ratio tap changer and the phase tap changer
        // And copy adjustments of ratio/angle to the interpreted branch
        branch.end1.a = tcs.rtc1.ratio * tcs.ptc1.ratio;
        branch.end1.angle = tcs.ptc1.angle;
        branch.end2.a = tcs.rtc2.ratio * tcs.ptc2.ratio;
        branch.end2.angle = tcs.ptc2.angle;

        interpretPhaseAngleClock(branch);
        // We are updating the detected tap changers with the value of angle
        // AFTER we have interpreted the phase angle clock
        tcs.ptc1.angle = branch.end1.angle;
        tcs.ptc2.angle = branch.end2.angle;

        branch.setDetectedBranchModel(new DetectedBranchModel(
            new Complex(branch.end1.g, branch.end1.b),
            new Complex(branch.end2.g, branch.end2.b),
            tcs));

        // Add the structural ratio to the interpreted branch
        // AFTER detecting the branch model:
        // the structural ratio is considered at the proper side
        // depending on the alternative, but we do not want it to
        // interfere in the information about the detected model
        interpretStructuralRatio(branch);

        return branch;
    }

    private void applyCorrections(InterpretedBranch branch) {
        CgmesTapChangerStatus rtap1 = transformer.end1().rtc().status();
        CgmesTapChangerStatus ptap1 = transformer.end1().ptc().status();
        CgmesTapChangerStatus rtap2 = transformer.end2().rtc().status();
        CgmesTapChangerStatus ptap2 = transformer.end2().ptc().status();

        // TODO Review the sequence here:
        // We apply a correction to x from current tap step
        // But after applying the correction we are recalculating the x
        // Should we first compute x, then apply corrections?
        applyCorrectionsEnd1(rtap1, branch);
        branch.end1.x = transformer.end1().ptc().overrideX(branch.end1.x, ptap1.angle());
        applyCorrectionsEnd1(ptap1, branch);
        applyCorrectionsEnd2(rtap2, branch);
        double ptap2angle = interpretPhaseTapChangerAngleEnd2(alternative, ptap2.angle());
        branch.end2.x = transformer.end2().ptc().overrideX(branch.end2.x, ptap2angle);
        applyCorrectionsEnd2(ptap2, branch);
    }

    public void interpretShuntAdmittance(InterpretedBranch branch) {
        double g1 = transformer.end1().g();
        double b1 = transformer.end1().b();
        double g2 = transformer.end2().g();
        double b2 = transformer.end2().b();
        switch (alternative.getXfmr2YShunt()) {
            case END1:
                branch.end1.g = g1 + g2;
                branch.end1.b = b1 + b2;
                branch.end2.g = 0;
                branch.end2.b = 0;
                break;
            case END2:
                branch.end1.g = 0;
                branch.end1.b = 0;
                branch.end2.g = g1 + g2;
                branch.end2.b = b1 + b2;
                break;
            case END1_END2:
                branch.end1.g = g1;
                branch.end1.b = b1;
                branch.end2.g = g2;
                branch.end2.b = b2;
                break;
            case SPLIT:
                branch.end1.g = (g1 + g2) * 0.5;
                branch.end1.b = (b1 + b2) * 0.5;
                branch.end2.g = (g1 + g2) * 0.5;
                branch.end2.b = (b1 + b2) * 0.5;
                break;
        }
    }

    private BranchInterpretedTapChangers interpretTapChangers() {
        BranchInterpretedTapChangers i = new BranchInterpretedTapChangers();
        switch (alternative.getXfmr2RatioPhase()) {
            case END1:
                interpretBothIncomingTapChangersAt(i.rtc1, i.ptc1);
                break;
            case END2:
                interpretBothIncomingTapChangersAt(i.rtc2, i.ptc2);
                break;
            case END1_END2:
                interpretIncomingTapChangersAtCorrespondingEnd(i);
                break;
            case X:
                if (transformer.end1().x() == 0.0) {
                    interpretBothIncomingTapChangersAt(i.rtc1, i.ptc1);
                } else {
                    interpretBothIncomingTapChangersAt(i.rtc2, i.ptc2);
                }
                break;
            default:
                throw new PowsyblException("Unsupported " + alternative.getXfmr2RatioPhase());
        }
        return i;
    }

    public void interpretStructuralRatio(InterpretedBranch branch) {
        int rtcEnd = transformer.end1().rtc().hasStepVoltageIncrement() ? 1 : 2;
        double ratedU1 = transformer.end1().ratedU();
        double ratedU2 = transformer.end2().ratedU();
        switch (alternative.getXfmr2Ratio0()) {
            case END1:
                branch.end1.a *= ratedU1 / ratedU2;
                branch.end2.a *= 1.0;
                break;
            case END2:
                branch.end1.a *= 1.0;
                branch.end2.a *= ratedU2 / ratedU1;
                break;
            case RTC:
                if (rtcEnd == 1) {
                    branch.end1.a *= ratedU1 / ratedU2;
                    branch.end2.a *= 1.0;
                } else {
                    branch.end1.a *= 1.0;
                    branch.end2.a *= ratedU2 / ratedU1;
                }
                break;
            case X:
                if (transformer.end1().x() == 0.0) {
                    // Structural ratio in the side that has x == 0
                    branch.end1.a *= ratedU1 / ratedU2;
                    branch.end2.a *= 1.0;
                } else {
                    branch.end1.a *= 1.0;
                    branch.end2.a *= ratedU2 / ratedU1;
                }
                break;
            default:
                throw new PowsyblException("Unsupported alternative " + alternative.getXfmr2Ratio0());
        }
    }

    public void interpretPhaseAngleClock(InterpretedBranch branch) {
        if (alternative.getXfmr2PhaseAngleClock() == Xfmr2PhaseAngleClockAlternative.END1_END2) {
            branch.end1.angle += transformer.end1().phaseAngleClockDegrees();

            double angle2 = transformer.end2().phaseAngleClockDegrees();
            if (alternative.isXfmr2Pac2Negate()) {
                angle2 = -angle2;
            }
            branch.end2.angle += angle2;
        }
    }

    private void interpretBothIncomingTapChangersAt(InterpretedRatioTapChanger irtc, InterpretedPhaseTapChanger iptc) {
        CgmesRatioTapChanger rtc1 = transformer.end1().rtc();
        CgmesPhaseTapChanger ptc1 = transformer.end1().ptc();
        CgmesRatioTapChanger rtc2 = transformer.end2().rtc();
        CgmesPhaseTapChanger ptc2 = transformer.end2().ptc();
        double ptc2angle = interpretPhaseTapChangerAngleEnd2(alternative, ptc2.status().angle());

        irtc.ratio = rtc1.status().ratio() * rtc2.status().ratio();
        irtc.regulatingControl = rtc1.isRegulatingControlEnabled() || rtc2.isRegulatingControlEnabled();
        irtc.changeable = rtc1.hasDifferentRatios() || rtc2.hasDifferentRatios();
        iptc.ratio = ptc1.status().ratio() * ptc2.status().ratio();
        iptc.angle = ptc1.status().angle() + ptc2angle;
        iptc.regulatingControl = ptc1.isRegulatingControlEnabled() || ptc2.isRegulatingControlEnabled();
        iptc.changeable = ptc1.hasDifferentRatiosAngles() || ptc2.hasDifferentRatiosAngles();
    }

    private void interpretIncomingTapChangersAtCorrespondingEnd(BranchInterpretedTapChangers i) {
        CgmesRatioTapChanger rtc1 = transformer.end1().rtc();
        CgmesPhaseTapChanger ptc1 = transformer.end1().ptc();
        CgmesRatioTapChanger rtc2 = transformer.end2().rtc();
        CgmesPhaseTapChanger ptc2 = transformer.end2().ptc();
        double ptc2angle = interpretPhaseTapChangerAngleEnd2(alternative, ptc2.status().angle());

        i.rtc1.ratio = rtc1.status().ratio();
        i.rtc1.regulatingControl = rtc1.isRegulatingControlEnabled();
        i.rtc1.changeable = rtc1.hasDifferentRatios();
        i.ptc1.ratio = ptc1.status().ratio();
        i.ptc1.angle = ptc1.status().angle();
        i.ptc1.regulatingControl = ptc1.isRegulatingControlEnabled();
        i.ptc1.changeable = ptc1.hasDifferentRatiosAngles();

        i.rtc2.ratio = rtc2.status().ratio();
        i.rtc2.regulatingControl = rtc2.isRegulatingControlEnabled();
        i.rtc2.changeable = rtc2.hasDifferentRatios();
        i.ptc2.ratio = ptc2.status().ratio();
        i.ptc2.angle = ptc2angle;
        i.ptc2.regulatingControl = ptc2.isRegulatingControlEnabled();
        i.ptc2.changeable = ptc2.hasDifferentRatiosAngles();
    }

    private double interpretPhaseTapChangerAngleEnd2(InterpretationAlternative config, double angle) {
        double outAngle = angle;
        if (config.isXfmr2Ptc2Negate()) {
            outAngle = -angle;
        }
        return outAngle;
    }

    private void applyCorrectionsEnd1(CgmesTapChangerStatus tap, InterpretedBranch branch) {
        double xc = tap.xc();
        double rc = tap.rc();
        double bc = tap.bc();
        double gc = tap.gc();

        // TODO Question Why if the x1 is zero we apply the correction to x2 ???
        if (branch.end1.x != 0.0) {
            branch.end1.x = CgmesTapChanger.applyCorrection(branch.end1.x, xc);
        } else {
            branch.end2.x = CgmesTapChanger.applyCorrection(branch.end2.x, xc);
        }
        if (branch.end1.r != 0.0) {
            branch.end1.r = CgmesTapChanger.applyCorrection(branch.end1.r, rc);
        } else {
            branch.end2.r = CgmesTapChanger.applyCorrection(branch.end2.r, rc);
        }
        if (branch.end1.b != 0.0) {
            branch.end1.b = CgmesTapChanger.applyCorrection(branch.end1.b, bc);
        } else {
            branch.end2.b = CgmesTapChanger.applyCorrection(branch.end2.b, bc);
        }
        if (branch.end1.g != 0.0) {
            branch.end1.g = CgmesTapChanger.applyCorrection(branch.end1.g, gc);
        } else {
            branch.end2.g = CgmesTapChanger.applyCorrection(branch.end2.g, gc);
        }
    }

    private void applyCorrectionsEnd2(CgmesTapChangerStatus tapChangerData, InterpretedBranch branch) {
        double xc = tapChangerData.xc();
        double rc = tapChangerData.rc();
        double bc = tapChangerData.bc();
        double gc = tapChangerData.gc();

        if (branch.end2.x != 0.0) {
            branch.end2.x = CgmesTapChanger.applyCorrection(branch.end2.x, xc);
        } else {
            branch.end1.x = CgmesTapChanger.applyCorrection(branch.end1.x, xc);
        }
        if (branch.end2.r != 0.0) {
            branch.end2.r = CgmesTapChanger.applyCorrection(branch.end2.r, rc);
        } else {
            branch.end1.r = CgmesTapChanger.applyCorrection(branch.end1.r, rc);
        }
        if (branch.end2.b != 0.0) {
            branch.end2.b = CgmesTapChanger.applyCorrection(branch.end2.b, bc);
        } else {
            branch.end1.b = CgmesTapChanger.applyCorrection(branch.end1.b, bc);
        }
        if (branch.end2.g != 0.0) {
            branch.end2.g = CgmesTapChanger.applyCorrection(branch.end2.g, gc);
        } else {
            branch.end1.g = CgmesTapChanger.applyCorrection(branch.end1.g, gc);
        }
    }

    private final CgmesTransformer transformer;
    private final InterpretationAlternative alternative;
}
