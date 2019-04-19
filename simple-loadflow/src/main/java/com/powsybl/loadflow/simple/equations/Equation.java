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
    private final String id;

    private final EquationType type;

    private int row = -1;

    Equation(String id, EquationType type) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
    }

    public String getId() {
        return id;
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

    void initTarget(NetworkContext network, double[] targets) {
        switch (type) {
            case BUS_P:
                targets[row] = network.getBusP(id);
                break;

            case BUS_Q:
                targets[row] = network.getBusQ(id);
                break;

            case BUS_V:
                targets[row] = network.getBus(id).getGenerators().iterator().next().getTargetV();
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
        return id.hashCode() + type.hashCode() + row;
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
        int c = row - o.row;
        if (c == 0) {
            c = id.compareTo(o.id);
            if (c == 0) {
                c = type.ordinal() - o.type.ordinal();
            }
        }
        return c;
    }

    @Override
    public String toString() {
        return "Equation(id=" + id + ", type=" + type + ", row=" + row + ")";
    }
}
