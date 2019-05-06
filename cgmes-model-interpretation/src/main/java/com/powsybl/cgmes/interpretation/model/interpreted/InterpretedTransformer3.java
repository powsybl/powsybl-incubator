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

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretedTransformer3 {

    public InterpretedTransformer3(CgmesTransformer transformer, InterpretationAlternative alternative) {
        InterpretedBranch.TransformerEndParameters end1Parameters = InterpretedBranch
            .getEndParameters(transformer.end1());
        InterpretedBranch.TransformerEndParameters end2Parameters = InterpretedBranch
            .getEndParameters(transformer.end2());
        InterpretedBranch.TransformerEndParameters end3Parameters = InterpretedBranch
            .getEndParameters(transformer.end3());

        XXX end1RatioPhaseData = interpretRatioPhaseEnd(transformer.end1(), end1Parameters, alternative);
        XXX end2RatioPhaseData = interpretRatioPhaseEnd(transformer.end2(), end2Parameters, alternative);
        XXX end3RatioPhaseData = interpretRatioPhaseEnd(transformer.end3(), end3Parameters, alternative);

        InterpretedBranch.ShuntAdmittances end1YShuntData = interpretAsShuntAdmittances(end1RatioPhaseData.parameters,
            alternative);
        InterpretedBranch.ShuntAdmittances end2YShuntData = interpretAsShuntAdmittances(end2RatioPhaseData.parameters,
            alternative);
        InterpretedBranch.ShuntAdmittances end3YShuntData = interpretAsShuntAdmittances(end3RatioPhaseData.parameters,
            alternative);

        InterpretedBranch.PhaseAngleClocks end1PhaseAngleClockData = interpretPhaseAngleClockEnd(transformer.end1(),
            alternative);
        InterpretedBranch.PhaseAngleClocks end2PhaseAngleClockData = interpretPhaseAngleClockEnd(transformer.end2(),
            alternative);
        InterpretedBranch.PhaseAngleClocks end3PhaseAngleClockData = interpretPhaseAngleClockEnd(transformer.end3(),
            alternative);

        end1RatioPhaseData.addPhase(end1PhaseAngleClockData);
        end2RatioPhaseData.addPhase(end2PhaseAngleClockData);
        end3RatioPhaseData.addPhase(end3PhaseAngleClockData);

        branchModelEnd1 = new DetectedBranchModel(end1YShuntData.ysh1, end1YShuntData.ysh2,
            end1RatioPhaseData.tapChanger.rtc1, end1RatioPhaseData.tapChanger.ptc1,
            end1RatioPhaseData.tapChanger.rtc2, end1RatioPhaseData.tapChanger.ptc2);
        branchModelEnd2 = new DetectedBranchModel(end2YShuntData.ysh1, end2YShuntData.ysh2,
            end2RatioPhaseData.tapChanger.rtc1, end2RatioPhaseData.tapChanger.ptc1,
            end2RatioPhaseData.tapChanger.rtc2, end2RatioPhaseData.tapChanger.ptc2);
        branchModelEnd3 = new DetectedBranchModel(end3YShuntData.ysh1, end3YShuntData.ysh2,
            end3RatioPhaseData.tapChanger.rtc1, end3RatioPhaseData.tapChanger.ptc1,
            end3RatioPhaseData.tapChanger.rtc2, end3RatioPhaseData.tapChanger.ptc2);

        // add structural ratio after detected branch model
        double ratedU0 = transformer.end1().ratedU();
        InterpretedBranch.Ratios end1StructuralRatioData = interpretStructuralRatioEnd(transformer.end1(), alternative,
            ratedU0);
        InterpretedBranch.Ratios end2StructuralRatioData = interpretStructuralRatioEnd(transformer.end2(), alternative,
            ratedU0);
        InterpretedBranch.Ratios end3StructuralRatioData = interpretStructuralRatioEnd(transformer.end3(), alternative,
            ratedU0);

        InterpretedBranch.Ratios end1RatioData = calculateRatioEnd(end1RatioPhaseData, end1StructuralRatioData);
        InterpretedBranch.Ratios end2RatioData = calculateRatioEnd(end2RatioPhaseData, end2StructuralRatioData);
        InterpretedBranch.Ratios end3RatioData = calculateRatioEnd(end3RatioPhaseData, end3StructuralRatioData);

        // admittance
        admittanceMatrixEnd1 = new BranchAdmittanceMatrix(end1RatioPhaseData.parameters.r,
            end1RatioPhaseData.parameters.x, end1RatioData.a1, end1RatioPhaseData.tapChanger.ptc1.angle,
            end1YShuntData.ysh1, end1RatioData.a2, end1RatioPhaseData.tapChanger.ptc2.angle, end1YShuntData.ysh2);
        admittanceMatrixEnd2 = new BranchAdmittanceMatrix(end2RatioPhaseData.parameters.r,
            end2RatioPhaseData.parameters.x, end2RatioData.a1, end2RatioPhaseData.tapChanger.ptc1.angle,
            end2YShuntData.ysh1, end2RatioData.a2, end2RatioPhaseData.tapChanger.ptc2.angle, end2YShuntData.ysh2);
        admittanceMatrixEnd3 = new BranchAdmittanceMatrix(end3RatioPhaseData.parameters.r,
            end3RatioPhaseData.parameters.x, end3RatioData.a1, end3RatioPhaseData.tapChanger.ptc1.angle,
            end3YShuntData.ysh1, end3RatioData.a2, end3RatioPhaseData.tapChanger.ptc2.angle, end3YShuntData.ysh2);
    }

    private XXX interpretRatioPhaseEnd(CgmesTransformerEnd transformerEnd,
        InterpretedBranch.TransformerEndParameters endParameters, InterpretationAlternative alternative) {

        XXX ratioPhaseData = new XXX();

        CgmesTapChangerStatus rtcStatus = transformerEnd.rtc().status();
        InterpretedBranch.CorrectionFactors rtcEndCorrectionFactors = InterpretedBranch.correctionFactors(rtcStatus);

        CgmesTapChangerStatus ptcStatus = transformerEnd.ptc().status();
        double newX = transformerEnd.ptc().overrideX(endParameters.x, ptcStatus.angle());
        InterpretedBranch.CorrectionFactors ptcEndCorrectionFactors = InterpretedBranch.correctionFactors(ptcStatus);

        ratioPhaseData.parameters = applyCorrectionFactors(endParameters, rtcEndCorrectionFactors,
            ptcEndCorrectionFactors, newX);

        // network side always at end1
        if (alternative.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.STAR_BUS_SIDE) {

            ratioPhaseData.tapChanger.rtc2.ratio = rtcStatus.ratio();
            ratioPhaseData.tapChanger.rtc2.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            ratioPhaseData.tapChanger.rtc2.changeable = transformerEnd.rtc().hasDifferentRatios();
            ratioPhaseData.tapChanger.ptc2.ratio = ptcStatus.ratio();
            ratioPhaseData.tapChanger.ptc2.angle = ptcStatus.angle();
            ratioPhaseData.tapChanger.ptc2.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            ratioPhaseData.tapChanger.ptc2.changeable = transformerEnd.ptc().hasDifferentRatiosAngles();

        } else if (alternative
            .getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE) {

            ratioPhaseData.tapChanger.rtc1.ratio = rtcStatus.ratio();
            ratioPhaseData.tapChanger.rtc1.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            ratioPhaseData.tapChanger.rtc1.changeable = transformerEnd.rtc().hasDifferentRatios();
            ratioPhaseData.tapChanger.ptc1.ratio = ptcStatus.ratio();
            ratioPhaseData.tapChanger.ptc1.angle = ptcStatus.angle();
            ratioPhaseData.tapChanger.ptc1.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            ratioPhaseData.tapChanger.ptc1.changeable = transformerEnd.ptc().hasDifferentRatiosAngles();
        }

        return ratioPhaseData;
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

    private InterpretedBranch.PhaseAngleClocks interpretPhaseAngleClockEnd(CgmesTransformerEnd transformerEnd,
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
    private InterpretedBranch.TransformerEndParameters applyCorrectionFactors(
        InterpretedBranch.TransformerEndParameters initialEndParameters,
        InterpretedBranch.CorrectionFactors rtcEndParameterCorrections,
        InterpretedBranch.CorrectionFactors ptcEndParameterCorrections,
        double newX) {

        InterpretedBranch.TransformerEndParameters finalEndParameters = new InterpretedBranch.TransformerEndParameters();

        finalEndParameters.r = initialEndParameters.r * rtcEndParameterCorrections.r * ptcEndParameterCorrections.r;
        finalEndParameters.g = initialEndParameters.g * rtcEndParameterCorrections.g * ptcEndParameterCorrections.g;
        finalEndParameters.b = initialEndParameters.b * rtcEndParameterCorrections.b * ptcEndParameterCorrections.b;

        finalEndParameters.x = newX * rtcEndParameterCorrections.x * ptcEndParameterCorrections.x;

        return finalEndParameters;
    }

    private InterpretedBranch.Ratios calculateRatioEnd(XXX endRatioPhaseData,
        InterpretedBranch.Ratios endStructuralRatioData) {

        InterpretedBranch.Ratios ratioData = new InterpretedBranch.Ratios();

        ratioData.a1 = endRatioPhaseData.tapChanger.rtc1.ratio * endRatioPhaseData.tapChanger.ptc1.ratio
            * endStructuralRatioData.a1;
        ratioData.a2 = endRatioPhaseData.tapChanger.rtc2.ratio * endRatioPhaseData.tapChanger.ptc2.ratio
            * endStructuralRatioData.a2;

        return ratioData;
    }

    static class XXX {
        InterpretedBranch.TransformerEndParameters parameters = new InterpretedBranch.TransformerEndParameters();
        InterpretedBranch.TapChangers tapChanger = new InterpretedBranch.TapChangers();

        private void addPhase(InterpretedBranch.PhaseAngleClocks pacs) {
            tapChanger.ptc1.angle += pacs.angle1;
            tapChanger.ptc2.angle += pacs.angle2;
        }
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
