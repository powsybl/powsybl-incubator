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
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformerEnd;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public final class InterpretedBranch {

    private InterpretedBranch() {
    }

    static class ShuntAdmittances {
        Complex ysh1 = Complex.ZERO;
        Complex ysh2 = Complex.ZERO;
    }

    static class Ratios {
        double a1 = 1.0;
        double a2 = 1.0;
    }

    static class TransformerEndParameters {
        double r = 0.0;
        double x = 0.0;
        double g = 0.0;
        double b = 0.0;
    }

    static class CorrectionFactors {
        double r = 0.0;
        double x = 0.0;
        double g = 0.0;
        double b = 0.0;
    }

    static class PhaseAngleClocks {
        double angle1 = 0.0;
        double angle2 = 0.0;
    }

    static TransformerEndParameters getEndParameters(CgmesTransformerEnd transformerEnd) {
        TransformerEndParameters p = new TransformerEndParameters();
        p.r = transformerEnd.r();
        p.x = transformerEnd.x();
        p.b = transformerEnd.b();
        p.g = transformerEnd.g();
        return p;
    }

    static CorrectionFactors correctionFactors(CgmesTapChangerStatus tapChangerData) {
        CorrectionFactors cs = new CorrectionFactors();
        cs.r = CgmesTapChanger.getCorrectionFactor(tapChangerData.rc());
        cs.x = CgmesTapChanger.getCorrectionFactor(tapChangerData.xc());
        cs.g = CgmesTapChanger.getCorrectionFactor(tapChangerData.gc());
        cs.b = CgmesTapChanger.getCorrectionFactor(tapChangerData.bc());
        return cs;
    }

    static InterpretedBranch.Ratios calculateRatios(InterpretedBranch.TapChangers tcs,
            InterpretedBranch.Ratios structuralRatios) {
        InterpretedBranch.Ratios ratios = new InterpretedBranch.Ratios();
        ratios.a1 = tcs.rtc1.ratio * tcs.ptc1.ratio * structuralRatios.a1;
        ratios.a2 = tcs.rtc2.ratio * tcs.ptc2.ratio * structuralRatios.a2;
        return ratios;
    }

    static class TapChanger {
        double ratio = 1.0;
        boolean regulatingControl = false;
        boolean changeable = false;
    }

    static class RatioTapChanger extends TapChanger {
    }

    static class PhaseTapChanger extends TapChanger {
        double angle = 0.0;
    }

    static class TapChangers {
        RatioTapChanger rtc1 = new RatioTapChanger();
        PhaseTapChanger ptc1 = new PhaseTapChanger();
        RatioTapChanger rtc2 = new RatioTapChanger();
        PhaseTapChanger ptc2 = new PhaseTapChanger();

        void setRatio1(double ratio, boolean regulatingControl, boolean changeable) {
            rtc1.ratio = ratio;
            rtc1.regulatingControl = regulatingControl;
            rtc1.changeable = changeable;
        }

        void setPhase1(double ratio, double angle, boolean regulatingControl, boolean changeable) {
            ptc1.ratio = ratio;
            ptc1.angle = angle;
            ptc1.regulatingControl = regulatingControl;
            ptc1.changeable = changeable;
        }

        void setRatio2(double ratio, boolean regulatingControl, boolean changeable) {
            rtc2.ratio = ratio;
            rtc2.regulatingControl = regulatingControl;
            rtc2.changeable = changeable;
        }

        void setPhase2(double ratio, double angle, boolean regulatingControl, boolean changeable) {
            ptc2.ratio = ratio;
            ptc2.angle = angle;
            ptc2.regulatingControl = regulatingControl;
            ptc2.changeable = changeable;
        }

        void addPhaseAngleClocks(InterpretedBranch.PhaseAngleClocks pacs) {
            ptc1.angle += pacs.angle1;
            ptc2.angle += pacs.angle2;
        }
    }
}
