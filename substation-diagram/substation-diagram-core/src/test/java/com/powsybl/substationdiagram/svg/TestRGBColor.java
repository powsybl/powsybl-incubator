package com.powsybl.substationdiagram.svg;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.powsybl.substationdiagram.util.RGBColor;

public class TestRGBColor {

    @Test
    public void test() {
        String red = "#FF0000";
        RGBColor color = RGBColor.parse(red);
        assertEquals(red, color.toString());
        assertEquals(new RGBColor(255, 0, 0), color);
        assertEquals(255, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(0, color.getBlue());
        double factor = 0.7d;
        List<RGBColor> gradient = color.getColorGradient(3, factor);
        assertEquals(3, gradient.size());
        assertEquals(color.getBrighter(factor), gradient.get(0));
        assertEquals("#CB0000", gradient.get(2).toString());
    }
}
