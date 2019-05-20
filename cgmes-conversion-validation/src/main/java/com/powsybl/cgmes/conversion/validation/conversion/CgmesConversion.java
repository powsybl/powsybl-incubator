package com.powsybl.cgmes.conversion.validation.conversion;

import com.powsybl.cgmes.conversion.Conversion;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr2PhaseAngleClockAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr3RatioPhaseInterpretationAlternative;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.iidm.network.Network;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class CgmesConversion {

    public CgmesConversion(CgmesModel model, InterpretationAlternative alternative) {
        Conversion.Config config = configureConversion(alternative);
        conversion = new Conversion(model, config);
    }

    public Network convert() {
        return conversion.convert();
    }

    private Conversion.Config configureConversion(InterpretationAlternative alternative) {
        Conversion.Config config = new Conversion.Config();
        config.allowUnsupportedTapChangers();

        switch (alternative.getXfmr2RatioPhase()) {
            case END1:
                config.setXfmr2RatioPhaseEnd1(true);
                break;
            case END2:
                config.setXfmr2RatioPhaseEnd2(true);
                break;
            case END1_END2:
                config.setXfmr2RatioPhaseEnd1End2(true);
                break;
            case RTC:
                config.setXfmr2RatioPhaseRtc(true);
                break;
            case X:
                break;
        }

        if (alternative.isXfmr2Ptc2Negate()) {
            config.setXfmr2Phase2Negate(true);
        }

        switch (alternative.getXfmr2YShunt()) {
            case END1:
                config.setXfmr2ShuntEnd1(true);
                break;
            case END2:
                config.setXfmr2ShuntEnd2(true);
                break;
            case END1_END2:
                config.setXfmr2ShuntEnd1End2(true);
                break;
            case SPLIT:
                config.setXfmr2ShuntSplit(true);
                break;
        }

        if (alternative.getXfmr2PhaseAngleClock() == Xfmr2PhaseAngleClockAlternative.END1_END2) {
            config.setXfmr2PhaseAngleClockEnd1End2(true);
        }

        if (alternative.isXfmr2Pac2Negate()) {
            config.setXfmr2PhaseAngleClock2Negate(true);
        }

        switch (alternative.getXfmr2Ratio0()) {
            case END1:
                config.setXfmr2Ratio0End1(true);
                break;
            case END2:
                config.setXfmr2Ratio0End2(true);
                break;
            case END1_END2:
                break;
            case RTC:
                config.setXfmr2Ratio0Rtc(true);
                break;
            case X:
                config.setXfmr2Ratio0X(true);
                break;
        }

        config.setXfmr3RatioPhaseNetworkSide(alternative.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE);

        switch (alternative.getXfmr3YShunt()) {
            case NETWORK_SIDE:
                config.setXfmr3ShuntNetworkSide(true);
                break;
            case STAR_BUS_SIDE:
                config.setXfmr3ShuntStarBusSide(true);
                break;
            case SPLIT:
                config.setXfmr3ShuntSplit(true);
                break;
        }

        switch (alternative.getXfmr3PhaseAngleClock()) {
            case OFF:
                break;
            case NETWORK_SIDE:
                config.setXfmr3PhaseAngleClockNetworkSide(true);
                break;
            case STAR_BUS_SIDE:
                config.setXfmr3PhaseAngleClockStarBusSide(true);
                break;
        }

        switch (alternative.getXfmr3Ratio0Side()) {
            case STAR_BUS_SIDE:
                config.setXfmr3Ratio0StarBusSide(true);
                break;
            case NETWORK_SIDE:
                config.setXfmr3Ratio0NetworkSide(true);
                break;
            case END1:
                config.setXfmr3Ratio0End1(true);
                break;
            case END2:
                config.setXfmr3Ratio0End2(true);
                break;
            case END3:
                config.setXfmr3Ratio0End3(true);
                break;
        }

        return config;
    }

    private final Conversion conversion;
}
