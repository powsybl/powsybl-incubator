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
        InterpretedBranch.TransformerEndParameters end1 = InterpretedBranch.getEndParameters(transformer.end1());
        InterpretedBranch.TransformerEndParameters end2 = InterpretedBranch.getEndParameters(transformer.end2());
        InterpretedBranch.TransformerEndParameters end3 = InterpretedBranch.getEndParameters(transformer.end3());
        // Interpret the tap changers AND modify, as a side effect, the transformer parameters
        InterpretedBranch.TransformerEndParameters modifiedEnd1 = new InterpretedBranch.TransformerEndParameters();
        InterpretedBranch.TapChangers tcsEnd1 = interpretTapChangerStatus(transformer.end1(), end1, alternative, modifiedEnd1);
        InterpretedBranch.TransformerEndParameters modifiedEnd2 = new InterpretedBranch.TransformerEndParameters();
        InterpretedBranch.TapChangers tcsEnd2 = interpretTapChangerStatus(transformer.end2(), end2, alternative, modifiedEnd2);
        InterpretedBranch.TransformerEndParameters modifiedEnd3 = new InterpretedBranch.TransformerEndParameters();
        InterpretedBranch.TapChangers tcsEnd3 = interpretTapChangerStatus(transformer.end3(), end3, alternative, modifiedEnd3);

        InterpretedBranch.ShuntAdmittances yshEnd1 = interpretAsShuntAdmittances(modifiedEnd1, alternative);
        InterpretedBranch.ShuntAdmittances yshEnd2 = interpretAsShuntAdmittances(modifiedEnd2, alternative);
        InterpretedBranch.ShuntAdmittances yshEnd3 = interpretAsShuntAdmittances(modifiedEnd3, alternative);

        InterpretedBranch.PhaseAngleClocks pacsEnd1 = interpretPhaseAngleClock(transformer.end1(), alternative);
        tcsEnd1.addPhaseAngleClocks(pacsEnd1);
        InterpretedBranch.PhaseAngleClocks pacsEnd2 = interpretPhaseAngleClock(transformer.end2(), alternative);
        tcsEnd2.addPhaseAngleClocks(pacsEnd2);
        InterpretedBranch.PhaseAngleClocks pacsEnd3 = interpretPhaseAngleClock(transformer.end3(), alternative);
        tcsEnd3.addPhaseAngleClocks(pacsEnd3);

        branchModelEnd1 = new DetectedBranchModel(yshEnd1.ysh1, yshEnd1.ysh2,
                tcsEnd1.rtc1, tcsEnd1.ptc1,
                tcsEnd1.rtc2, tcsEnd1.ptc2);
        branchModelEnd2 = new DetectedBranchModel(yshEnd2.ysh1, yshEnd2.ysh2,
                tcsEnd2.rtc1, tcsEnd2.ptc1,
                tcsEnd2.rtc2, tcsEnd2.ptc2);
        branchModelEnd3 = new DetectedBranchModel(yshEnd3.ysh1, yshEnd3.ysh2,
                tcsEnd3.rtc1, tcsEnd3.ptc1,
                tcsEnd3.rtc2, tcsEnd3.ptc2);

        // add structural ratio after detected branch model
        double ratedU0 = transformer.end1().ratedU();
        InterpretedBranch.Ratios structuralRatiosEnd1 = interpretStructuralRatioEnd(transformer.end1(), alternative,
                ratedU0);
        InterpretedBranch.Ratios structuralRatiosEnd2 = interpretStructuralRatioEnd(transformer.end2(), alternative,
                ratedU0);
        InterpretedBranch.Ratios structuralRatiosEnd3 = interpretStructuralRatioEnd(transformer.end3(), alternative,
                ratedU0);

        InterpretedBranch.Ratios ratiosEnd1 = InterpretedBranch.calculateRatios(tcsEnd1, structuralRatiosEnd1);
        InterpretedBranch.Ratios ratiosEnd2 = InterpretedBranch.calculateRatios(tcsEnd2, structuralRatiosEnd2);
        InterpretedBranch.Ratios ratiosEnd3 = InterpretedBranch.calculateRatios(tcsEnd3, structuralRatiosEnd3);

        admittanceMatrixEnd1 = new BranchAdmittanceMatrix(modifiedEnd1.r, modifiedEnd1.x, ratiosEnd1.a1, tcsEnd1.ptc1.angle, yshEnd1.ysh1, ratiosEnd1.a2,
                tcsEnd1.ptc2.angle, yshEnd1.ysh2);
        admittanceMatrixEnd2 = new BranchAdmittanceMatrix(modifiedEnd2.r, modifiedEnd2.x, ratiosEnd2.a1, tcsEnd2.ptc1.angle, yshEnd2.ysh1, ratiosEnd2.a2,
                tcsEnd2.ptc2.angle, yshEnd2.ysh2);
        admittanceMatrixEnd3 = new BranchAdmittanceMatrix(modifiedEnd3.r, modifiedEnd3.x, ratiosEnd3.a1, tcsEnd3.ptc1.angle, yshEnd3.ysh1, ratiosEnd3.a2,
                tcsEnd3.ptc2.angle, yshEnd3.ysh2);
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
        if (alternative.getXfmr3PhaseAngleClock() == Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE) {
            pacs.angle2 = transformerEnd.phaseAngleClockDegrees();
        } else if (alternative.getXfmr3PhaseAngleClock() == Xfmr3PhaseAngleClockAlternative.NETWORK_SIDE) {
            pacs.angle1 = transformerEnd.phaseAngleClockDegrees();
        }
        return pacs;
    }

    private InterpretedBranch.Ratios interpretStructuralRatioEnd(CgmesTransformerEnd transformerEnd,
            InterpretationAlternative alternative, double ratedU0) {
        double ratedU = transformerEnd.ratedU();
        InterpretedBranch.Ratios structuralRatioData = new InterpretedBranch.Ratios();
        if (alternative.getXfmr3Ratio0StarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.STAR_BUS_SIDE) {
            structuralRatioData.a1 = 1.0;
            structuralRatioData.a2 = ratedU0 / ratedU;
        } else if (alternative.getXfmr3Ratio0StarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE) {
            structuralRatioData.a1 = ratedU / ratedU0;
            structuralRatioData.a2 = 1.0;
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
