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
import com.powsybl.cgmes.interpretation.model.interpreted.BranchInterpretedTapChangers.InterpretedPhaseTapChanger;
import com.powsybl.cgmes.interpretation.model.interpreted.BranchInterpretedTapChangers.InterpretedRatioTapChanger;
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
        this.transformer = transformer;

        r1 = transformer.end1().r();
        x1 = transformer.end1().x();
        b1 = transformer.end1().b();
        g1 = transformer.end1().g();

        r2 = transformer.end2().r();
        x2 = transformer.end2().x();
        b2 = transformer.end2().b();
        g2 = transformer.end2().g();

        r3 = transformer.end3().r();
        x3 = transformer.end3().x();
        b3 = transformer.end3().b();
        g3 = transformer.end3().g();

        Xfmr3InterpretedTapChangers ratioPhaseData = getXfmr3RatioPhase(config);
        InterpretedRatioTapChanger ratio11 = ratioPhaseData.end1.rtc1;
        InterpretedPhaseTapChanger phase11 = ratioPhaseData.end1.ptc1;
        InterpretedRatioTapChanger ratio12 = ratioPhaseData.end1.rtc2;
        InterpretedPhaseTapChanger phase12 = ratioPhaseData.end1.ptc2;
        InterpretedRatioTapChanger ratio21 = ratioPhaseData.end2.rtc1;
        InterpretedPhaseTapChanger phase21 = ratioPhaseData.end2.ptc1;
        InterpretedRatioTapChanger ratio22 = ratioPhaseData.end2.rtc2;
        InterpretedPhaseTapChanger phase22 = ratioPhaseData.end2.ptc2;
        InterpretedRatioTapChanger ratio31 = ratioPhaseData.end3.rtc1;
        InterpretedPhaseTapChanger phase31 = ratioPhaseData.end3.ptc1;
        InterpretedRatioTapChanger ratio32 = ratioPhaseData.end3.rtc2;
        InterpretedPhaseTapChanger phase32 = ratioPhaseData.end3.ptc2;

        // yshunt
        Xfmr3YShuntData yShuntData = getXfmr3YShunt(config);
        Complex ysh11 = yShuntData.end1.ysh1;
        Complex ysh12 = yShuntData.end1.ysh2;
        Complex ysh21 = yShuntData.end2.ysh1;
        Complex ysh22 = yShuntData.end2.ysh2;
        Complex ysh31 = yShuntData.end3.ysh1;
        Complex ysh32 = yShuntData.end3.ysh2;

        // phaseAngleClock
        Xfmr3PhaseAngleClockData phaseAngleClockData = getXfmr3PhaseAngleClock(config);
        phase11.angle += phaseAngleClockData.end1.angle1;
        phase12.angle += phaseAngleClockData.end1.angle2;
        phase21.angle += phaseAngleClockData.end2.angle1;
        phase22.angle += phaseAngleClockData.end2.angle2;
        phase31.angle += phaseAngleClockData.end3.angle1;
        phase32.angle += phaseAngleClockData.end3.angle2;

        branchModelEnd1 = new DetectedBranchModel(ysh11, ysh12, ratio11, phase11, ratio12, phase12);
        branchModelEnd2 = new DetectedBranchModel(ysh21, ysh22, ratio21, phase21, ratio22, phase22);
        branchModelEnd3 = new DetectedBranchModel(ysh31, ysh32, ratio31, phase31, ratio32, phase32);

        double a11 = ratio11.ratio * phase11.ratio;
        double a12 = ratio12.ratio * phase12.ratio;
        double a21 = ratio21.ratio * phase21.ratio;
        double a22 = ratio22.ratio * phase22.ratio;
        double a31 = ratio31.ratio * phase31.ratio;
        double a32 = ratio32.ratio * phase32.ratio;

        // add structural ratio after detected branch model
        double ratedU0 = 1.0;
        Xfmr3Ratio0Data ratio0Data = getXfmr3Ratio0(config, ratedU0);
        a11 *= ratio0Data.end1.a01;
        a12 *= ratio0Data.end1.a02;
        a21 *= ratio0Data.end2.a01;
        a22 *= ratio0Data.end2.a02;
        a31 *= ratio0Data.end3.a01;
        a32 *= ratio0Data.end3.a02;

        // admittance
        admittanceMatrixEnd1 = new BranchAdmittanceMatrix(r1, x1, a11, phase11.angle, ysh11, a12, phase12.angle, ysh12);
        admittanceMatrixEnd2 = new BranchAdmittanceMatrix(r2, x2, a21, phase21.angle, ysh21, a22, phase22.angle, ysh22);
        admittanceMatrixEnd3 = new BranchAdmittanceMatrix(r3, x3, a31, phase31.angle, ysh31, a32, phase32.angle, ysh32);
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

    private Xfmr3Ratio0Data getXfmr3Ratio0(InterpretationAlternative config, double ratedU0) {
        double ratedU1 = transformer.end1().ratedU();
        double ratedU2 = transformer.end2().ratedU();
        double ratedU3 = transformer.end3().ratedU();
        Xfmr3Ratio0Data ratio0Data = new Xfmr3Ratio0Data();
        if (config.getXfmr3Ratio0StarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.STAR_BUS_SIDE) {
            ratio0Data.end1.a01 = 1.0;
            ratio0Data.end1.a02 = ratedU0 / ratedU1;
            ratio0Data.end2.a01 = 1.0;
            ratio0Data.end2.a02 = ratedU0 / ratedU2;
            ratio0Data.end3.a01 = 1.0;
            ratio0Data.end3.a02 = ratedU0 / ratedU3;
        } else if (config.getXfmr3Ratio0StarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE) {
            ratio0Data.end1.a01 = ratedU1 / ratedU0;
            ratio0Data.end1.a02 = 1.0;
            ratio0Data.end2.a01 = ratedU2 / ratedU0;
            ratio0Data.end2.a02 = 1.0;
            ratio0Data.end3.a01 = ratedU3 / ratedU0;
            ratio0Data.end3.a02 = 1.0;
        }
        return ratio0Data;
    }

    private Xfmr3InterpretedTapChangers getXfmr3RatioPhase(InterpretationAlternative config) {

        Xfmr3InterpretedTapChangers ratioPhaseData = new Xfmr3InterpretedTapChangers();

        CgmesTapChangerStatus rtc1status = transformer.end1().rtc().status();
        double rtc1a = rtc1status.ratio();

        xfmr3ParametersCorrectionEnd1(rtc1status);

        // phase end1
        CgmesTapChangerStatus ptc1status = transformer.end1().ptc().status();
        double ptc1a = ptc1status.ratio();
        double ptc1A = ptc1status.angle();
        x1 = transformer.end1().ptc().overrideX(x1, ptc1A);

        xfmr3ParametersCorrectionEnd1(ptc1status);

        // ratio end2
        CgmesTapChangerStatus rtc2status = transformer.end2().rtc().status();
        double rtc2a = rtc2status.ratio();

        xfmr3ParametersCorrectionEnd2(rtc2status);

        // phase end2
        CgmesTapChangerStatus ptc2status = transformer.end2().ptc().status();
        double ptc2a = ptc2status.ratio();
        double ptc2A = ptc2status.angle();
        x2 = transformer.end1().ptc().overrideX(x2, ptc2A);

        xfmr3ParametersCorrectionEnd2(ptc2status);

        // ratio end3
        CgmesTapChangerStatus rtc3status = transformer.end3().rtc().status();
        double rtc3a = rtc3status.ratio();

        xfmr3ParametersCorrectionEnd3(rtc3status);

        // phase end3
        CgmesTapChangerStatus ptc3status = transformer.end3().ptc().status();
        double ptc3a = ptc3status.ratio();
        double ptc3A = ptc3status.angle();
        x2 = transformer.end1().ptc().overrideX(x2, ptc3A);

        xfmr3ParametersCorrectionEnd3(ptc3status);

        boolean rtc1DifferentRatios = transformer.end1().rtc().hasDifferentRatios();
        boolean rtc2DifferentRatios = transformer.end2().rtc().hasDifferentRatios();
        boolean rtc3DifferentRatios = transformer.end3().rtc().hasDifferentRatios();
        boolean ptc1DifferentRatiosAngles = transformer.end1().ptc().hasDifferentRatiosAngles();
        boolean ptc2DifferentRatiosAngles = transformer.end2().ptc().hasDifferentRatiosAngles();
        boolean ptc3DifferentRatiosAngles = transformer.end3().ptc().hasDifferentRatiosAngles();

        boolean rtc1RegulatingControl = transformer.end1().rtc().isRegulatingControlEnabled();
        boolean ptc1RegulatingControl = transformer.end1().ptc().isRegulatingControlEnabled();
        boolean rtc2RegulatingControl = transformer.end2().rtc().isRegulatingControlEnabled();
        boolean ptc2RegulatingControl = transformer.end2().ptc().isRegulatingControlEnabled();
        boolean rtc3RegulatingControl = transformer.end3().rtc().isRegulatingControlEnabled();
        boolean ptc3RegulatingControl = transformer.end3().ptc().isRegulatingControlEnabled();

        // network side always at end1
        if (config.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.STAR_BUS_SIDE) {
            ratioPhaseData.end1.rtc2.ratio = rtc1a;
            ratioPhaseData.end1.rtc2.regulatingControl = rtc1RegulatingControl;
            ratioPhaseData.end1.rtc2.changeable = rtc1DifferentRatios;
            ratioPhaseData.end1.ptc2.ratio = ptc1a;
            ratioPhaseData.end1.ptc2.angle = ptc1A;
            ratioPhaseData.end1.ptc2.regulatingControl = ptc1RegulatingControl;
            ratioPhaseData.end1.ptc2.changeable = ptc1DifferentRatiosAngles;
            ratioPhaseData.end2.rtc2.ratio = rtc2a;
            ratioPhaseData.end2.rtc2.regulatingControl = rtc2RegulatingControl;
            ratioPhaseData.end2.rtc2.changeable = rtc2DifferentRatios;
            ratioPhaseData.end2.ptc2.ratio = ptc2a;
            ratioPhaseData.end2.ptc2.angle = ptc2A;
            ratioPhaseData.end2.ptc2.regulatingControl = ptc2RegulatingControl;
            ratioPhaseData.end2.ptc2.changeable = ptc2DifferentRatiosAngles;
            ratioPhaseData.end3.rtc2.ratio = rtc3a;
            ratioPhaseData.end3.rtc2.regulatingControl = rtc3RegulatingControl;
            ratioPhaseData.end3.rtc2.changeable = rtc3DifferentRatios;
            ratioPhaseData.end3.ptc2.ratio = ptc3a;
            ratioPhaseData.end3.ptc2.angle = ptc3A;
            ratioPhaseData.end3.ptc2.regulatingControl = ptc3RegulatingControl;
            ratioPhaseData.end3.ptc2.changeable = ptc3DifferentRatiosAngles;
        } else if (config.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE) {
            ratioPhaseData.end1.rtc1.ratio = rtc1a;
            ratioPhaseData.end1.rtc1.regulatingControl = rtc1RegulatingControl;
            ratioPhaseData.end1.rtc1.changeable = rtc1DifferentRatios;
            ratioPhaseData.end1.ptc1.ratio = ptc1a;
            ratioPhaseData.end1.ptc1.angle = ptc1A;
            ratioPhaseData.end1.ptc1.regulatingControl = ptc1RegulatingControl;
            ratioPhaseData.end1.ptc1.changeable = ptc1DifferentRatiosAngles;
            ratioPhaseData.end2.rtc1.ratio = rtc2a;
            ratioPhaseData.end2.rtc1.regulatingControl = rtc2RegulatingControl;
            ratioPhaseData.end2.rtc1.changeable = rtc2DifferentRatios;
            ratioPhaseData.end2.ptc1.ratio = ptc2a;
            ratioPhaseData.end2.ptc1.angle = ptc2A;
            ratioPhaseData.end2.ptc1.regulatingControl = ptc2RegulatingControl;
            ratioPhaseData.end2.ptc1.changeable = ptc2DifferentRatiosAngles;
            ratioPhaseData.end3.rtc1.ratio = rtc3a;
            ratioPhaseData.end3.rtc1.regulatingControl = rtc3RegulatingControl;
            ratioPhaseData.end3.rtc1.changeable = rtc3DifferentRatios;
            ratioPhaseData.end3.ptc1.ratio = ptc3a;
            ratioPhaseData.end3.ptc1.angle = ptc3A;
            ratioPhaseData.end3.ptc1.regulatingControl = ptc3RegulatingControl;
            ratioPhaseData.end3.ptc1.changeable = ptc3DifferentRatiosAngles;
        }

        return ratioPhaseData;
    }

    private Xfmr3YShuntData getXfmr3YShunt(InterpretationAlternative config) {
        Xfmr3ShuntInterpretationAlternative xfmr3YShunt = config.getXfmr3YShunt();
        Xfmr3YShuntData yShuntData = new Xfmr3YShuntData();
        switch (xfmr3YShunt) {
            case NETWORK_SIDE:
                yShuntData.end1.ysh1 = yShuntData.end1.ysh1.add(new Complex(g1, b1));
                yShuntData.end2.ysh1 = yShuntData.end2.ysh1.add(new Complex(g2, b2));
                yShuntData.end3.ysh1 = yShuntData.end3.ysh1.add(new Complex(g3, b3));
                break;
            case STAR_BUS_SIDE:
                yShuntData.end1.ysh2 = yShuntData.end1.ysh2.add(new Complex(g1, b1));
                yShuntData.end2.ysh2 = yShuntData.end2.ysh2.add(new Complex(g2, b2));
                yShuntData.end3.ysh2 = yShuntData.end3.ysh2.add(new Complex(g3, b3));
                break;
            case SPLIT:
                yShuntData.end1.ysh1 = yShuntData.end1.ysh1.add(new Complex(g1 * 0.5, b1 * 0.5));
                yShuntData.end2.ysh1 = yShuntData.end2.ysh1.add(new Complex(g2 * 0.5, b2 * 0.5));
                yShuntData.end3.ysh1 = yShuntData.end3.ysh1.add(new Complex(g3 * 0.5, b3 * 0.5));
                yShuntData.end1.ysh2 = yShuntData.end1.ysh2.add(new Complex(g1 * 0.5, b1 * 0.5));
                yShuntData.end2.ysh2 = yShuntData.end2.ysh2.add(new Complex(g2 * 0.5, b2 * 0.5));
                yShuntData.end3.ysh2 = yShuntData.end3.ysh2.add(new Complex(g3 * 0.5, b3 * 0.5));
                break;
        }
        return yShuntData;
    }

    private Xfmr3PhaseAngleClockData getXfmr3PhaseAngleClock(InterpretationAlternative config) {
        double angle1 = transformer.end1().phaseAngleClockDegrees();
        double angle2 = transformer.end2().phaseAngleClockDegrees();
        double angle3 = transformer.end3().phaseAngleClockDegrees();
        Xfmr3PhaseAngleClockData phaseAngleClockData = new Xfmr3PhaseAngleClockData();
        if (config.getXfmr3PhaseAngleClock() == Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE) {
            phaseAngleClockData.end1.angle2 = angle1;
            phaseAngleClockData.end2.angle2 = angle2;
            phaseAngleClockData.end3.angle2 = angle3;
        } else if (config.getXfmr3PhaseAngleClock() == Xfmr3PhaseAngleClockAlternative.NETWORK_SIDE) {
            phaseAngleClockData.end1.angle1 = angle1;
            phaseAngleClockData.end2.angle1 = angle2;
            phaseAngleClockData.end3.angle1 = angle3;
        }
        return phaseAngleClockData;
    }

    private void xfmr3ParametersCorrectionEnd1(CgmesTapChangerStatus tapChangerData) {
        double xc = tapChangerData.xc();
        double rc = tapChangerData.rc();
        double bc = tapChangerData.bc();
        double gc = tapChangerData.gc();

        x1 = CgmesTapChanger.applyCorrection(x1, xc);
        r1 = CgmesTapChanger.applyCorrection(r1, rc);
        b1 = CgmesTapChanger.applyCorrection(b1, bc);
        g1 = CgmesTapChanger.applyCorrection(g1, gc);
    }

    private void xfmr3ParametersCorrectionEnd2(CgmesTapChangerStatus tapChangerData) {
        double xc = tapChangerData.xc();
        double rc = tapChangerData.rc();
        double bc = tapChangerData.bc();
        double gc = tapChangerData.gc();

        x2 = CgmesTapChanger.applyCorrection(x2, xc);
        r2 = CgmesTapChanger.applyCorrection(r2, rc);
        b2 = CgmesTapChanger.applyCorrection(b2, bc);
        g2 = CgmesTapChanger.applyCorrection(g2, gc);
    }

    private void xfmr3ParametersCorrectionEnd3(CgmesTapChangerStatus tapChangerData) {
        double xc = tapChangerData.xc();
        double rc = tapChangerData.rc();
        double bc = tapChangerData.bc();
        double gc = tapChangerData.gc();

        x3 = CgmesTapChanger.applyCorrection(x3, xc);
        r3 = CgmesTapChanger.applyCorrection(r3, rc);
        b3 = CgmesTapChanger.applyCorrection(b3, bc);
        g3 = CgmesTapChanger.applyCorrection(g3, gc);
    }

    static class Xfmr3InterpretedTapChangers {
        BranchInterpretedTapChangers end1 = new BranchInterpretedTapChangers();
        BranchInterpretedTapChangers end2 = new BranchInterpretedTapChangers();
        BranchInterpretedTapChangers end3 = new BranchInterpretedTapChangers();
    }

    static class Xfmr3YShuntData {
        YShuntData end1 = new YShuntData();
        YShuntData end2 = new YShuntData();
        YShuntData end3 = new YShuntData();
    }

    static class Xfmr3Ratio0Data {
        Ratio0Data end1 = new Ratio0Data();
        Ratio0Data end2 = new Ratio0Data();
        Ratio0Data end3 = new Ratio0Data();
    }

    static class Xfmr3PhaseAngleClockData {
        PhaseAngleClockData end1 = new PhaseAngleClockData();
        PhaseAngleClockData end2 = new PhaseAngleClockData();
        PhaseAngleClockData end3 = new PhaseAngleClockData();
    }

    static class YShuntData {
        Complex ysh1 = Complex.ZERO;
        Complex ysh2 = Complex.ZERO;
    }

    static class PhaseAngleClockData {
        double angle1;
        double angle2;
    }

    static class Ratio0Data {
        double a02;
        double a01;
    }

    private final CgmesTransformer transformer;

    private double r1;
    private double x1;
    private double b1;
    private double g1;
    private double r2;
    private double x2;
    private double b2;
    private double g2;
    private double r3;
    private double x3;
    private double b3;
    private double g3;

    private final BranchAdmittanceMatrix admittanceMatrixEnd1;
    private final BranchAdmittanceMatrix admittanceMatrixEnd2;
    private final BranchAdmittanceMatrix admittanceMatrixEnd3;
    private final DetectedBranchModel branchModelEnd1;
    private final DetectedBranchModel branchModelEnd2;
    private final DetectedBranchModel branchModelEnd3;
}
