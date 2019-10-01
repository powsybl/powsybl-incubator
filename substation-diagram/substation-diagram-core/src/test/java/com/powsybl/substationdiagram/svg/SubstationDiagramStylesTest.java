package com.powsybl.substationdiagram.svg;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SubstationDiagramStylesTest {

    @Test
    public void test() {
        String input = "ab_cd.ef gh";
        String escaped = SubstationDiagramStyles.escapeId(input);
        assertEquals(input, SubstationDiagramStyles.unescapeId(escaped));
    }
}
