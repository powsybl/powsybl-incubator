/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation.model.interpreted;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretationAlternative {

    public enum LineShuntInterpretationAlternative {
        END1, END2, SPLIT
    }

    public enum Xfmr2ShuntInterpretationAlternative {
        END1, END2, SPLIT, END1_END2
    }

    public enum Xfmr2RatioPhaseIntepretationAlternative {
        END1, END2, END1_END2, RTC, X
    }

    public enum Xfmr2PhaseAngleClockAlternative {
        OFF, END1_END2
    }

    public enum Xfmr3StructuralRatioInterpretationAlternative {
        STAR_BUS_SIDE, NETWORK_SIDE, END1, END2, END3
    }

    public enum Xfmr3RatioPhaseInterpretationAlternative {
        STAR_BUS_SIDE, NETWORK_SIDE
    }

    public enum Xfmr3ShuntInterpretationAlternative {
        STAR_BUS_SIDE, NETWORK_SIDE, SPLIT
    }

    public enum Xfmr3PhaseAngleClockAlternative {
        OFF, STAR_BUS_SIDE, NETWORK_SIDE
    }

    public LineShuntInterpretationAlternative getLineBshunt() {
        return lineBshunt;
    }

    public void setLineBshunt(LineShuntInterpretationAlternative lineBshunt) {
        this.lineBshunt = lineBshunt;
    }

    public boolean isLineRatio0() {
        return lineRatio0;
    }

    public void setLineRatio0(boolean lineRatio0) {
        this.lineRatio0 = lineRatio0;
    }

    public Xfmr2RatioPhaseIntepretationAlternative getXfmr2Ratio0() {
        return xfmr2Ratio0;
    }

    public void setXfmr2Ratio0(Xfmr2RatioPhaseIntepretationAlternative xfmr2Ratio0) {
        this.xfmr2Ratio0 = xfmr2Ratio0;
    }

    public Xfmr2RatioPhaseIntepretationAlternative getXfmr2RatioPhase() {
        return xfmr2RatioPhase;
    }

    public void setXfmr2RatioPhase(Xfmr2RatioPhaseIntepretationAlternative xfmr2RatioPhase) {
        this.xfmr2RatioPhase = xfmr2RatioPhase;
    }

    public boolean isXfmr2Ptc2Negate() {
        return xfmr2Ptc2Negate;
    }

    public void setXfmr2Ptc2Negate(boolean xfmr2Ptc2Negate) {
        this.xfmr2Ptc2Negate = xfmr2Ptc2Negate;
    }

    public Xfmr2ShuntInterpretationAlternative getXfmr2YShunt() {
        return xfmr2YShunt;
    }

    public void setXfmr2YShunt(Xfmr2ShuntInterpretationAlternative xfmr2YShunt) {
        this.xfmr2YShunt = xfmr2YShunt;
    }

    public Xfmr2PhaseAngleClockAlternative getXfmr2PhaseAngleClock() {
        return xfmr2PhaseAngleClock;
    }

    public void setXfmr2PhaseAngleClock(Xfmr2PhaseAngleClockAlternative xfmr2PhaseAngleClock) {
        this.xfmr2PhaseAngleClock = xfmr2PhaseAngleClock;
    }

    public boolean isXfmr2Pac2Negate() {
        return xfmr2Pac2Negate;
    }

    public void setXfmr2Pac2Negate(boolean xfmr2Pac2Negate) {
        this.xfmr2Pac2Negate = xfmr2Pac2Negate;
    }

    public Xfmr3StructuralRatioInterpretationAlternative getXfmr3Ratio0Side() {
        return xfmr3Ratio0Side;
    }

    public void setXfmr3Ratio0Side(Xfmr3StructuralRatioInterpretationAlternative xfmr3Ratio0StarBusSide) {
        this.xfmr3Ratio0Side = xfmr3Ratio0StarBusSide;
    }

    public Xfmr3RatioPhaseInterpretationAlternative getXfmr3RatioPhaseStarBusSide() {
        return xfmr3RatioPhaseStarBusSide;
    }

    public void setXfmr3RatioPhaseStarBusSide(Xfmr3RatioPhaseInterpretationAlternative xfmr3RatioStarBusSide) {
        this.xfmr3RatioPhaseStarBusSide = xfmr3RatioStarBusSide;
    }

    public Xfmr3ShuntInterpretationAlternative getXfmr3YShunt() {
        return xfmr3YShunt;
    }

    public void setXfmr3YShunt(Xfmr3ShuntInterpretationAlternative xfmr3YShunt) {
        this.xfmr3YShunt = xfmr3YShunt;
    }

    public Xfmr3PhaseAngleClockAlternative getXfmr3PhaseAngleClock() {
        return xfmr3PhaseAngleClock;
    }

    public void setXfmr3PhaseAngleClock(Xfmr3PhaseAngleClockAlternative xfmr3PhaseAngleClock) {
        this.xfmr3PhaseAngleClock = xfmr3PhaseAngleClock;
    }

    public int length() {
        return toString().length();
    }

    @Override
    public String toString() {
        StringBuilder configuration = new StringBuilder();
        switch (lineBshunt) {
            case END1:
                configuration.append("Line_end1.");
                break;
            case END2:
                configuration.append("Line_end2.");
                break;
            default:
                break;
        }
        if (lineRatio0) {
            configuration.append("Line_ratio0_on.");
        }
        switch (xfmr2Ratio0) {
            case END1:
                configuration.append("Xfmr2_ratio0_end1.");
                break;
            case X:
                configuration.append("Xfmr2_ratio0_x.");
                break;
            case RTC:
                configuration.append("Xfmr2_ratio0_rtc.");
                break;
            default:
                break;
        }
        switch (xfmr2RatioPhase) {
            case END1:
                configuration.append("Xfmr2_ratio_end1.");
                break;
            case END2:
                configuration.append("Xfmr2_ratio_end2.");
                break;
            case X:
                configuration.append("Xfmr2_ratio_x.");
                break;
            default:
                break;
        }
        if (xfmr2Ptc2Negate) {
            configuration.append("Xfmr2_ptc2_tabular_negate_on.");
        }
        switch (xfmr2YShunt) {
            case END2:
                configuration.append("Xfmr2_yshunt_end2.");
                break;
            case END1_END2:
                configuration.append("Xfmr2_yshunt_end1_end2.");
                break;
            case SPLIT:
                configuration.append("Xfmr2_yshunt_split.");
                break;
            default:
                break;
        }
        if (xfmr2PhaseAngleClock == Xfmr2PhaseAngleClockAlternative.END1_END2) {
            configuration.append("Xfmr2_clock_on_end1_end2.");
        }
        if (xfmr2Pac2Negate) {
            configuration.append("Xfmr2_pac2_negate_on.");
        }
        switch (xfmr3Ratio0Side) {
            case END1:
                configuration.append("Xfmr3_ratedU0_end1.");
                break;
            case END2:
                configuration.append("Xfmr3_ratedU0_end2.");
                break;
            case END3:
                configuration.append("Xfmr3_ratedU0_end3.");
                break;
            case NETWORK_SIDE:
                configuration.append("Xfmr3_ratedU0_network_side.");
                break;
        }
        if (xfmr3RatioPhaseStarBusSide == Xfmr3RatioPhaseInterpretationAlternative.STAR_BUS_SIDE) {
            configuration.append("Xfmr3_ratio_star_bus_side.");
        }
        switch (xfmr3YShunt) {
            case STAR_BUS_SIDE:
                configuration.append("Xfmr3_yshunt_star_bus_side.");
                break;
            case SPLIT:
                configuration.append("Xfmr3_yshunt_split.");
                break;
            default:
                break;
        }
        switch (xfmr3PhaseAngleClock) {
            case STAR_BUS_SIDE:
                configuration.append("Xfmr3_clock_on_star_bus_side.");
                break;
            case NETWORK_SIDE:
                configuration.append("Xfmr3_clock_on_network_side.");
                break;
            default:
                break;
        }
        if (configuration.length() == 0) {
            configuration.append("Default.");
        }
        return configuration.toString();

    }

    LineShuntInterpretationAlternative lineBshunt = LineShuntInterpretationAlternative.SPLIT;
    boolean lineRatio0 = true;
    Xfmr2RatioPhaseIntepretationAlternative xfmr2Ratio0 = Xfmr2RatioPhaseIntepretationAlternative.END2;
    Xfmr2RatioPhaseIntepretationAlternative xfmr2RatioPhase = Xfmr2RatioPhaseIntepretationAlternative.END1_END2;
    boolean xfmr2Ptc2Negate = false;
    Xfmr2ShuntInterpretationAlternative xfmr2YShunt = Xfmr2ShuntInterpretationAlternative.END1;
    Xfmr2PhaseAngleClockAlternative xfmr2PhaseAngleClock = Xfmr2PhaseAngleClockAlternative.OFF;
    boolean xfmr2Pac2Negate = false;
    Xfmr3StructuralRatioInterpretationAlternative xfmr3Ratio0Side = Xfmr3StructuralRatioInterpretationAlternative.STAR_BUS_SIDE;
    Xfmr3RatioPhaseInterpretationAlternative xfmr3RatioPhaseStarBusSide = Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE;
    Xfmr3ShuntInterpretationAlternative xfmr3YShunt = Xfmr3ShuntInterpretationAlternative.NETWORK_SIDE;
    Xfmr3PhaseAngleClockAlternative xfmr3PhaseAngleClock = Xfmr3PhaseAngleClockAlternative.OFF;
}
