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

    public InterpretedTransformer3(CgmesTransformer transformer, InterpretationAlternative config) {
        super();

        EndParameters end1Parameters = getEndParameters(transformer.end1());
        EndParameters end2Parameters = getEndParameters(transformer.end2());
        EndParameters end3Parameters = getEndParameters(transformer.end3());

        RatioPhaseData end1RatioPhaseData = getRatioPhaseEnd(transformer.end1(), end1Parameters, config);
        RatioPhaseData end2RatioPhaseData = getRatioPhaseEnd(transformer.end2(), end2Parameters, config);
        RatioPhaseData end3RatioPhaseData = getRatioPhaseEnd(transformer.end3(), end3Parameters, config);

        // yshunt
        YShuntData end1YShuntData = getYShuntEnd(end1RatioPhaseData.parameters, config);
        YShuntData end2YShuntData = getYShuntEnd(end2RatioPhaseData.parameters, config);
        YShuntData end3YShuntData = getYShuntEnd(end3RatioPhaseData.parameters, config);

        // phaseAngleClock
        PhaseAngleClockData end1PhaseAngleClockData = getPhaseAngleClockEnd(transformer.end1(), config);
        PhaseAngleClockData end2PhaseAngleClockData = getPhaseAngleClockEnd(transformer.end2(), config);
        PhaseAngleClockData end3PhaseAngleClockData = getPhaseAngleClockEnd(transformer.end3(), config);

        end1RatioPhaseData.addPhase(end1PhaseAngleClockData);
        end2RatioPhaseData.addPhase(end2PhaseAngleClockData);
        end3RatioPhaseData.addPhase(end3PhaseAngleClockData);

        branchModelEnd1 = new DetectedBranchModel(end1YShuntData.ysh1, end1YShuntData.ysh2,
                end1RatioPhaseData.tapChanger.rtc1, end1RatioPhaseData.tapChanger.ptc1, end1RatioPhaseData.tapChanger.rtc2, end1RatioPhaseData.tapChanger.ptc2);
        branchModelEnd2 = new DetectedBranchModel(end2YShuntData.ysh1, end2YShuntData.ysh2,
                end2RatioPhaseData.tapChanger.rtc1, end2RatioPhaseData.tapChanger.ptc1, end2RatioPhaseData.tapChanger.rtc2, end2RatioPhaseData.tapChanger.ptc2);
        branchModelEnd3 = new DetectedBranchModel(end3YShuntData.ysh1, end3YShuntData.ysh2,
                end3RatioPhaseData.tapChanger.rtc1, end3RatioPhaseData.tapChanger.ptc1, end3RatioPhaseData.tapChanger.rtc2, end3RatioPhaseData.tapChanger.ptc2);

        // add structural ratio after detected branch model
        double ratedU0 = transformer.end1().ratedU();
        Ratio0Data end1Ratio0Data = getRatio0End(transformer.end1(), config, ratedU0);
        Ratio0Data end2Ratio0Data = getRatio0End(transformer.end2(), config, ratedU0);
        Ratio0Data end3Ratio0Data = getRatio0End(transformer.end3(), config, ratedU0);

        RatioData end1RatioData = calculateRatioEnd(end1RatioPhaseData, end1Ratio0Data);
        RatioData end2RatioData = calculateRatioEnd(end2RatioPhaseData, end2Ratio0Data);
        RatioData end3RatioData = calculateRatioEnd(end3RatioPhaseData, end3Ratio0Data);

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

    private Ratio0Data getRatio0End(CgmesTransformerEnd transformerEnd, InterpretationAlternative config, double ratedU0) {
        double ratedU = transformerEnd.ratedU();
        Ratio0Data ratio0Data = new Ratio0Data();
        if (config.getXfmr3Ratio0StarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.STAR_BUS_SIDE) {
            ratio0Data.a01 = 1.0;
            ratio0Data.a02 = ratedU0 / ratedU;
        } else if (config.getXfmr3Ratio0StarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE) {
            ratio0Data.a01 = ratedU / ratedU0;
            ratio0Data.a02 = 1.0;
        }
        return ratio0Data;
    }

    private RatioPhaseData getRatioPhaseEnd(CgmesTransformerEnd transformerEnd,
            EndParameters endParameters, InterpretationAlternative config) {

        RatioPhaseData ratioPhaseData = new RatioPhaseData();

        // rtc
        CgmesTapChangerStatus rtcStatus = transformerEnd.rtc().status();
        EndParameters rtcEndParameters = correctParametersEnd(rtcStatus);

        // ptc
        CgmesTapChangerStatus ptcStatus = transformerEnd.ptc().status();
        double newX = transformerEnd.ptc().overrideX(endParameters.x, ptcStatus.angle());
        EndParameters ptcEndParameters = correctParametersEnd(ptcStatus);

        ratioPhaseData.parameters = mergeFactorCorrectionsParameters(endParameters, rtcEndParameters, ptcEndParameters, newX);

        // network side always at end1
        if (config.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.STAR_BUS_SIDE) {

            ratioPhaseData.tapChanger.rtc2.ratio = rtcStatus.ratio();
            ratioPhaseData.tapChanger.rtc2.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            ratioPhaseData.tapChanger.rtc2.changeable = transformerEnd.rtc().hasDifferentRatios();
            ratioPhaseData.tapChanger.ptc2.ratio = ptcStatus.ratio();
            ratioPhaseData.tapChanger.ptc2.angle = ptcStatus.angle();
            ratioPhaseData.tapChanger.ptc2.regulatingControl = transformerEnd.rtc().isRegulatingControlEnabled();
            ratioPhaseData.tapChanger.ptc2.changeable = transformerEnd.ptc().hasDifferentRatiosAngles();

        } else if (config.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE) {

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

    private YShuntData getYShuntEnd(EndParameters endParameters, InterpretationAlternative config) {
        Xfmr3ShuntInterpretationAlternative xfmr3YShunt = config.getXfmr3YShunt();
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

    private PhaseAngleClockData getPhaseAngleClockEnd(CgmesTransformerEnd transformerEnd,
            InterpretationAlternative config) {
        double angle = transformerEnd.phaseAngleClockDegrees();
        PhaseAngleClockData phaseAngleClockData = new PhaseAngleClockData();
        if (config.getXfmr3PhaseAngleClock() == Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE) {
            phaseAngleClockData.angle2 = angle;
        } else if (config.getXfmr3PhaseAngleClock() == Xfmr3PhaseAngleClockAlternative.NETWORK_SIDE) {
            phaseAngleClockData.angle1 = angle;
        }
        return phaseAngleClockData;
    }

    private EndParameters getEndParameters(CgmesTransformerEnd transformerEnd) {
        EndParameters endParameters = new EndParameters();

        endParameters.r = transformerEnd.r();
        endParameters.x = transformerEnd.x();
        endParameters.b = transformerEnd.b();
        endParameters.g = transformerEnd.g();

        return endParameters;
    }

    private EndParameters correctParametersEnd(CgmesTapChangerStatus tapChangerData) {

        EndParameters newEndParameters = new EndParameters();

        newEndParameters.r = CgmesTapChanger.correction(tapChangerData.rc());
        newEndParameters.x = CgmesTapChanger.correction(tapChangerData.xc());
        newEndParameters.g = CgmesTapChanger.correction(tapChangerData.gc());
        newEndParameters.b = CgmesTapChanger.correction(tapChangerData.bc());

        return newEndParameters;
    }

    // TODO warnings under incoherent configurations
    private EndParameters mergeFactorCorrectionsParameters(EndParameters initialEndParameters,
            EndParameters rtcEndParameterCorrections, EndParameters ptcEndParameterCorrections, double newX) {

        EndParameters finalEndParameters = new EndParameters();

        finalEndParameters.r = initialEndParameters.r * rtcEndParameterCorrections.r * ptcEndParameterCorrections.r;
        finalEndParameters.g = initialEndParameters.g * rtcEndParameterCorrections.g * ptcEndParameterCorrections.g;
        finalEndParameters.b = initialEndParameters.b * rtcEndParameterCorrections.b * ptcEndParameterCorrections.b;

        finalEndParameters.x = newX * rtcEndParameterCorrections.x * ptcEndParameterCorrections.x;

        return finalEndParameters;
    }

    private RatioData calculateRatioEnd(RatioPhaseData endRatioPhaseData, Ratio0Data endRatio0Data) {

        RatioData ratioData = new RatioData();

        ratioData.a1 = endRatioPhaseData.tapChanger.rtc1.ratio * endRatioPhaseData.tapChanger.ptc1.ratio * endRatio0Data.a01;
        ratioData.a2 = endRatioPhaseData.tapChanger.rtc2.ratio * endRatioPhaseData.tapChanger.ptc2.ratio * endRatio0Data.a02;

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

    static class Ratio0Data {
        double a01 = 1.0;
        double a02 = 1.0;
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
