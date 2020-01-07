/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.conversion.validation.conversion;

import com.powsybl.cgmes.conversion.Conversion;
import com.powsybl.cgmes.conversion.Conversion.Xfmr2RatioPhaseInterpretationAlternative;
import com.powsybl.cgmes.conversion.Conversion.Xfmr2ShuntInterpretationAlternative;
import com.powsybl.cgmes.conversion.Conversion.Xfmr2StructuralRatioInterpretationAlternative;
import com.powsybl.cgmes.conversion.Conversion.Xfmr3ShuntInterpretationAlternative;
import com.powsybl.cgmes.conversion.Conversion.Xfmr3StructuralRatioInterpretationAlternative;
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
                config.setXfmr2RatioPhase(Xfmr2RatioPhaseInterpretationAlternative.END1);
                break;
            case END2:
                config.setXfmr2RatioPhase(Xfmr2RatioPhaseInterpretationAlternative.END2);
                break;
            case END1_END2:
                config.setXfmr2RatioPhase(Xfmr2RatioPhaseInterpretationAlternative.END1_END2);
                break;
            case X:
                config.setXfmr2RatioPhase(Xfmr2RatioPhaseInterpretationAlternative.X);
                break;
        }
        config.setXfmr2PhaseNegate(alternative.isXfmr2PtcNegate());

        switch (alternative.getXfmr2YShunt()) {
            case END1:
                config.setXfmr2Shunt(Xfmr2ShuntInterpretationAlternative.END1);
                break;
            case END2:
                config.setXfmr2Shunt(Xfmr2ShuntInterpretationAlternative.END2);
                break;
            case END1_END2:
                config.setXfmr2Shunt(Xfmr2ShuntInterpretationAlternative.END1_END2);
                break;
            case SPLIT:
                config.setXfmr2Shunt(Xfmr2ShuntInterpretationAlternative.SPLIT);
                break;
        }

        switch (alternative.getXfmr2PhaseAngleClock()) {
            case OFF:
                //config.setXfmr2PhaseAngleClockOn(false);
                break;
            case ON:
                //config.setXfmr2PhaseAngleClockOn(true);
                break;
        }

        switch (alternative.getXfmr2Ratio0()) {
            case END1:
                config.setXfmr2StructuralRatio(Xfmr2StructuralRatioInterpretationAlternative.END1);
                break;
            case END2:
                config.setXfmr2StructuralRatio(Xfmr2StructuralRatioInterpretationAlternative.END2);
                break;
            case END1_END2:
                break;
            case X:
                config.setXfmr2StructuralRatio(Xfmr2StructuralRatioInterpretationAlternative.X);
                break;
        }

        config.setXfmr3RatioPhaseNetworkSide(alternative.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseInterpretationAlternative.NETWORK_SIDE);
        config.setXfmr3PhaseNegate(alternative.isXfmr3PtcNegate());

        switch (alternative.getXfmr3YShunt()) {
            case NETWORK_SIDE:
                config.setXfmr3Shunt(Xfmr3ShuntInterpretationAlternative.NETWORK_SIDE);
                break;
            case STAR_BUS_SIDE:
                config.setXfmr3Shunt(Xfmr3ShuntInterpretationAlternative.STAR_BUS_SIDE);
                break;
            case SPLIT:
                config.setXfmr3Shunt(Xfmr3ShuntInterpretationAlternative.SPLIT);
                break;
        }

        switch (alternative.getXfmr3PhaseAngleClock()) {
            case OFF:
                //config.setXfmr3PhaseAngleClockOn(false);
                break;
            case ON:
                //config.setXfmr3PhaseAngleClockOn(true);
                break;
        }

        switch (alternative.getXfmr3Ratio0Side()) {
            case STAR_BUS_SIDE:
                config.setXfmr3StructuralRatio(Xfmr3StructuralRatioInterpretationAlternative.STAR_BUS_SIDE);
                break;
            case NETWORK_SIDE:
                config.setXfmr3StructuralRatio(Xfmr3StructuralRatioInterpretationAlternative.NETWORK_SIDE);
                break;
            case END1:
                config.setXfmr3StructuralRatio(Xfmr3StructuralRatioInterpretationAlternative.END1);
                break;
            case END2:
                config.setXfmr3StructuralRatio(Xfmr3StructuralRatioInterpretationAlternative.END2);
                break;
            case END3:
                config.setXfmr3StructuralRatio(Xfmr3StructuralRatioInterpretationAlternative.END3);
                break;
        }

        return config;
    }

    private final Conversion conversion;
}
