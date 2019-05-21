/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Equation implements Comparable<Equation> {

    /**
     * Bus or any other equipment id.
     */
    private final int num;

    private final EquationType type;

    private int row = -1;

    /**
     * true if this equation term is par of an equation that is part of a system to solve, false otherwise
     */
    private boolean partOfSystem = true;

    Equation(int num, EquationType type) {
        this.num = num;
        this.type = Objects.requireNonNull(type);
    }

    public int getNum() {
        return num;
    }

    public EquationType getType() {
        return type;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public boolean isPartOfSystem() {
        return partOfSystem;
    }

    public void setPartOfSystem(boolean partOfSystem) {
        this.partOfSystem = partOfSystem;
    }

    void initTarget(NetworkContext network, double[] targets) {
        switch (type) {
            case BUS_P:
                targets[row] = network.getBus(num).getTargetP();
                break;

            case BUS_Q:
                targets[row] = network.getBus(num).getTargetQ();
                break;

            case BUS_V:
                targets[row] = network.getBus(num).getTargetV();
                break;

            case BUS_PHI:
                targets[row] = 0;
                break;

            default:
                throw new IllegalStateException("Unknown state variable type "  + type);
        }
    }

    @Override
    public int hashCode() {
        return num + type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Equation) {
            return compareTo((Equation) obj) == 0;
        }
        return false;
    }

    @Override
    public int compareTo(Equation o) {
        if (o == this) {
            return 0;
        }
        int c = num - o.num;
        if (c == 0) {
            c = type.ordinal() - o.type.ordinal();
        }
        return c;
    }

    @Override
    public String toString() {
        return "Equation(num=" + num + ", type=" + type + ", row=" + row + ")";
    }
}
