/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTapChangerStatus;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformer;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr2PhaseAngleClockAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretedBranch.CorrectionFactors;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretedBranch.TransformerEndParameters;
import com.powsybl.commons.PowsyblException;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretedTransformer2 {

    public InterpretedTransformer2(CgmesTransformer transformer, InterpretationAlternative alternative) {
        TransformerEndParameters end1 = InterpretedBranch.getEndParameters(transformer.end1());
        TransformerEndParameters end2 = InterpretedBranch.getEndParameters(transformer.end2());
        // Interpret the tap changers AND modify, as a side effect, the
        // transformer parameters
        TransformerParameters transformerParameters = new TransformerParameters();
        InterpretedBranch.TapChangers tcs = interpretTapChangerStatus(transformer, end1, end2, alternative,
            transformerParameters);
        /*if (transformer.id().equals("_475ae609-3691-4af0-a4af-2d34029312df")) {
            LOG.info("g1 {}", end1.g);
            LOG.info("b1 {}", end1.b);
            LOG.info("g2 {}", end2.g);
            LOG.info("b2 {}", end2.b);
        }*/
        InterpretedBranch.ShuntAdmittances ysh = interpretAsShuntAdmittances(transformerParameters, alternative);
        InterpretedBranch.PhaseAngleClocks pacs = interpretPhaseAngleClock(transformer, alternative);
        tcs.addPhaseAngleClocks(pacs);

        branchModel = new DetectedBranchModel(ysh.ysh1, ysh.ysh2,
            tcs.rtc1, tcs.ptc1,
            tcs.rtc2, tcs.ptc2);

        InterpretedBranch.Ratios structuralRatios = interpretStructuralRatio(transformer, alternative);
        InterpretedBranch.Ratios ratios = InterpretedBranch.calculateRatios(tcs, structuralRatios);

        admittanceMatrix = new BranchAdmittanceMatrix(
            transformerParameters.r, transformerParameters.x,
            ratios.a1, tcs.ptc1.angle, ysh.ysh1,
            ratios.a2, tcs.ptc2.angle, ysh.ysh2);
        /*if (transformer.id().equals("_f9aec7ee-396b-4401-aebf-31644eb4b06d")) {
            LOG.info("a1 {} angle1 {}", ratios.a1, Math.toRadians(tcs.ptc1.angle));
            LOG.info("a2 {} angle2 {}", ratios.a2, Math.toRadians(tcs.ptc2.angle));
            LOG.info("rateU1 {} rateU2 {}", transformer.end1().ratedU(), transformer.end2().ratedU());
            LOG.info("struct a1 {} a2 {}", structuralRatios.a1, structuralRatios.a2);
            LOG.info("y11 {}", admittanceMatrix.y11());
            LOG.info("y12 {}", admittanceMatrix.y12());
            LOG.info("y21 {}", admittanceMatrix.y21());
            LOG.info("y22 {}", admittanceMatrix.y22());
        }*/
    }

    private InterpretedBranch.TapChangers interpretTapChangerStatus(CgmesTransformer transformer,
        TransformerEndParameters end1Parameters, TransformerEndParameters end2Parameters,
        InterpretationAlternative alternative,
        TransformerParameters transformerParameters) {

        InterpretedBranch.TapChangers tcs = new InterpretedBranch.TapChangers();

        CgmesTapChangerStatus rtc1Status = transformer.end1().rtc().status();
        CorrectionFactors rtc1EndCorrectionFactors = InterpretedBranch.correctionFactors(rtc1Status);

        CgmesTapChangerStatus ptc1Status = transformer.end1().ptc().status();
        double newX1 = transformer.end1().ptc().overrideX(end1Parameters.x, ptc1Status.angle());
        CorrectionFactors ptc1EndCorrectionFactors = InterpretedBranch.correctionFactors(ptc1Status);

        CgmesTapChangerStatus rtc2Status = transformer.end2().rtc().status();
        CorrectionFactors rtc2EndCorrectionFactors = InterpretedBranch.correctionFactors(rtc2Status);

        CgmesTapChangerStatus ptc2Status = transformer.end2().ptc().status();
        double newX2 = transformer.end2().ptc().overrideX(end2Parameters.x, ptc2Status.angle());
        CorrectionFactors ptc2EndCorrectionFactors = InterpretedBranch.correctionFactors(ptc2Status);

        applyCorrectionFactors(end1Parameters, end2Parameters,
            rtc1EndCorrectionFactors, ptc1EndCorrectionFactors, newX1,
            rtc2EndCorrectionFactors, ptc2EndCorrectionFactors, newX2,
            transformerParameters);

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
                tcs.setRatio1(rtc12Ratio, rtc12RegulatingControl, rtc12Changeable);
                tcs.setPhase1(ptc12Ratio, ptc12Angle, ptc12RegulatingControl, ptc12Changeable);
                break;
            case END2:
                tcs.setRatio2(rtc12Ratio, rtc12RegulatingControl, rtc12Changeable);
                tcs.setPhase2(ptc12Ratio, ptc12Angle, ptc12RegulatingControl, ptc12Changeable);
                break;
            case END1_END2:
                tcs.setRatio1(rtc1Status.ratio(), transformer.end1().rtc().isRegulatingControlEnabled(),
                    transformer.end1().rtc().hasDifferentRatios());
                tcs.setPhase1(ptc1Status.ratio(), ptc1Status.angle(),
                    transformer.end1().ptc().isRegulatingControlEnabled(),
                    transformer.end1().ptc().hasDifferentRatiosAngles());

                tcs.setRatio2(rtc2Status.ratio(), transformer.end2().rtc().isRegulatingControlEnabled(),
                    transformer.end2().rtc().hasDifferentRatios());
                tcs.setPhase2(ptc2Status.ratio(), ptc2Status.angle(),
                    transformer.end2().ptc().isRegulatingControlEnabled(),
                    transformer.end2().ptc().hasDifferentRatiosAngles());

                break;
            case X:
                if (end1Parameters.x == 0.0) {
                    tcs.setRatio1(rtc12Ratio, rtc12RegulatingControl, rtc12Changeable);
                    tcs.setPhase1(ptc12Ratio, ptc12Angle, ptc12RegulatingControl, ptc12Changeable);
                } else {
                    tcs.setRatio2(rtc12Ratio, rtc12RegulatingControl, rtc12Changeable);
                    tcs.setPhase2(ptc12Ratio, ptc12Angle, ptc12RegulatingControl, ptc12Changeable);
                }
                break;
            case RTC:
                // does not apply
                break;
        }

        return tcs;
    }

    private InterpretedBranch.ShuntAdmittances interpretAsShuntAdmittances(TransformerParameters parameters,
        InterpretationAlternative alternative) {
        InterpretedBranch.ShuntAdmittances ysh = new InterpretedBranch.ShuntAdmittances();
        switch (alternative.getXfmr2YShunt()) {
            case END1:
                ysh.ysh1 = new Complex(parameters.g1 + parameters.g2, parameters.b1 + parameters.b2);
                break;
            case END2:
                ysh.ysh2 = new Complex(parameters.g1 + parameters.g2, parameters.b1 + parameters.b2);
                break;
            case END1_END2:
                ysh.ysh1 = new Complex(parameters.g1, parameters.b1);
                ysh.ysh2 = new Complex(parameters.g2, parameters.b2);
                break;
            case SPLIT:
                ysh.ysh1 = new Complex(
                    (parameters.g1 + parameters.g2) * 0.5,
                    (parameters.b1 + parameters.b2) * 0.5);
                ysh.ysh2 = new Complex(
                    (parameters.g1 + parameters.g2) * 0.5,
                    (parameters.b1 + parameters.b2) * 0.5);
                break;
        }
        return ysh;
    }

    private InterpretedBranch.PhaseAngleClocks interpretPhaseAngleClock(CgmesTransformer transformer,
        InterpretationAlternative alternative) {

        InterpretedBranch.PhaseAngleClocks pacs = new InterpretedBranch.PhaseAngleClocks();
        if (alternative.getXfmr2PhaseAngleClock() == Xfmr2PhaseAngleClockAlternative.END1_END2) {
            pacs.angle1 += transformer.end1().phaseAngleClockDegrees();
            double angle2 = transformer.end2().phaseAngleClockDegrees();
            if (alternative.isXfmr2Pac2Negate()) {
                angle2 = -angle2;
            }
            pacs.angle2 = angle2;
        }
        return pacs;
    }

    private InterpretedBranch.Ratios interpretStructuralRatio(
        CgmesTransformer transformer,
        InterpretationAlternative alternative) {

        int rtcEnd = transformer.end1().rtc().hasStepVoltageIncrement() ? 1 : 2;
        double ratedU1 = transformer.end1().ratedU();
        double ratedU2 = transformer.end2().ratedU();
        InterpretedBranch.Ratios structuralRatios = new InterpretedBranch.Ratios();
        switch (alternative.getXfmr2Ratio0()) {
            case END1:
                structuralRatios.a1 = ratedU1 / ratedU2;
                break;
            case END2:
                structuralRatios.a2 = ratedU2 / ratedU1;
                break;
            case RTC:
                if (rtcEnd == 1) {
                    structuralRatios.a1 = ratedU1 / ratedU2;
                } else {
                    structuralRatios.a2 = ratedU2 / ratedU1;
                }
                break;
            case X:
                if (transformer.end1().x() == 0.0) {
                    // Structural ratio in the side that has x == 0
                    structuralRatios.a1 = ratedU1 / ratedU2;
                } else {
                    structuralRatios.a2 = ratedU2 / ratedU1;
                }
                break;
            default:
                throw new PowsyblException("Unsupported alternative " + alternative.getXfmr2Ratio0());
        }

        return structuralRatios;
    }

    // TODO warnings under incoherent cases
    private void applyCorrectionFactors(
        TransformerEndParameters initialEnd1Parameters,
        TransformerEndParameters initialEnd2Parameters,
        CorrectionFactors rtc1EndCorrectionFactors, CorrectionFactors ptc1EndCorrectionFactors, double newX1,
        CorrectionFactors rtc2EndCorrectionFactors, CorrectionFactors ptc2EndCorrectionFactors, double newX2,
        TransformerParameters p1) {

        if (hasInitialParameters(initialEnd1Parameters) && hasInitialParameters(initialEnd2Parameters)) {
            p1.r = initialEnd1Parameters.r * rtc1EndCorrectionFactors.r * ptc1EndCorrectionFactors.r;
            p1.r += initialEnd2Parameters.r * rtc2EndCorrectionFactors.r * ptc2EndCorrectionFactors.r;
            p1.x = newX1 * rtc1EndCorrectionFactors.x * ptc1EndCorrectionFactors.x;
            p1.x += newX2 * rtc2EndCorrectionFactors.x * ptc2EndCorrectionFactors.x;
            p1.g1 = initialEnd1Parameters.g * rtc1EndCorrectionFactors.g * ptc1EndCorrectionFactors.g;
            p1.g2 = initialEnd2Parameters.g * rtc2EndCorrectionFactors.g * ptc2EndCorrectionFactors.g;
            p1.b1 = initialEnd1Parameters.b * rtc1EndCorrectionFactors.b * ptc1EndCorrectionFactors.b;
            p1.b2 = initialEnd2Parameters.b * rtc2EndCorrectionFactors.b * ptc2EndCorrectionFactors.b;
        } else if (hasInitialParameters(initialEnd1Parameters)) {
            p1.r = initialEnd1Parameters.r * rtc1EndCorrectionFactors.r * ptc1EndCorrectionFactors.r;
            p1.r *= rtc2EndCorrectionFactors.r * ptc2EndCorrectionFactors.r;
            if (newX1 != 0.0 && newX1 != initialEnd1Parameters.x) {
                p1.x = newX1;
            } else if (newX2 != 0.0 && newX2 != initialEnd1Parameters.x) {
                p1.x = newX2;
            } else {
                p1.x = initialEnd1Parameters.x;
            }
            p1.x *= rtc1EndCorrectionFactors.x * ptc1EndCorrectionFactors.x;
            p1.x *= rtc2EndCorrectionFactors.x * ptc2EndCorrectionFactors.x;
            p1.g1 = initialEnd1Parameters.g * rtc1EndCorrectionFactors.g * ptc1EndCorrectionFactors.g;
            p1.g1 *= rtc2EndCorrectionFactors.g * ptc2EndCorrectionFactors.g;
            p1.b1 = initialEnd1Parameters.b * rtc1EndCorrectionFactors.b * ptc1EndCorrectionFactors.b;
            p1.b1 *= rtc2EndCorrectionFactors.b * ptc2EndCorrectionFactors.b;
        } else {
            p1.r = initialEnd2Parameters.r * rtc1EndCorrectionFactors.r * ptc1EndCorrectionFactors.r;
            p1.r *= rtc2EndCorrectionFactors.r * ptc2EndCorrectionFactors.r;
            if (newX1 != 0.0 && newX1 != initialEnd2Parameters.x) {
                p1.x = newX1;
            } else if (newX2 != 0.0 && newX2 != initialEnd2Parameters.x) {
                p1.x = newX2;
            } else {
                p1.x = initialEnd2Parameters.x;
            }
            p1.x *= rtc1EndCorrectionFactors.x * ptc1EndCorrectionFactors.x;
            p1.x *= rtc2EndCorrectionFactors.x * ptc2EndCorrectionFactors.x;
            p1.g2 = initialEnd2Parameters.g * rtc1EndCorrectionFactors.g * ptc1EndCorrectionFactors.g;
            p1.g2 *= rtc2EndCorrectionFactors.g * ptc2EndCorrectionFactors.g;
            p1.b2 = initialEnd2Parameters.b * rtc1EndCorrectionFactors.b * ptc1EndCorrectionFactors.b;
            p1.b2 *= rtc2EndCorrectionFactors.b * ptc2EndCorrectionFactors.b;
        }
    }

    private boolean hasInitialParameters(TransformerEndParameters initialEndParameters) {
        if (initialEndParameters.x != 0 || initialEndParameters.r != 0.0 || initialEndParameters.g != 0.0
            || initialEndParameters.b != 0.0) {
            return true;
        }
        return false;
    }

    static class TransformerParameters {
        double r = 0.0;
        double x = 0.0;
        double g1 = 0.0;
        double b1 = 0.0;
        double g2 = 0.0;
        double b2 = 0.0;
    }

    public DetectedBranchModel getBranchModel() {
        return branchModel;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrix() {
        return admittanceMatrix;
    }

    private final BranchAdmittanceMatrix admittanceMatrix;
    private final DetectedBranchModel branchModel;
    private static final Logger LOG = LoggerFactory.getLogger(InterpretedTransformer2.class);
}
