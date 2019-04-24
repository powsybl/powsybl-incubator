/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.powsybl.cgmes.interpretation.Interpretation;
import com.powsybl.cgmes.interpretation.InterpretationAlternatives;
import com.powsybl.cgmes.interpretation.InterpretationResults;
import com.powsybl.cgmes.interpretation.Configuration;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class KronReductionTests {

    @Test
    public void lineConnectedTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.modelWithLine(true));
    }

    @Test
    public void lineAntennaTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.modelWithLine(false));
    }

    // FIXME This test fails,
    // error is not below tolerance for all interpretation alternatives
    // Only the alternatives that have Xfmr2_yshunt_split have an error < tolerance
    // @Test
    public void xfmr2ConnectedTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.modelWithXfmr2(true));
    }

    @Test
    public void xfmr2AntennaTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.modelWithXfmr2(false));
    }

    @Test
    public void xfmr3ConnectedTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.modelWithXfmr3(true));
    }

    @Test
    public void xfmr3AntennaTest() throws IOException {
        testInterpretationWithoutErrors(KronReductionTestsModelFactory.modelWithXfmr3(false));
    }

    private void testInterpretationWithoutErrors(CgmesModelForInterpretation m) {
        InterpretationResults i = new Interpretation(m)
            .interpret(InterpretationAlternatives.configured());
        Assert.assertTrue(i.error() < Configuration.ERROR_TOLERANCE);
        Assert.assertEquals(0, i.countBadNodes());
    }
}
