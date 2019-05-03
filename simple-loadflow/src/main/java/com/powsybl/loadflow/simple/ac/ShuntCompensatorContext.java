/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.Variable;
import com.powsybl.loadflow.simple.equations.VariableType;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ShuntCompensatorContext {

    private final ShuntCompensator sc;

    private final Variable vVar;

    public ShuntCompensatorContext(ShuntCompensator sc, Bus bus, EquationContext equationContext) {
        this.sc = Objects.requireNonNull(sc);
        Objects.requireNonNull(bus);
        Objects.requireNonNull(equationContext);
        this.vVar = equationContext.getVariable(bus.getId(), VariableType.BUS_V);
    }

    public ShuntCompensator getSc() {
        return sc;
    }

    public Variable getvVar() {
        return vVar;
    }

    public double b() {
        return sc.getCurrentB();
    }

    public double q(double[] x) {
        Objects.requireNonNull(x);
        double v = x[vVar.getColumn()];
        return -sc.getCurrentB() * v * v;
    }
}
