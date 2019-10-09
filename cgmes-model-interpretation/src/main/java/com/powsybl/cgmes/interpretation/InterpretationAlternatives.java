/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.interpretation;

import java.util.ArrayList;
import java.util.List;

import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr2PhaseAngleClockAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr2RatioPhaseIntepretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr2ShuntInterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr3PhaseAngleClockAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr3ShuntInterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr3StructuralRatioInterpretationAlternative;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public final class InterpretationAlternatives {

    private InterpretationAlternatives() {
    }

    public static List<InterpretationAlternative> configured() {
        List<InterpretationAlternative> alternatives = new ArrayList<>();

        InterpretationAlternative a = new InterpretationAlternative();
        alternatives.add(a);
        if (Configuration.ONLY_DEFAULT_CONFIGURATION) {
            return alternatives;
        }

        a = new InterpretationAlternative();
        a.setLineRatio0(true);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2Ratio0(Xfmr2RatioPhaseIntepretationAlternative.END2);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2Ratio0(Xfmr2RatioPhaseIntepretationAlternative.END2);
        a.setXfmr2YShunt(Xfmr2ShuntInterpretationAlternative.SPLIT);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2YShunt(Xfmr2ShuntInterpretationAlternative.SPLIT);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2YShunt(Xfmr2ShuntInterpretationAlternative.SPLIT);
        a.setXfmr2Ratio0(Xfmr2RatioPhaseIntepretationAlternative.END1);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2Ratio0(Xfmr2RatioPhaseIntepretationAlternative.END1);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2Ratio0(Xfmr2RatioPhaseIntepretationAlternative.RTC);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2YShunt(Xfmr2ShuntInterpretationAlternative.SPLIT);
        a.setXfmr3YShunt(Xfmr3ShuntInterpretationAlternative.SPLIT);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2YShunt(Xfmr2ShuntInterpretationAlternative.SPLIT);
        a.setXfmr3YShunt(Xfmr3ShuntInterpretationAlternative.SPLIT);
        a.setLineRatio0(true);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2PhaseAngleClock(Xfmr2PhaseAngleClockAlternative.END1_END2);
        a.setXfmr3PhaseAngleClock(Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE);
        a.setXfmr2Pac2Negate(true);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2YShunt(Xfmr2ShuntInterpretationAlternative.SPLIT);
        a.setXfmr3YShunt(Xfmr3ShuntInterpretationAlternative.SPLIT);
        a.setXfmr2PhaseAngleClock(Xfmr2PhaseAngleClockAlternative.END1_END2);
        a.setXfmr3PhaseAngleClock(Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE);
        a.setXfmr2Pac2Negate(true);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr2YShunt(Xfmr2ShuntInterpretationAlternative.SPLIT);
        a.setXfmr3YShunt(Xfmr3ShuntInterpretationAlternative.SPLIT);
        a.setXfmr2PhaseAngleClock(Xfmr2PhaseAngleClockAlternative.END1_END2);
        a.setXfmr3PhaseAngleClock(Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE);
        a.setXfmr2Pac2Negate(true);
        a.setLineRatio0(true);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr3Ratio0Side(Xfmr3StructuralRatioInterpretationAlternative.NETWORK_SIDE);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr3Ratio0Side(Xfmr3StructuralRatioInterpretationAlternative.END1);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr3Ratio0Side(Xfmr3StructuralRatioInterpretationAlternative.END2);
        alternatives.add(a);

        a = new InterpretationAlternative();
        a.setXfmr3Ratio0Side(Xfmr3StructuralRatioInterpretationAlternative.END3);
        alternatives.add(a);

        return alternatives;
    }
}
