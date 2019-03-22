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
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * <pre>
 *              b
 *           /     \
 *          |       |
 * bbs1.1 -d1- ds1 -|-- bbs1.2
 * bbs2.1 ---- ds2 -d2- bbs2.2
 *
 * </pre>
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TestCase6CouplingNonFlatHorizontal extends AbstractTestCase {

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
                .setId("bbs1.1")
                .setNode(0)
                .add();
        bbs11.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs11, 1, 1));

        BusbarSection bbs12 = view.newBusbarSection()
                .setId("bbs1.2")
                .setNode(1)
                .add();
        bbs12.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs12, 1, 2));

        BusbarSection bbs21 = view.newBusbarSection()
                .setId("bbs2.1")
                .setNode(2)
                .add();
        bbs21.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs21, 2, 1));

        BusbarSection bbs22 = view.newBusbarSection()
                .setId("bbs2.2")
                .setNode(3)
                .add();
        bbs22.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs22, 2, 2));

        view.newDisconnector()
                .setId("d1")
                .setNode1(0)
                .setNode2(4)
                .add();

        view.newBreaker()
                .setId("b")
                .setNode1(4)
                .setNode2(5)
                .add();

        view.newDisconnector()
                .setId("d2")
                .setNode1(5)
                .setNode2(3)
                .add();

        view.newDisconnector()
                .setId("ds1")
                .setNode1(0)
                .setNode2(1)
                .add();

        view.newDisconnector()
                .setId("ds2")
                .setNode1(2)
                .setNode2(3)
                .add();
    }

    @Test
    public void test() {
        // build graph
        Graph g = Graph.create(vl);

        // assert graph structure
        assertEquals(11, g.getNodes().size());

        // detect cells
        new ImplicitCellDetector().detectCells(g);

        assertEquals(11, g.getNodes().size());

        // assert cells
        assertEquals(3, g.getCells().size());
        Iterator<Cell> it = g.getCells().iterator();
        Cell cellB = it.next();
        assertEquals("INTERN[FICT_vl_d1Fictif, FICT_vl_d2Fictif, b, bbs1.1, bbs2.2, d1, d2]", cellB.getFullId());
        Cell cellD1 = it.next();
        assertEquals("INTERNBOUND[bbs1.1, bbs1.2, ds1]", cellD1.getFullId());
        Cell cellD2 = it.next();
        assertEquals("INTERNBOUND[bbs2.1, bbs2.2, ds2]", cellD2.getFullId());

        // build blocks
        assertTrue(new BlockOrganizer().organize(g));

        // assert blocks and nodes rotation
        assertEquals(2, cellB.getPrimaryBlocksConnectedToBus().size());
        assertNotNull(cellB.getRootBlock());
        assertTrue(cellB.getRootBlock() instanceof ParallelBlock);
        ParallelBlock bp = (ParallelBlock) cellB.getRootBlock();
        assertEquals(new Position(0, 1, 2, 1, false, Orientation.VERTICAL), bp.getPosition());

        assertTrue(bp.getSubBlocks().get(0) instanceof SerialBlock);

        SerialBlock bc = (SerialBlock) bp.getSubBlocks().get(0);
        assertEquals(new Position(0, 0, 1, 1, false, Orientation.HORIZONTAL), bc.getPosition());

        assertTrue(bc.getLowerBlock() instanceof PrimaryBlock);
        PrimaryBlock bpyl = (PrimaryBlock) bc.getLowerBlock();
        assertEquals(new Position(0, 0, 1, 0, false, Orientation.VERTICAL), bpyl.getPosition());

        assertTrue(bc.getUpperBlock() instanceof PrimaryBlock);
        PrimaryBlock bpyu = (PrimaryBlock) bc.getUpperBlock();
        assertEquals(new Position(0, 0, 1, 1, false, Orientation.HORIZONTAL), bpyu.getPosition());

        PrimaryBlock bpy = (PrimaryBlock) bp.getSubBlocks().get(1);
        assertEquals(new Position(1, 0, 1, 0, true, Orientation.VERTICAL), bpy.getPosition());

        // calculate coordinates
        LayoutParameters layoutParameters = new LayoutParameters(20, 50, 0, 260,
                                                                 25, 20,
                                                                 50, 250, 40,
                                                                 30, true, true, 1, 50, 50);
        new PositionVoltageLevelLayout(g).run(layoutParameters);

        // assert coordinate
        assertEquals(10, g.getNodes().get(0).getX(), 0);
        assertEquals(260, g.getNodes().get(0).getY(), 0);
        assertFalse(g.getNodes().get(0).isRotated());

        assertEquals(60, g.getNodes().get(3).getX(), 0);
        assertEquals(285, g.getNodes().get(3).getY(), 0);
        assertFalse(g.getNodes().get(3).isRotated());

        assertEquals(50, g.getNodes().get(5).getX(), 0);
        assertEquals(220, g.getNodes().get(5).getY(), 0);
        assertTrue(g.getNodes().get(5).isRotated());

        assertEquals(25, g.getNodes().get(9).getX(), 0);
        assertEquals(220, g.getNodes().get(9).getY(), 0);
        assertTrue(g.getNodes().get(9).isRotated());

        assertEquals(75, g.getNodes().get(10).getX(), 0);
        assertEquals(220, g.getNodes().get(10).getY(), 0);
        assertTrue(g.getNodes().get(10).isRotated());

        assertEquals(50, g.getNodes().get(7).getX(), 0);
        assertEquals(260, g.getNodes().get(7).getY(), 0);
        assertTrue(g.getNodes().get(7).isRotated());

        assertEquals(50, g.getNodes().get(8).getX(), 0);
        assertEquals(285, g.getNodes().get(8).getY(), 0);
        assertTrue(g.getNodes().get(8).isRotated());

        // write SVG and compare to reference
        compareSvg(g, layoutParameters, "/TestCase6CouplingNonFlatHorizontal.svg");
    }
}
