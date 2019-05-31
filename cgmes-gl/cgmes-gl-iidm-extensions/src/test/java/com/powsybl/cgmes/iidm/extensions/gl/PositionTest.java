/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.gl;

import org.junit.Test;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class PositionTest {

    @Test
    public void test() {
        Network network = GLTestUtils.getNetwork();
        Substation substation1 = network.getSubstation("Substation1");
        SubstationPosition<Substation> substationPosition1 = new SubstationPosition<Substation>(substation1,
                                                                                                new PositionPoint(GLTestUtils.SUBSTATION_1_X, GLTestUtils.SUBSTATION_1_Y, 0));
        substation1.addExtension(SubstationPosition.class, substationPosition1);

        Substation substation2 = network.getSubstation("Substation2");
        SubstationPosition<Substation> substationPosition2 = new SubstationPosition<Substation>(substation2,
                                                                                                new PositionPoint(GLTestUtils.SUBSTATION_2_X, GLTestUtils.SUBSTATION_2_Y, 0));
        substation2.addExtension(SubstationPosition.class, substationPosition2);

        Line line = network.getLine("Line");
        LinePosition<Line> linePosition = new LinePosition<>(line);
        linePosition.addPoint(new PositionPoint(GLTestUtils.SUBSTATION_1_X, GLTestUtils.SUBSTATION_1_Y, 1));
        linePosition.addPoint(new PositionPoint(GLTestUtils.SUBSTATION_2_X, GLTestUtils.SUBSTATION_2_Y, 4));
        linePosition.addPoint(new PositionPoint(GLTestUtils.LINE_1_X, GLTestUtils.LINE_1_Y, 2));
        linePosition.addPoint(new PositionPoint(GLTestUtils.LINE_2_X, GLTestUtils.LINE_2_Y, 3));
        line.addExtension(LinePosition.class, linePosition);

        GLTestUtils.checkNetwork(network);
    }

}
