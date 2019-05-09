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
import com.powsybl.substationdiagram.model.Cell;
import com.powsybl.substationdiagram.model.Graph;
import com.powsybl.substationdiagram.svg.SVGWriter;
import com.rte_france.powsybl.iidm.network.extensions.cvg.BusbarSectionPosition;
import com.rte_france.powsybl.iidm.network.extensions.cvg.ConnectablePosition;
import org.junit.Before;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <pre>
 *
 *       la     lb      lc
 *       |      |       |
 *      nsa-bs-nsb-bs2-nsc
 *       |      |       |
 *       ba     bb      bc
 *       |      |       |
 * bbs---da-----db------dc
 *
 * </pre>
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TestCase5DoubleShunt extends AbstractTestCase {

    @Before
    public void setUp() {
        Network network = NetworkFactory.create("testCase1", "test");
        Substation s = network.newSubstation()
                .setId("s")
                .setCountry(Country.FR)
                .add();
        vl = s.newVoltageLevel()
                .setId("vl")
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .setNominalV(400)
                .add();
        VoltageLevel.NodeBreakerView view = vl.getNodeBreakerView()
                .setNodeCount(10);

        BusbarSection bbs11 = view.newBusbarSection()
                .setId("bbs")
                .setNode(0)
                .add();
        bbs11.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs11, 1, 1));

        Load la = vl.newLoad()
                .setId("la")
                .setNode(2)
                .setP0(10)
                .setQ0(10)
                .add();
        la.addExtension(ConnectablePosition.class, new ConnectablePosition<>(la, new ConnectablePosition
                .Feeder("la", 10, ConnectablePosition.Direction.TOP), null, null, null));

        view.newBreaker()
                .setId("ba")
                .setNode1(2)
                .setNode2(1)
                .add();

        view.newDisconnector()
                .setId("da")
                .setNode1(1)
                .setNode2(0)
                .add();

        Load lb = vl.newLoad()
                .setId("lb")
                .setNode(4)
                .setP0(10)
                .setQ0(10)
                .add();
        lb.addExtension(ConnectablePosition.class, new ConnectablePosition<>(lb, new ConnectablePosition
                .Feeder("lb", 20, ConnectablePosition.Direction.TOP), null, null, null));

        view.newBreaker()
                .setId("bb")
                .setNode1(4)
                .setNode2(3)
                .add();

        view.newDisconnector()
                .setId("db")
                .setNode1(3)
                .setNode2(0)
                .add();

        view.newBreaker()
                .setId("bs")
                .setNode1(2)
                .setNode2(4)
                .add();

        Load lc = vl.newLoad()
                .setId("lc")
                .setNode(6)
                .setP0(10)
                .setQ0(10)
                .add();
        lc.addExtension(ConnectablePosition.class, new ConnectablePosition<>(lc, new ConnectablePosition
                .Feeder("lc", 30, ConnectablePosition.Direction.TOP), null, null, null));

        view.newBreaker()
                .setId("bc")
                .setNode1(6)
                .setNode2(5)
                .add();

        view.newDisconnector()
                .setId("dc")
                .setNode1(5)
                .setNode2(0)
                .add();

        view.newBreaker()
                .setId("bs2")
                .setNode1(4)
                .setNode2(6)
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
        assertEquals(5, g.getCells().size());

        for (Cell cell : g.getCells()) {
            System.out.println(cell  + "\n     " + cell.getRootBlock());
        }

        // calculate coordinates
        LayoutParameters layoutParameters = new LayoutParameters()
                .setTranslateX(20)
                .setTranslateY(50)
                .setInitialXBus(0)
                .setInitialYBus(260)
                .setVerticalSpaceBus(25)
                .setHorizontalBusPadding(20)
                .setCellWidth(50)
                .setExternCellHeight(250)
                .setInternCellHeight(40)
                .setStackHeight(30)
                .setShowGrid(true)
                .setShowInternalNodes(true)
                .setScaleFactor(1)
                .setHorizontalSubstationPadding(50)
                .setVerticalSubstationPadding(50);

        new PositionVoltageLevelLayout(g).run(layoutParameters);

        g.writeJson(Paths.get("/tmp/cells.json"));

        try (FileWriter writer = new FileWriter("/tmp/toto.svg")) {
            new SVGWriter(componentLibrary, layoutParameters)
                    .write(g, styleProvider, writer);
            writer.flush();
            System.out.println(writer.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
