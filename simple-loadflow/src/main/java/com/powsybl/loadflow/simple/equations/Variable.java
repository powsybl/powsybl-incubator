/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.simple.network.NetworkContext;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Variable implements Comparable<Variable> {

    /**
     * Bus or any other equipment id.
     */
    private final String id;

    private final VariableType type;

    private int column = -1;

    Variable(String id, VariableType type) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
    }

    public String getId() {
        return id;
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

    void initState(LoadFlowParameters.VoltageInitMode mode, NetworkContext networkContext, double[] x) {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(networkContext);
        Objects.requireNonNull(x);
        switch (type) {
            case BUS_V:
                switch (mode) {
                    case UNIFORM_VALUES:
                        x[column] = networkContext.getBus(id).getVoltageLevel().getNominalV();
                        break;
                    case PREVIOUS_VALUES:
                        x[column] = networkContext.getBus(id).getV();
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported voltage init mode: " + mode);
                }
                break;

            case BUS_PHI:
                switch (mode) {
                    case UNIFORM_VALUES:
                        x[column] = 0;
                        break;
                    case PREVIOUS_VALUES:
                        x[column] = networkContext.getBus(id).getAngle();
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported voltage init mode: " + mode);
                }
                break;

            default:
                throw new IllegalStateException("Unknown variable type "  + type);
        }
    }

    void updateState(NetworkContext networkContext, double[] x) {
        Objects.requireNonNull(networkContext);
        Objects.requireNonNull(x);
        switch (type) {
            case BUS_V:
                networkContext.getBus(id).setV(x[column]);
                break;

            case BUS_PHI:
                networkContext.getBus(id).setAngle(Math.toDegrees(x[column]));
                break;

            default:
                throw new IllegalStateException("Unknown variable type "  + type);
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode() + type.hashCode() + column;
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
        int c = column - o.column;
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
        return "Variable(id=" + id + ", type=" + type + ", column=" + column + ")";
    }
}
