/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTapChangerStatus;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformer;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformerEnd;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr3PhaseAngleClockAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr3RatioPhaseInterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr3ShuntInterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretedBranch.TransformerEndParameters;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretedTransformer3 {

    public InterpretedTransformer3(CgmesTransformer transformer, InterpretationAlternative alternative) {

        double ratedU0 = getRatedU0(transformer, alternative);
        End end1 = interpretEnd(transformer.end1(), ratedU0, alternative);
        branchModelEnd1 = end1.branchModelEnd;
        admittanceMatrixEnd1 = end1.admittanceMatrixEnd;

        End end2 = interpretEnd(transformer.end2(), ratedU0, alternative);
        branchModelEnd2 = end2.branchModelEnd;
        admittanceMatrixEnd2 = end2.admittanceMatrixEnd;

        End end3 = interpretEnd(transformer.end3(), ratedU0, alternative);
        branchModelEnd3 = end3.branchModelEnd;
        admittanceMatrixEnd3 = end3.admittanceMatrixEnd;
    }

    private End interpretEnd(CgmesTransformerEnd transformerEnd, double ratedU0, InterpretationAlternative alternative) {
        InterpretedBranch.TransformerEndParameters end = InterpretedBranch.getEndParameters(transformerEnd);
        // Interpret the tap changers AND modify, as a side effect, the transformer parameters
        InterpretedBranch.TransformerEndParameters modifiedEnd = new InterpretedBranch.TransformerEndParameters();
        InterpretedBranch.TapChangers tcsEnd = interpretTapChangerStatus(transformerEnd, end, alternative, modifiedEnd);
        InterpretedBranch.ShuntAdmittances yshEnd = interpretAsShuntAdmittances(modifiedEnd, alternative);
        InterpretedBranch.PhaseAngleClocks pacsEnd = interpretPhaseAngleClock(transformerEnd, alternative);
        tcsEnd.addPhaseAngleClocks(pacsEnd);

        End e = new End();
        e.branchModelEnd = new DetectedBranchModel(yshEnd.ysh1, yshEnd.ysh2, tcsEnd.rtc1, tcsEnd.ptc1, tcsEnd.rtc2, tcsEnd.ptc2);

        InterpretedBranch.Ratios structuralRatiosEnd = interpretStructuralRatioEnd(transformerEnd, ratedU0, alternative);
        InterpretedBranch.Ratios ratiosEnd = InterpretedBranch.calculateRatios(tcsEnd, structuralRatiosEnd);

        e.admittanceMatrixEnd = new BranchAdmittanceMatrix(modifiedEnd.r, modifiedEnd.x, ratiosEnd.a1, tcsEnd.ptc1.angle, yshEnd.ysh1, ratiosEnd.a2,
                tcsEnd.ptc2.angle, yshEnd.ysh2);

        return e;
    }

    private InterpretedBranch.TapChangers interpretTapChangerStatus(CgmesTransformerEnd transformerEnd,
            InterpretedBranch.TransformerEndParameters end, InterpretationAlternative alternative,
            InterpretedBranch.TransformerEndParameters modifiedEnd) {

        InterpretedBranch.TapChangers tcs = new InterpretedBranch.TapChangers();

        CgmesTapChangerStatus rtcStatus = transformerEnd.rtc().status();
        InterpretedBranch.CorrectionFactors rtcEndCorrectionFactors = InterpretedBranch.correctionFactors(rtcStatus);

        CgmesTapChangerStatus ptcStatus = transformerEnd.ptc().status();
        double newX = transformerEnd.ptc().overrideX(end.x, ptcStatus.angle());
        InterpretedBranch.CorrectionFactors ptcEndCorrectionFactors = InterpretedBranch.correctionFactors(ptcStatus);

        applyCorrectionFactors(end, rtcEndCorrectionFactors, ptcEndCorrectionFactors, newX, modifiedEnd);

        // network side always at end1
        if (alternative.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.STAR_BUS_SIDE) {

            tcs.rtc2.ratio = rtcStatus.ratio();
            tcs.rtc2.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            tcs.rtc2.changeable = transformerEnd.rtc().hasDifferentRatios();
            tcs.ptc2.ratio = ptcStatus.ratio();
            tcs.ptc2.angle = ptcStatus.angle();
            tcs.ptc2.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            tcs.ptc2.changeable = transformerEnd.ptc().hasDifferentRatiosAngles();

        } else if (alternative.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE) {

            tcs.rtc1.ratio = rtcStatus.ratio();
            tcs.rtc1.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            tcs.rtc1.changeable = transformerEnd.rtc().hasDifferentRatios();
            tcs.ptc1.ratio = ptcStatus.ratio();
            tcs.ptc1.angle = ptcStatus.angle();
            tcs.ptc1.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            tcs.ptc1.changeable = transformerEnd.ptc().hasDifferentRatiosAngles();
        }

        return tcs;
    }

    private InterpretedBranch.ShuntAdmittances interpretAsShuntAdmittances(
            InterpretedBranch.TransformerEndParameters endParameters,
            InterpretationAlternative alternative) {
        Xfmr3ShuntInterpretationAlternative xfmr3YShunt = alternative.getXfmr3YShunt();
        InterpretedBranch.ShuntAdmittances ysh = new InterpretedBranch.ShuntAdmittances();
        switch (xfmr3YShunt) {
            case NETWORK_SIDE:
                ysh.ysh1 = new Complex(endParameters.g, endParameters.b);
                break;
            case STAR_BUS_SIDE:
                ysh.ysh2 = new Complex(endParameters.g, endParameters.b);
                break;
            case SPLIT:
                ysh.ysh1 = new Complex(endParameters.g * 0.5, endParameters.b * 0.5);
                ysh.ysh2 = new Complex(endParameters.g * 0.5, endParameters.b * 0.5);
                break;
        }
        return ysh;
    }

    private InterpretedBranch.PhaseAngleClocks interpretPhaseAngleClock(CgmesTransformerEnd transformerEnd,
            InterpretationAlternative alternative) {

        InterpretedBranch.PhaseAngleClocks pacs = new InterpretedBranch.PhaseAngleClocks();
        if (alternative.getXfmr3PhaseAngleClock() == Xfmr3PhaseAngleClockAlternative.ON) {
            pacs.angle1 += -transformerEnd.phaseAngleClockDegrees();
        }
        return pacs;
    }

    private double getRatedU0(CgmesTransformer transformer, InterpretationAlternative alternative) {
        double ratedU0;
        switch (alternative.getXfmr3Ratio0Side()) {
            case END1:
                ratedU0 = transformer.end1().ratedU();
                break;
            case END2:
                ratedU0 = transformer.end2().ratedU();
                break;
            case END3:
                ratedU0 = transformer.end3().ratedU();
                break;
            default:
                ratedU0 = 1.0;
                break;
        }
        return ratedU0;
    }

    private InterpretedBranch.Ratios interpretStructuralRatioEnd(CgmesTransformerEnd transformerEnd,
        double ratedU0, InterpretationAlternative alternative) {
        double ratedU = transformerEnd.ratedU();
        InterpretedBranch.Ratios structuralRatioData = new InterpretedBranch.Ratios();
        switch (alternative.getXfmr3Ratio0Side()) {
            case STAR_BUS_SIDE:
                structuralRatioData.a1 = 1.0;
                structuralRatioData.a2 = ratedU0 / ratedU;
                break;
            default:
                structuralRatioData.a1 = ratedU / ratedU0;
                structuralRatioData.a2 = 1.0;
                break;
        }
        return structuralRatioData;
    }

    // TODO warnings under incoherent cases
    private void applyCorrectionFactors(
            InterpretedBranch.TransformerEndParameters initialEndParameters,
            InterpretedBranch.CorrectionFactors rtcEndParameterCorrections,
            InterpretedBranch.CorrectionFactors ptcEndParameterCorrections,
            double newX, TransformerEndParameters modifiedEnd) {

        modifiedEnd.r = initialEndParameters.r * rtcEndParameterCorrections.r * ptcEndParameterCorrections.r;
        modifiedEnd.g = initialEndParameters.g * rtcEndParameterCorrections.g * ptcEndParameterCorrections.g;
        modifiedEnd.b = initialEndParameters.b * rtcEndParameterCorrections.b * ptcEndParameterCorrections.b;

        modifiedEnd.x = newX * rtcEndParameterCorrections.x * ptcEndParameterCorrections.x;
    }

    static class End {
        BranchAdmittanceMatrix admittanceMatrixEnd;
        DetectedBranchModel branchModelEnd;
    }

    public DetectedBranchModel getBranchModelEnd1() {
        return branchModelEnd1;
    }

    public DetectedBranchModel getBranchModelEnd2() {
        return branchModelEnd2;
    }

    public DetectedBranchModel getBranchModelEnd3() {
        return branchModelEnd3;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrixEnd1() {
        return admittanceMatrixEnd1;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrixEnd2() {
        return admittanceMatrixEnd2;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrixEnd3() {
        return admittanceMatrixEnd3;
    }

    private final BranchAdmittanceMatrix admittanceMatrixEnd1;
    private final BranchAdmittanceMatrix admittanceMatrixEnd2;
    private final BranchAdmittanceMatrix admittanceMatrixEnd3;
    private final DetectedBranchModel branchModelEnd1;
    private final DetectedBranchModel branchModelEnd2;
    private final DetectedBranchModel branchModelEnd3;
}
