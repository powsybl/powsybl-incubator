/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.*;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
abstract class AbstractOpenBranchAcEquationTerm implements EquationTerm {

    protected final BranchCharacteristics bc;

    private final Equation equation;

    private final List<Variable> variables;

    protected final double r1;
    protected final double r2;
    protected final double b1;
    protected final double b2;
    protected final double g1;
    protected final double g2;
    protected final double y;
    protected final double shunt;
    protected final double ksi;
    protected final double sinKsi;
    protected final double cosKsi;

    protected AbstractOpenBranchAcEquationTerm(BranchCharacteristics bc, EquationType equationType, VariableType variableType,
                                               Bus bus, EquationContext equationContext) {
        this.bc = Objects.requireNonNull(bc);
        Objects.requireNonNull(equationType);
        Objects.requireNonNull(variableType);
        Objects.requireNonNull(bus);
        Objects.requireNonNull(equationContext);
        equation = equationContext.getEquation(bus.getId(), equationType);
        variables = Collections.singletonList(equationContext.getVariable(bus.getId(), variableType));
        r1 = bc.r1();
        r2 = bc.r2();
        b1 = bc.b1();
        b2 = bc.b2();
        g1 = bc.g1();
        g2 = bc.g2();
        y = bc.y();
        ksi = bc.ksi();
        sinKsi = Math.sin(ksi);
        cosKsi = Math.cos(ksi);
        shunt = (g1 + y * sinKsi) * (g1 + y * sinKsi) + (-b1 + y * cosKsi) * (-b1 + y * cosKsi);
    }

    @Override
    public Equation getEquation() {
        return equation;
    }

    @Override
    public List<Variable> getVariables() {
        return variables;
    }

    @Override
    public double rhs(Variable variable) {
        return 0;
    }
}
