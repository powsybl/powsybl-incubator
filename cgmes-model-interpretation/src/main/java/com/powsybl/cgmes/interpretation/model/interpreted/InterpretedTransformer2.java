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
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr2PhaseAngleClockAlternative;
import com.powsybl.commons.PowsyblException;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretedTransformer2 {

    public InterpretedTransformer2(CgmesTransformer transformer, InterpretationAlternative alternative) {
        super();

        EndParameters end1Parameters = getEndParameters(transformer.end1());
        EndParameters end2Parameters = getEndParameters(transformer.end2());

        RatioPhaseData ratioPhaseData = interpretRatioPhase(transformer, end1Parameters, end2Parameters, alternative);

        // yshunt
        YShuntData yshuntData = interpretYShunt(ratioPhaseData.parameters, alternative);

        // phaseAngleClock
        PhaseAngleClockData phaseAngleClockData = interpretPhaseAngleClock(transformer, alternative);

        ratioPhaseData.addPhase(phaseAngleClockData);

        branchModel = new DetectedBranchModel(yshuntData.ysh1, yshuntData.ysh2,
                ratioPhaseData.tapChanger.rtc1, ratioPhaseData.tapChanger.ptc1,
                ratioPhaseData.tapChanger.rtc2, ratioPhaseData.tapChanger.ptc2);

        RatioData structuralRatioData = interpretStructuralRatio(transformer, alternative);
        RatioData ratioData = calculateRatio(ratioPhaseData, structuralRatioData);

        // admittance
        admittanceMatrix = new BranchAdmittanceMatrix(ratioPhaseData.parameters.r,
                ratioPhaseData.parameters.x, ratioData.a1, ratioPhaseData.tapChanger.ptc1.angle,
                yshuntData.ysh1, ratioData.a2, ratioPhaseData.tapChanger.ptc2.angle, yshuntData.ysh2);
    }

    private RatioPhaseData interpretRatioPhase(CgmesTransformer transformer,
            EndParameters end1Parameters, EndParameters end2Parameters, InterpretationAlternative alternative) {

        RatioPhaseData ratioPhaseData = new RatioPhaseData();

        // rtc1
        CgmesTapChangerStatus rtc1Status = transformer.end1().rtc().status();
        EndParameters rtc1EndCorrectionFactors = correctionFactorsEnd(rtc1Status);

        // ptc
        CgmesTapChangerStatus ptc1Status = transformer.end1().ptc().status();
        double newX1 = transformer.end1().ptc().overrideX(end1Parameters.x, ptc1Status.angle());
        EndParameters ptc1EndCorrectionFactors = correctionFactorsEnd(ptc1Status);

        // rtc2
        CgmesTapChangerStatus rtc2Status = transformer.end2().rtc().status();
        EndParameters rtc2EndCorrectionFactors = correctionFactorsEnd(rtc2Status);

        // ptc2
        CgmesTapChangerStatus ptc2Status = transformer.end2().ptc().status();
        double newX2 = transformer.end2().ptc().overrideX(end2Parameters.x, ptc2Status.angle());
        EndParameters ptc2EndCorrectionFactors = correctionFactorsEnd(ptc2Status);

        ratioPhaseData.parameters = mergeFactorCorrectionsParameters(end1Parameters, end2Parameters,
                rtc1EndCorrectionFactors, ptc1EndCorrectionFactors, newX1,
                rtc2EndCorrectionFactors, ptc2EndCorrectionFactors, newX2);

        // Prepare data to be recorded
        double rtc12Ratio = rtc1Status.ratio() * rtc2Status.ratio();
        boolean rtc12RegulatingControl = transformer.end1().rtc().isRegulatingControlEnabled() ||
                transformer.end2().rtc().isRegulatingControlEnabled();
        boolean rtc12Changeable = transformer.end1().rtc().hasDifferentRatios() ||
                transformer.end2().rtc().hasDifferentRatios();

        double ptc12Ratio = ptc1Status.ratio() * ptc2Status.ratio();
        double ptc12Angle = ptc1Status.angle() + ptc2Status.angle();
        boolean ptc12RegulatingControl = transformer.end1().ptc().isRegulatingControlEnabled() ||
                transformer.end2().ptc().isRegulatingControlEnabled();
        boolean ptc12Changeable = transformer.end1().ptc().hasDifferentRatiosAngles() ||
                transformer.end2().ptc().hasDifferentRatiosAngles();

        switch (alternative.getXfmr2RatioPhase()) {
            case END1:
                ratioPhaseData.setRatio1(rtc12Ratio, rtc12RegulatingControl, rtc12Changeable);
                ratioPhaseData.setPhase1(ptc12Ratio, ptc12Angle, ptc12RegulatingControl, ptc12Changeable);
                break;
            case END2:
                ratioPhaseData.setRatio2(rtc12Ratio, rtc12RegulatingControl, rtc12Changeable);
                ratioPhaseData.setPhase2(ptc12Ratio, ptc12Angle, ptc12RegulatingControl, ptc12Changeable);
                break;
            case END1_END2:
                ratioPhaseData.setRatio1(rtc1Status.ratio(), transformer.end1().rtc().isRegulatingControlEnabled(),
                        transformer.end1().rtc().hasDifferentRatios());
                ratioPhaseData.setPhase1(ptc1Status.ratio(), ptc1Status.angle(),
                        transformer.end1().ptc().isRegulatingControlEnabled(),
                        transformer.end1().ptc().hasDifferentRatiosAngles());

                ratioPhaseData.setRatio2(rtc2Status.ratio(), transformer.end2().rtc().isRegulatingControlEnabled(),
                        transformer.end2().rtc().hasDifferentRatios());
                ratioPhaseData.setPhase2(ptc2Status.ratio(), ptc2Status.angle(),
                        transformer.end2().ptc().isRegulatingControlEnabled(),
                        transformer.end2().ptc().hasDifferentRatiosAngles());

                break;
            case X:
                if (end1Parameters.x == 0.0) {
                    ratioPhaseData.setRatio1(rtc12Ratio, rtc12RegulatingControl, rtc12Changeable);
                    ratioPhaseData.setPhase1(ptc12Ratio, ptc12Angle, ptc12RegulatingControl, ptc12Changeable);
                } else {
                    ratioPhaseData.setRatio2(rtc12Ratio, rtc12RegulatingControl, rtc12Changeable);
                    ratioPhaseData.setPhase2(ptc12Ratio, ptc12Angle, ptc12RegulatingControl, ptc12Changeable);
                }
                break;
        }

        return ratioPhaseData;
    }

    private YShuntData interpretYShunt(Parameters parameters, InterpretationAlternative alternative) {
        YShuntData yShuntData = new YShuntData();

        switch (alternative.getXfmr2YShunt()) {
            case END1:
                yShuntData.ysh1 = new Complex(parameters.g1 + parameters.g2, parameters.b1 + parameters.b2);
                break;
            case END2:
                yShuntData.ysh2 = new Complex(parameters.g1 + parameters.g2, parameters.b1 + parameters.b2);
                break;
            case END1_END2:
                yShuntData.ysh1 = new Complex(parameters.g1, parameters.b1);
                yShuntData.ysh2 = new Complex(parameters.g2, parameters.b2);
                break;
            case SPLIT:
                yShuntData.ysh1 = new Complex((parameters.g1 + parameters.g2) * 0.5,
                        (parameters.b1 + parameters.b2) * 0.5);
                yShuntData.ysh2 = new Complex((parameters.g1 + parameters.g2) * 0.5,
                        (parameters.b1 + parameters.b2) * 0.5);
                break;
        }

        return yShuntData;
    }

    private PhaseAngleClockData interpretPhaseAngleClock(CgmesTransformer transformer,
            InterpretationAlternative alternative) {

        PhaseAngleClockData phaseAngleClockData = new PhaseAngleClockData();
        if (alternative.getXfmr2PhaseAngleClock() == Xfmr2PhaseAngleClockAlternative.END1_END2) {
            phaseAngleClockData.angle1 += transformer.end1().phaseAngleClockDegrees();

            double angle2 = transformer.end2().phaseAngleClockDegrees();
            if (alternative.isXfmr2Pac2Negate()) {
                angle2 = -angle2;
            }
            phaseAngleClockData.angle2 = angle2;
        }

        return phaseAngleClockData;
    }

    private RatioData interpretStructuralRatio(CgmesTransformer transformer, InterpretationAlternative alternative) {
        int rtcEnd = transformer.end1().rtc().hasStepVoltageIncrement() ? 1 : 2;
        double ratedU1 = transformer.end1().ratedU();
        double ratedU2 = transformer.end2().ratedU();
        RatioData structuralRatioData = new RatioData();
        switch (alternative.getXfmr2Ratio0()) {
            case END1:
                structuralRatioData.a1 = ratedU1 / ratedU2;
                break;
            case END2:
                structuralRatioData.a2 = ratedU2 / ratedU1;
                break;
            case RTC:
                if (rtcEnd == 1) {
                    structuralRatioData.a1 = ratedU1 / ratedU2;
                } else {
                    structuralRatioData.a2 = ratedU2 / ratedU1;
                }
                break;
            case X:
                if (transformer.end1().x() == 0.0) {
                    // Structural ratio in the side that has x == 0
                    structuralRatioData.a1 = ratedU1 / ratedU2;
                } else {
                    structuralRatioData.a2 = ratedU2 / ratedU1;
                }
                break;
            default:
                throw new PowsyblException("Unsupported alternative " + alternative.getXfmr2Ratio0());
        }

        return structuralRatioData;
    }

    // TODO warnings under incoherent cases
    private Parameters mergeFactorCorrectionsParameters(EndParameters initialEnd1Parameters,
            EndParameters initialEnd2Parameters, EndParameters rtc1EndCorrectionFactors,
            EndParameters ptc1EndCorrectionFactors, double newX1,
            EndParameters rtc2EndCorrectionFactors, EndParameters ptc2EndCorrectionFactors,
            double newX2) {

        Parameters parameters = new Parameters();

        if (initialEnd1Parameters.x != 0.0 && initialEnd2Parameters.x != 0.0) {
            parameters.r = initialEnd1Parameters.r * rtc1EndCorrectionFactors.r * ptc1EndCorrectionFactors.r;
            parameters.r += initialEnd2Parameters.r * rtc2EndCorrectionFactors.r * ptc2EndCorrectionFactors.r;
            parameters.x = newX1 * rtc1EndCorrectionFactors.x * ptc1EndCorrectionFactors.x;
            parameters.x += newX2 * rtc2EndCorrectionFactors.x * ptc2EndCorrectionFactors.x;
            parameters.g1 = initialEnd1Parameters.g * rtc1EndCorrectionFactors.g * ptc1EndCorrectionFactors.g;
            parameters.g2 = initialEnd2Parameters.g * rtc2EndCorrectionFactors.g * ptc2EndCorrectionFactors.g;
            parameters.b1 = initialEnd1Parameters.b * rtc1EndCorrectionFactors.b * ptc1EndCorrectionFactors.b;
            parameters.b2 = initialEnd2Parameters.b * rtc2EndCorrectionFactors.b * ptc2EndCorrectionFactors.b;
        } else if (initialEnd1Parameters.x != 0.0) {
            parameters.r = initialEnd1Parameters.r * rtc1EndCorrectionFactors.r * ptc1EndCorrectionFactors.r;
            parameters.r *= rtc2EndCorrectionFactors.r * ptc2EndCorrectionFactors.r;
            if (newX1 != 0.0 && newX1 != initialEnd1Parameters.x) {
                parameters.x = newX1;
            } else if (newX2 != 0.0 && newX2 != initialEnd1Parameters.x) {
                parameters.x = newX2;
            } else {
                parameters.x = initialEnd1Parameters.x;
            }
            parameters.x *= rtc1EndCorrectionFactors.x * ptc1EndCorrectionFactors.x;
            parameters.x *= rtc2EndCorrectionFactors.x * ptc2EndCorrectionFactors.x;
            parameters.g1 = initialEnd1Parameters.g * rtc1EndCorrectionFactors.g * ptc1EndCorrectionFactors.g;
            parameters.g1 *= rtc2EndCorrectionFactors.g * ptc2EndCorrectionFactors.g;
            parameters.b1 = initialEnd1Parameters.b * rtc1EndCorrectionFactors.b * ptc1EndCorrectionFactors.b;
            parameters.b1 *= rtc2EndCorrectionFactors.b * ptc2EndCorrectionFactors.b;
        } else {
            parameters.r = initialEnd2Parameters.r * rtc1EndCorrectionFactors.r * ptc1EndCorrectionFactors.r;
            parameters.r *= rtc2EndCorrectionFactors.r * ptc2EndCorrectionFactors.r;
            if (newX1 != 0.0 && newX1 != initialEnd2Parameters.x) {
                parameters.x = newX1;
            } else if (newX2 != 0.0 && newX2 != initialEnd2Parameters.x) {
                parameters.x = newX2;
            } else {
                parameters.x = initialEnd2Parameters.x;
            }
            parameters.x *= rtc1EndCorrectionFactors.x * ptc1EndCorrectionFactors.x;
            parameters.x *= rtc2EndCorrectionFactors.x * ptc2EndCorrectionFactors.x;
            parameters.g2 = initialEnd2Parameters.g * rtc1EndCorrectionFactors.g * ptc1EndCorrectionFactors.g;
            parameters.g2 *= rtc2EndCorrectionFactors.g * ptc2EndCorrectionFactors.g;
            parameters.b2 = initialEnd2Parameters.b * rtc1EndCorrectionFactors.b * ptc1EndCorrectionFactors.b;
            parameters.b2 *= rtc2EndCorrectionFactors.b * ptc2EndCorrectionFactors.b;
        }

        return parameters;
    }

    private RatioData calculateRatio(RatioPhaseData ratioPhaseData, RatioData structuralRatioData) {

        RatioData ratioData = new RatioData();

        ratioData.a1 = ratioPhaseData.tapChanger.rtc1.ratio * ratioPhaseData.tapChanger.ptc1.ratio * structuralRatioData.a1;
        ratioData.a2 = ratioPhaseData.tapChanger.rtc2.ratio * ratioPhaseData.tapChanger.ptc2.ratio * structuralRatioData.a2;

        return ratioData;
    }

    static class RatioPhaseData {
        Parameters parameters = new Parameters();
        BranchInterpretedTapChangers tapChanger = new BranchInterpretedTapChangers();

        private void setRatio1(double ratio, boolean regulatingControl, boolean changeable) {
            tapChanger.rtc1.ratio = ratio;
            tapChanger.rtc1.regulatingControl = regulatingControl;
            tapChanger.rtc1.changeable = changeable;
        }

        private void setPhase1(double ratio, double angle, boolean regulatingControl, boolean changeable) {
            tapChanger.ptc1.ratio = ratio;
            tapChanger.ptc1.angle = angle;
            tapChanger.ptc1.regulatingControl = regulatingControl;
            tapChanger.ptc1.changeable = changeable;
        }

        private void setRatio2(double ratio, boolean regulatingControl, boolean changeable) {
            tapChanger.rtc2.ratio = ratio;
            tapChanger.rtc2.regulatingControl = regulatingControl;
            tapChanger.rtc2.changeable = changeable;
        }

        private void setPhase2(double ratio, double angle, boolean regulatingControl, boolean changeable) {
            tapChanger.ptc2.ratio = ratio;
            tapChanger.ptc2.angle = angle;
            tapChanger.ptc2.regulatingControl = regulatingControl;
            tapChanger.ptc2.changeable = changeable;
        }

        private void addPhase(PhaseAngleClockData phaseAngleClockData) {
            tapChanger.ptc1.angle += phaseAngleClockData.angle1;
            tapChanger.ptc2.angle += phaseAngleClockData.angle2;
        }
    }

    static class Parameters {
        double r = 0.0;
        double x = 0.0;
        double g1 = 0.0;
        double b1 = 0.0;
        double g2 = 0.0;
        double b2 = 0.0;
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

    public DetectedBranchModel getBranchModel() {
        return branchModel;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrix() {
        return admittanceMatrix;
    }

    private final BranchAdmittanceMatrix admittanceMatrix;
    private final DetectedBranchModel branchModel;
}
