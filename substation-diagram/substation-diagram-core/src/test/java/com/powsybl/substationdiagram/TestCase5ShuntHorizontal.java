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
import com.powsybl.substationdiagram.model.*;
import com.rte_france.powsybl.iidm.network.extensions.cvg.BusbarSectionPosition;
import com.rte_france.powsybl.iidm.network.extensions.cvg.ConnectablePosition;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * <pre>
 *
 *       la     lb
 *       |      |
 *      nsa-bs-nsb
 *       |      |
 *       ba     bb
 *       |      |
 * bbs---da-----db---
 *
 * </pre>
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TestCase5ShuntHorizontal extends AbstractTestCase {

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
    }

    @Test
    public void test() throws IOException {
        // build graph
        Graph g = Graph.create(vl);

        // assert graph structure
        assertEquals(10, g.getNodes().size());

        // detect cells
        new ImplicitCellDetector().detectCells(g);

        // assert cells
        assertEquals(3, g.getCells().size());
        Iterator<Cell> it = g.getCells().iterator();
        Cell cellb = it.next();
        assertEquals(Cell.CellType.EXTERN, cellb.getType());
        assertEquals(6, cellb.getNodes().size());
        assertEquals("EXTERN[bb, bbs, db, dbFictif, lb, lbFictif]", cellb.getFullId());

        Cell cellShunt = it.next();
        assertEquals(Cell.CellType.SHUNT, cellShunt.getType());
        assertEquals(3, cellShunt.getNodes().size());
        assertEquals("SHUNT[bs, laFictif, lbFictif]", cellShunt.getFullId());

        Cell cella = it.next();
        assertEquals(Cell.CellType.EXTERN, cella.getType());
        assertEquals(6, cella.getNodes().size());
        assertEquals("EXTERN[ba, bbs, da, daFictif, la, laFictif]", cella.getFullId());

        // build blocks
        assertTrue(new BlockOrganizer().organize(g));

        // assert blocks and nodes rotation
        assertEquals(1, cella.getPrimaryBlocksConnectedToBus().size());
        assertNotNull(cella.getRootBlock());
        assertTrue(cella.getRootBlock() instanceof SerialBlock);
        assertEquals(new Position(0, 0, 1, 3, false, Orientation.VERTICAL), cella.getRootBlock().getPosition());

        assertEquals(1, cellb.getPrimaryBlocksConnectedToBus().size());
        assertNotNull(cellb.getRootBlock());
        assertTrue(cellb.getRootBlock() instanceof SerialBlock);
        assertEquals(new Position(1, 0, 1, 3, false, Orientation.VERTICAL), cellb.getRootBlock().getPosition());

        assertEquals(0, cellShunt.getPrimaryBlocksConnectedToBus().size());
        assertNotNull(cellShunt.getRootBlock());
        assertTrue(cellShunt.getRootBlock() instanceof PrimaryBlock);
        assertEquals(new Position(-1, -1, 0, 0, false, Orientation.HORIZONTAL), cellShunt.getRootBlock().getPosition());

        // calculate coordinates
        LayoutParameters layoutParameters = new LayoutParameters(20, 50, 0, 260,
                                                                 25, 20,
                                                                 50, 250, 40,
                                                                 30, true, true);
        new PositionVoltageLevelLayout(g).run(layoutParameters);

        // assert coordinate
        assertEquals(50, g.getNodes().get(7).getX(), 0);
        assertEquals(63.3, g.getNodes().get(7).getY(), 0.1);
        assertTrue(g.getNodes().get(7).isRotated());

        assertEquals(25, g.getNodes().get(8).getX(), 0);
        assertEquals(63.3, g.getNodes().get(8).getY(), 0.1);
        assertFalse(g.getNodes().get(8).isRotated());

        assertEquals(75, g.getNodes().get(9).getX(), 0);
        assertEquals(63.3, g.getNodes().get(9).getY(), 0.1);
        assertFalse(g.getNodes().get(9).isRotated());

        // write SVG and compare to reference
        compareSvg(g, layoutParameters, "/TestCase5ShuntHorizontal.svg");
    }
}
