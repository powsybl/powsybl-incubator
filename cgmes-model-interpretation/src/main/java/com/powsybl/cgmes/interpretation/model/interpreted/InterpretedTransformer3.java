/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTapChanger;
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
        super();

        EndParameters end1Parameters = getEndParameters(transformer.end1());
        EndParameters end2Parameters = getEndParameters(transformer.end2());
        EndParameters end3Parameters = getEndParameters(transformer.end3());

        RatioPhaseData end1RatioPhaseData = interpretRatioPhaseEnd(transformer.end1(), end1Parameters, alternative);
        RatioPhaseData end2RatioPhaseData = interpretRatioPhaseEnd(transformer.end2(), end2Parameters, alternative);
        RatioPhaseData end3RatioPhaseData = interpretRatioPhaseEnd(transformer.end3(), end3Parameters, alternative);

        // yshunt
        YShuntData end1YShuntData = interpretYShuntEnd(end1RatioPhaseData.parameters, alternative);
        YShuntData end2YShuntData = interpretYShuntEnd(end2RatioPhaseData.parameters, alternative);
        YShuntData end3YShuntData = interpretYShuntEnd(end3RatioPhaseData.parameters, alternative);

        // phaseAngleClock
        PhaseAngleClockData end1PhaseAngleClockData = interpretPhaseAngleClockEnd(transformer.end1(), alternative);
        PhaseAngleClockData end2PhaseAngleClockData = interpretPhaseAngleClockEnd(transformer.end2(), alternative);
        PhaseAngleClockData end3PhaseAngleClockData = interpretPhaseAngleClockEnd(transformer.end3(), alternative);

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
        RatioData end1StructuralRatioData = interpretStructuralRatioEnd(transformer.end1(), alternative, ratedU0);
        RatioData end2StructuralRatioData = interpretStructuralRatioEnd(transformer.end2(), alternative, ratedU0);
        RatioData end3StructuralRatioData = interpretStructuralRatioEnd(transformer.end3(), alternative, ratedU0);

        RatioData end1RatioData = calculateRatioEnd(end1RatioPhaseData, end1StructuralRatioData);
        RatioData end2RatioData = calculateRatioEnd(end2RatioPhaseData, end2StructuralRatioData);
        RatioData end3RatioData = calculateRatioEnd(end3RatioPhaseData, end3StructuralRatioData);

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

    private RatioPhaseData interpretRatioPhaseEnd(CgmesTransformerEnd transformerEnd,
            EndParameters endParameters, InterpretationAlternative alternative) {

        RatioPhaseData ratioPhaseData = new RatioPhaseData();

        // rtc
        CgmesTapChangerStatus rtcStatus = transformerEnd.rtc().status();
        EndParameters rtcEndCorrectionFactors = correctionFactorsEnd(rtcStatus);

        // ptc
        CgmesTapChangerStatus ptcStatus = transformerEnd.ptc().status();
        double newX = transformerEnd.ptc().overrideX(endParameters.x, ptcStatus.angle());
        EndParameters ptcEndCorrectionFactors = correctionFactorsEnd(ptcStatus);

        ratioPhaseData.parameters = mergeFactorCorrectionsParameters(endParameters, rtcEndCorrectionFactors,
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

        } else if (alternative.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE) {

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

    private YShuntData interpretYShuntEnd(EndParameters endParameters, InterpretationAlternative alternative) {
        Xfmr3ShuntInterpretationAlternative xfmr3YShunt = alternative.getXfmr3YShunt();
        YShuntData yShuntData = new YShuntData();
        switch (xfmr3YShunt) {
            case NETWORK_SIDE:
                yShuntData.ysh1 = new Complex(endParameters.g, endParameters.b);
                break;
            case STAR_BUS_SIDE:
                yShuntData.ysh2 = new Complex(endParameters.g, endParameters.b);
                break;
            case SPLIT:
                yShuntData.ysh1 = new Complex(endParameters.g * 0.5, endParameters.b * 0.5);
                yShuntData.ysh2 = new Complex(endParameters.g * 0.5, endParameters.b * 0.5);
                break;
        }
        return yShuntData;
    }

    private PhaseAngleClockData interpretPhaseAngleClockEnd(CgmesTransformerEnd transformerEnd,
            InterpretationAlternative alternative) {

        PhaseAngleClockData phaseAngleClockData = new PhaseAngleClockData();
        if (alternative.getXfmr3PhaseAngleClock() == Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE) {
            phaseAngleClockData.angle2 = transformerEnd.phaseAngleClockDegrees();
        } else if (alternative.getXfmr3PhaseAngleClock() == Xfmr3PhaseAngleClockAlternative.NETWORK_SIDE) {
            phaseAngleClockData.angle1 = transformerEnd.phaseAngleClockDegrees();
        }
        return phaseAngleClockData;
    }

    private RatioData interpretStructuralRatioEnd(CgmesTransformerEnd transformerEnd,
            InterpretationAlternative alternative, double ratedU0) {
        double ratedU = transformerEnd.ratedU();
        RatioData structuralRatioData = new RatioData();
        if (alternative.getXfmr3Ratio0StarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.STAR_BUS_SIDE) {
            structuralRatioData.a1 = 1.0;
            structuralRatioData.a2 = ratedU0 / ratedU;
        } else if (alternative.getXfmr3Ratio0StarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE) {
            structuralRatioData.a1 = ratedU / ratedU0;
            structuralRatioData.a2 = 1.0;
        }
        return structuralRatioData;
    }

    private EndParameters getEndParameters(CgmesTransformerEnd transformerEnd) {
        EndParameters endParameters = new EndParameters();

        endParameters.r = transformerEnd.r();
        endParameters.x = transformerEnd.x();
        endParameters.b = transformerEnd.b();
        endParameters.g = transformerEnd.g();

        return endParameters;
    }

    private EndParameters correctionFactorsEnd(CgmesTapChangerStatus tapChangerData) {

        EndParameters newEndParameters = new EndParameters();

        newEndParameters.r = CgmesTapChanger.getCorrectionFactor(tapChangerData.rc());
        newEndParameters.x = CgmesTapChanger.getCorrectionFactor(tapChangerData.xc());
        newEndParameters.g = CgmesTapChanger.getCorrectionFactor(tapChangerData.gc());
        newEndParameters.b = CgmesTapChanger.getCorrectionFactor(tapChangerData.bc());

        return newEndParameters;
    }

    // TODO warnings under incoherent cases
    private EndParameters mergeFactorCorrectionsParameters(EndParameters initialEndParameters,
            EndParameters rtcEndParameterCorrections, EndParameters ptcEndParameterCorrections, double newX) {

        EndParameters finalEndParameters = new EndParameters();

        finalEndParameters.r = initialEndParameters.r * rtcEndParameterCorrections.r * ptcEndParameterCorrections.r;
        finalEndParameters.g = initialEndParameters.g * rtcEndParameterCorrections.g * ptcEndParameterCorrections.g;
        finalEndParameters.b = initialEndParameters.b * rtcEndParameterCorrections.b * ptcEndParameterCorrections.b;

        finalEndParameters.x = newX * rtcEndParameterCorrections.x * ptcEndParameterCorrections.x;

        return finalEndParameters;
    }

    private RatioData calculateRatioEnd(RatioPhaseData endRatioPhaseData, RatioData endStructuralRatioData) {

        RatioData ratioData = new RatioData();

        ratioData.a1 = endRatioPhaseData.tapChanger.rtc1.ratio * endRatioPhaseData.tapChanger.ptc1.ratio * endStructuralRatioData.a1;
        ratioData.a2 = endRatioPhaseData.tapChanger.rtc2.ratio * endRatioPhaseData.tapChanger.ptc2.ratio * endStructuralRatioData.a2;

        return ratioData;
    }

    static class RatioPhaseData {
        EndParameters parameters = new EndParameters();
        BranchInterpretedTapChangers tapChanger = new BranchInterpretedTapChangers();

        private void addPhase(PhaseAngleClockData endPhaseAngleClockData) {

            tapChanger.ptc1.angle += endPhaseAngleClockData.angle1;
            tapChanger.ptc2.angle += endPhaseAngleClockData.angle2;
        }
    }

    static class EndParameters {
        double r = 0.0;
        double x = 0.0;
        double g = 0.0;
        double b = 0.0;
    }

    static class YShuntData {
        Complex ysh1 = Complex.ZERO;
        Complex ysh2 = Complex.ZERO;
    }

    static class PhaseAngleClockData {
        double angle1 = 0.0;
        double angle2 = 0.0;
    }

    static class RatioData {
        double a1 = 1.0;
        double a2 = 1.0;
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
