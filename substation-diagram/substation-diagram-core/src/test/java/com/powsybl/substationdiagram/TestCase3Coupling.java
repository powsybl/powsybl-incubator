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

import static org.junit.Assert.*;

/**
 * <pre>
 *     b
 *    / \
 *   |   |
 * -d1---|---- bbs1
 * -----d2---- bbs2
 *
 * </pre>
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TestCase3Coupling extends AbstractTestCase {

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

        BusbarSection bbs1 = view.newBusbarSection()
                .setId("bbs1")
                .setNode(0)
                .add();
        bbs1.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs1, 1, 1));

        view.newDisconnector()
                .setId("d1")
                .setNode1(0)
                .setNode2(1)
                .add();

        view.newBreaker()
                .setId("b")
                .setNode1(1)
                .setNode2(2)
                .add();

        view.newDisconnector()
                .setId("d2")
                .setNode1(2)
                .setNode2(3)
                .add();

        BusbarSection bbs2 = view.newBusbarSection()
                .setId("bbs2")
                .setNode(3)
                .add();
        bbs2.addExtension(BusbarSectionPosition.class, new BusbarSectionPosition(bbs2, 2, 1));

    }

    @Test
    public void test() {
        // build graph
        Graph g = Graph.create(vl);

        // assert graph structure
        assertEquals(7, g.getNodes().size());

        // detect cells
        new ImplicitCellDetector().detectCells(g);

        // assert cells
        assertEquals(1, g.getCells().size());
        Cell cell = g.getCells().iterator().next();
        assertEquals(Cell.CellType.INTERN, cell.getType());
        assertEquals(7, cell.getNodes().size());
        assertEquals(2, cell.getBusbars().size());
        assertEquals("INTERN[1, 2, b, bbs1, bbs2, d1, d2]", cell.getFullId());

        // build blocks
        assertTrue(new BlockOrganizer().organize(g));

        // assert blocks and nodes rotation
        assertEquals(2, cell.getPrimaryBlocksConnectedToBus().size());
        assertNotNull(cell.getRootBlock());
        assertTrue(cell.getRootBlock() instanceof ParallelBlock);
        ParallelBlock bp = (ParallelBlock) cell.getRootBlock();
        assertEquals(new Position(0, 1, 2, 1, false, Orientation.VERTICAL), bp.getPosition());
        assertEquals("bbs2", bp.getStartingNode().getId());
        assertEquals("1", bp.getEndingNode().getId());
        assertEquals(2, bp.getSubBlocks().size());

        assertTrue(bp.getSubBlocks().get(0) instanceof SerialBlock);

        SerialBlock bc = (SerialBlock) bp.getSubBlocks().get(0);
        assertEquals(new Position(0, 0, 1, 1, false, Orientation.HORIZONTAL), bc.getPosition());

        assertTrue(bc.getLowerBlock() instanceof PrimaryBlock);
        PrimaryBlock bpyl = (PrimaryBlock) bc.getLowerBlock();
        assertEquals(Node.NodeType.BUS, bpyl.getStartingNode().getType());
        assertEquals(new Position(0, 0, 1, 0, false, Orientation.VERTICAL), bpyl.getPosition());

        assertTrue(bc.getUpperBlock() instanceof PrimaryBlock);
        PrimaryBlock bpyu = (PrimaryBlock) bc.getUpperBlock();
        assertEquals(Node.NodeType.SWITCH, bpyu.getNodes().get(1).getType());
        assertEquals(new Position(0, 0, 1, 1, false, Orientation.HORIZONTAL), bpyu.getPosition());

        // calculate coordinates
        LayoutParameters layoutParameters = new LayoutParameters(20, 50, 0, 260,
                                                                 25, 20,
                                                                 50, 250, 40,
                                                                 30, true, true);

        new PositionVoltageLevelLayout(g).run(layoutParameters);

        // assert coordinate
        assertEquals(10, g.getNodes().get(0).getX(), 0);
        assertEquals(260, g.getNodes().get(0).getY(), 0);
        assertFalse(g.getNodes().get(0).isRotated());

        assertEquals(10, g.getNodes().get(1).getX(), 0);
        assertEquals(285, g.getNodes().get(1).getY(), 0);
        assertFalse(g.getNodes().get(1).isRotated());

        assertEquals(75, g.getNodes().get(2).getX(), 0);
        assertEquals(220, g.getNodes().get(2).getY(), 0);
        assertTrue(g.getNodes().get(2).isRotated());

        assertEquals(75, g.getNodes().get(3).getX(), 0);
        assertEquals(260, g.getNodes().get(3).getY(), 0);
        assertFalse(g.getNodes().get(3).isRotated());

        assertEquals(25, g.getNodes().get(4).getX(), 0);
        assertEquals(220, g.getNodes().get(4).getY(), 0);
        assertTrue(g.getNodes().get(4).isRotated());

        assertEquals(50, g.getNodes().get(5).getX(), 0);
        assertEquals(220, g.getNodes().get(5).getY(), 0);
        assertTrue(g.getNodes().get(5).isRotated());

        assertEquals(25, g.getNodes().get(6).getX(), 0);
        assertEquals(285, g.getNodes().get(6).getY(), 0);
        assertFalse(g.getNodes().get(6).isRotated());

        // write SVG and compare to reference
        compareSvg(g, layoutParameters, "/TestCase3Coupling.svg");
    }
}
