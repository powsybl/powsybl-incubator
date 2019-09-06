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
        assertEquals(color.getRed(), 255);
        assertEquals(color.getGreen(), 0);
        assertEquals(color.getBlue(), 0);
        double factor = 0.7d;
        List<RGBColor> gradient = color.getColorGradient(3, factor);
        assertEquals(gradient.size(), 3);
        assertEquals(gradient.get(0), color.getBrighter(factor));
        assertEquals(gradient.get(2).toString(), "#CB0000");
    }
}
