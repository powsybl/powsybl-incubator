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
public class BranchInterpretedTapChangers {
    InterpretedRatioTapChanger rtc1 = new InterpretedRatioTapChanger();
    InterpretedPhaseTapChanger ptc1 = new InterpretedPhaseTapChanger();
    InterpretedRatioTapChanger rtc2 = new InterpretedRatioTapChanger();
    InterpretedPhaseTapChanger ptc2 = new InterpretedPhaseTapChanger();

    static class InterpretedTapChanger {
        double ratio = 1.0;
        boolean regulatingControl = false;
        boolean changeable = false;
    }

    static class InterpretedRatioTapChanger extends InterpretedTapChanger {
    }

    static class InterpretedPhaseTapChanger extends InterpretedTapChanger {
        double angle = 0.0;
    }
}