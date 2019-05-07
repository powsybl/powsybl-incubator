/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.powsybl.cgmes.interpretation.Configuration;
import com.powsybl.cgmes.interpretation.Interpretation;
import com.powsybl.cgmes.interpretation.InterpretationResults;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.LineShuntInterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr2ShuntInterpretationAlternative;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative.Xfmr3ShuntInterpretationAlternative;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class KronReductionTests {

    @Test
    public void allConnectedTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.model(false, false, false, false, false));
    }

    @Test
    public void kronAntennaLineFromToTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.model(true, false, false, false, false));
    }

    @Test
    public void kronAntennaLineToFromTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.model(false, true, false, false, false));
    }

    @Test
    public void kronAntennaXfmr2FromToTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.model(false, false, true, false, false));
    }

    @Test
    public void kronAntennaXfmr2ToFromTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.model(false, false, false, true, false));
    }

    @Test
    public void kronAntennaXfmr3Test() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.model(false, false, false, false, true));
    }

    @Test
    public void kronAntennaAllDisconnectedTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.model(true, true, true, true, true));
    }

    private void testInterpretationWithoutErrors(CgmesModelForInterpretation m) {
        List<InterpretationAlternative> alternatives = new ArrayList<>();
        InterpretationAlternative interpretationAlternative = new InterpretationAlternative();
        interpretationAlternative.setLineBshunt(LineShuntInterpretationAlternative.SPLIT);
        interpretationAlternative.setXfmr2YShunt(Xfmr2ShuntInterpretationAlternative.SPLIT);
        interpretationAlternative.setXfmr3YShunt(Xfmr3ShuntInterpretationAlternative.SPLIT);
        alternatives.add(interpretationAlternative);
        InterpretationResults i = new Interpretation(m).interpret(alternatives);
        Assert.assertTrue(i.error() < Configuration.ERROR_TOLERANCE);
        Assert.assertEquals(0, i.countBadNodes());
    }
}
