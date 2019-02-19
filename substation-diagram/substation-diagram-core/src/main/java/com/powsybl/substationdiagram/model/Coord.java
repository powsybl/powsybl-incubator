/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.model;

import java.util.Objects;

/**
 * class use to store relatives coordinates of a nodeBus
 *
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Coord {
    private double x;
    private double y;

    private double xSpan;
    private double ySpan;

    public Coord(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getXSpan() {
        return xSpan;
    }

    public void setXSpan(double xSpan) {
        this.xSpan = xSpan;
    }

    public double getYSpan() {
        return ySpan;
    }

    public void setYSpan(double ySpan) {
        this.ySpan = ySpan;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, xSpan, ySpan);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Coord) {
            Coord other = (Coord) obj;
            return other.x == x
                    && other.y == y
                    && other.xSpan == xSpan
                    && other.ySpan == ySpan;
        }
        return false;
    }
}

