/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram;

import com.powsybl.iidm.network.*;
import com.powsybl.substationdiagram.layout.BlockOrganizer;
import com.powsybl.substationdiagram.layout.ImplicitCellDetector;
import com.powsybl.substationdiagram.layout.LayoutParameters;
import com.powsybl.substationdiagram.layout.PositionVoltageLevelLayout;
import com.powsybl.substationdiagram.model.Graph;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BusBreakerTestCase1 extends AbstractTestCase {

    private VoltageLevel vl;

    @Before
    public void setUp() {
        Network network = NetworkFactory.create("busBreakerTestCase1", "test");
        Substation s = network.newSubstation()
                .setId("s")
                .setCountry(Country.FR)
                .add();
        vl = s.newVoltageLevel()
                .setId("vl")
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setNominalV(400)
                .add();
        VoltageLevel.BusBreakerView view = vl.getBusBreakerView();
        view.newBus()
                .setId("b1")
                .add();
        view.newBus()
                .setId("b2")
                .add();
        Load l = vl.newLoad()
                .setId("l")
                .setConnectableBus("b1")
                .setBus("b1")
                .setP0(10)
                .setQ0(10)
                .add();
    }

    @Test
    public void test() {
        // build graph
        Graph g = Graph.create(vl);

        // detect cells
        new ImplicitCellDetector().detectCells(g);

        // build blocks
        assertTrue(new BlockOrganizer().organize(g));

        // calculate coordinates
        LayoutParameters layoutParameters = new LayoutParameters(20, 50, 0, 260,
                25, 20,
                50, 250, 40,
                30, true, true, 1);

        new PositionVoltageLevelLayout(g).run(layoutParameters);

        // write SVG and compare to reference
        compareSvg(g, layoutParameters, "/BusBreakerTestCase1.svg");
    }
}
