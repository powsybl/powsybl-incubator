/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.conversion.validation.conversion;

import com.powsybl.cgmes.conversion.Conversion;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
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
        // Configure the tap position to get
        config.setProfileUsedForInitialStateValues("SV");

        switch (alternative.getXfmr2RatioPhase()) {
            case END1:
                config.setXfmr2RatioPhaseEnd1();
                break;
            case END2:
                config.setXfmr2RatioPhaseEnd2();
                break;
            case END1_END2:
                config.setXfmr2RatioPhaseEnd1End2();
                break;
            case X:
                config.setXfmr2RatioPhaseX();
                break;
        }
        config.setXfmr2PhaseNegate(alternative.isXfmr2PtcNegate());

        switch (alternative.getXfmr2YShunt()) {
            case END1:
                config.setXfmr2ShuntEnd1();
                break;
            case END2:
                config.setXfmr2ShuntEnd2();
                break;
            case END1_END2:
                config.setXfmr2ShuntEnd1End2();
                break;
            case SPLIT:
                config.setXfmr2ShuntSplit();
                break;
        }

        switch (alternative.getXfmr2PhaseAngleClock()) {
            case OFF:
                config.setXfmr2PhaseAngleClockOn(false);
                break;
            case ON:
                config.setXfmr2PhaseAngleClockOn(true);
                break;
        }

        switch (alternative.getXfmr2Ratio0()) {
            case END1:
                config.setXfmr2Ratio0End1();
                break;
            case END2:
                config.setXfmr2Ratio0End2();
                break;
            case END1_END2:
                break;
            case X:
                config.setXfmr2Ratio0X();
                break;
        }

        config.setXfmr3RatioPhaseNetworkSide(alternative.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE);
        config.setXfmr3PhaseNegate(alternative.isXfmr3PtcNegate());

        switch (alternative.getXfmr3YShunt()) {
            case NETWORK_SIDE:
                config.setXfmr3ShuntNetworkSide();
                break;
            case STAR_BUS_SIDE:
                config.setXfmr3ShuntStarBusSide();
                break;
            case SPLIT:
                config.setXfmr3ShuntSplit();
                break;
        }

        switch (alternative.getXfmr3PhaseAngleClock()) {
            case OFF:
                config.setXfmr3PhaseAngleClockOn(false);
                break;
            case ON:
                config.setXfmr3PhaseAngleClockOn(true);
                break;
        }

        switch (alternative.getXfmr3Ratio0Side()) {
            case STAR_BUS_SIDE:
                config.setXfmr3Ratio0StarBusSide();
                break;
            case NETWORK_SIDE:
                config.setXfmr3Ratio0NetworkSide();
                break;
            case END1:
                config.setXfmr3Ratio0End1();
                break;
            case END2:
                config.setXfmr3Ratio0End2();
                break;
            case END3:
                config.setXfmr3Ratio0End3();
                break;
        }

        return config;
    }

    private final Conversion conversion;
}
