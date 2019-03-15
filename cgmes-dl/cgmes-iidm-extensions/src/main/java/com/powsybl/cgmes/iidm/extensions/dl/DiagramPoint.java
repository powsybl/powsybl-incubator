/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.iidm.extensions.dl;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 */
public class DiagramPoint implements Comparable<DiagramPoint> {

    final double x;
    final double y;
    final int seq;

    public DiagramPoint(double x, double y, int seq) {
        this.x = x;
        this.y = y;
        this.seq = seq;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getSeq() {
        return seq;
    }

    @Override
    public int compareTo(DiagramPoint point) {
        return this.seq - point.seq;
    }

    @Override
    public String toString() {
        return "[" + String.join(",", Double.toString(x), Double.toString(y), Integer.toString(seq)) + "]";
    }

}
