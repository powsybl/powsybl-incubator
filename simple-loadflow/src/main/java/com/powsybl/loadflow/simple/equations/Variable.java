/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.loadflow.LoadFlowParameters.VoltageInitMode;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Variable implements Comparable<Variable> {

    /**
     * Bus or any other equipment num.
     */
    private final int num;

    private final VariableType type;

    private int column = -1;

    Variable(int num, VariableType type) {
        this.num = num;
        this.type = Objects.requireNonNull(type);
    }

    public int getNum() {
        return num;
    }

    public VariableType getType() {
        return type;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    void initState(VoltageInitMode mode, NetworkContext networkContext, double[] x) {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(networkContext);
        Objects.requireNonNull(x);
        if (type == VariableType.BUS_V && mode == VoltageInitMode.UNIFORM_VALUES) {
            x[column] = 1;
        } else if (type == VariableType.BUS_V && mode == VoltageInitMode.PREVIOUS_VALUES) {
            x[column] = networkContext.getBus(num).getV();
        } else if (type == VariableType.BUS_PHI && mode == VoltageInitMode.UNIFORM_VALUES) {
            x[column] = 0;
        } else if (type == VariableType.BUS_PHI && mode == VoltageInitMode.PREVIOUS_VALUES) {
            x[column] = Math.toRadians(networkContext.getBus(num).getAngle());
        } else {
            throw new UnsupportedOperationException("Incorrect state variable initialization: type=" + type + " and mode=" + mode);
        }
    }

    void updateState(NetworkContext networkContext, double[] x) {
        Objects.requireNonNull(networkContext);
        Objects.requireNonNull(x);
        switch (type) {
            case BUS_V:
                networkContext.getBus(num).setV(x[column]);
                break;

            case BUS_PHI:
                networkContext.getBus(num).setAngle(Math.toDegrees(x[column]));
                break;

            default:
                throw new IllegalStateException("Unknown variable type "  + type);
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
        if (obj instanceof Variable) {
            return compareTo((Variable) obj) == 0;
        }
        return false;
    }

    @Override
    public int compareTo(Variable o) {
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
        return "Variable(num=" + num + ", type=" + type + ", column=" + column + ")";
    }
}
