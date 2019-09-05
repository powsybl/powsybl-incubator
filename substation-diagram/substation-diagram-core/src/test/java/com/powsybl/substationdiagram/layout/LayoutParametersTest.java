/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.layout;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
 */
public class LayoutParametersTest {

    @Test
    public void test() {
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
                .setVerticalSubstationPadding(50)
                .setArrowDistance(20)
                .setDiagramName("diagram-name");
        LayoutParameters layoutParameters2 = new LayoutParameters(layoutParameters);

        assertEquals(layoutParameters.getTranslateX(), layoutParameters2.getTranslateX(), 0);
        assertEquals(layoutParameters.getTranslateY(), layoutParameters2.getTranslateY(), 0);
        assertEquals(layoutParameters.getInitialXBus(), layoutParameters2.getInitialXBus(), 0);
        assertEquals(layoutParameters.getInitialYBus(), layoutParameters2.getInitialYBus(), 0);
        assertEquals(layoutParameters.getVerticalSpaceBus(), layoutParameters2.getVerticalSpaceBus(), 0);
        assertEquals(layoutParameters.getHorizontalBusPadding(), layoutParameters2.getHorizontalBusPadding(), 0);
        assertEquals(layoutParameters.getCellWidth(), layoutParameters2.getCellWidth(), 0);
        assertEquals(layoutParameters.getExternCellHeight(), layoutParameters2.getExternCellHeight(), 0);
        assertEquals(layoutParameters.getInternCellHeight(), layoutParameters2.getInternCellHeight(), 0);
        assertEquals(layoutParameters.getStackHeight(), layoutParameters2.getStackHeight(), 0);
        assertEquals(layoutParameters.isShowGrid(), layoutParameters2.isShowGrid());
        assertEquals(layoutParameters.isShowInternalNodes(), layoutParameters2.isShowInternalNodes());
        assertEquals(layoutParameters.getScaleFactor(), layoutParameters2.getScaleFactor(), 0);
        assertEquals(layoutParameters.getHorizontalSubstationPadding(), layoutParameters2.getHorizontalSubstationPadding(), 0);
        assertEquals(layoutParameters.getVerticalSubstationPadding(), layoutParameters2.getVerticalSubstationPadding(), 0);
        assertEquals(layoutParameters.getArrowDistance(), layoutParameters2.getArrowDistance(), 0);
        assertEquals(layoutParameters.getDiagramName(), layoutParameters2.getDiagramName());

    }
}
