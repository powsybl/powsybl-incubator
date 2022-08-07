/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.openloadflow.equations.AbstractNamedEquationTerm;
import com.powsybl.openloadflow.equations.Variable;
import com.powsybl.openloadflow.equations.VariableSet;
import com.powsybl.openloadflow.network.ElementType;
import com.powsybl.openloadflow.network.LfBranch;
import com.powsybl.openloadflow.network.LfBus;
import com.powsybl.openloadflow.network.PiModel;

import java.util.List;
import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public abstract class AbstractAdmittanceEquationTerm extends AbstractNamedEquationTerm<VariableType, EquationType> implements LinearEquationTerm {

    private final LfBranch branch;

    protected final Variable<VariableType> v1rVar;

    protected final Variable<VariableType> v1iVar;

    protected final Variable<VariableType> v2rVar;

    protected final Variable<VariableType> v2iVar;

    protected final List<Variable<VariableType>> variables;

    protected double rho;

    protected double zInvSquare;

    protected double r;

    protected double x;

    protected double cosA;

    protected double sinA;

    protected double cos2A;

    protected double sin2A;

    protected double gPi1;

    protected double bPi1;

    protected double gPi2;

    protected double bPi2;

    // zero sequence additional attributes
    //
    // Proposed Transformer model :
    //      Ia       Yg    A'  rho                 B'     Yg        Ib        Zga : grounding impedance on A side (in ohms expressed on A side)
    //   A-->--3*Zga--+    +--(())--+--Zoa--+--Zob--+     +--3*ZGb--<--B      Zoa : leakage impedance of A-winding (in ohms expressed on B side)
    //                Y +                   |           + Y                   Zob : leakage impdedance of B-winding (in ohms expressed on B side)
    //                    + D              Zom        + D                     Zom : magnetizing impedance of the two windings (in ohms expressed on B side)
    //                    |                 |         |                       Zgb : grounding impedance on B side (in ohms expressed on B side)
    //                    |                 |         |                       rho might be a complex value
    //                    |    free fluxes \          |
    //                    |                 |         |
    //                  /////             /////     /////                     A' and B' are connected to Yg, Y or D depending on the winding connection type (Y to ground, Y or Delta)
    //
    protected AbstractAdmittanceEquationTerm(LfBranch branch, LfBus bus1, LfBus bus2, VariableSet<VariableType> variableSet) {
        this.branch = Objects.requireNonNull(branch);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);
        Objects.requireNonNull(variableSet);

        v1rVar = variableSet.getVariable(bus1.getNum(), VariableType.BUS_VR);
        v2rVar = variableSet.getVariable(bus2.getNum(), VariableType.BUS_VR);
        v1iVar = variableSet.getVariable(bus1.getNum(), VariableType.BUS_VI);
        v2iVar = variableSet.getVariable(bus2.getNum(), VariableType.BUS_VI);

        variables = List.of(v1rVar, v2rVar, v1iVar, v2iVar);

        PiModel piModel = branch.getPiModel();
        if (piModel.getX() == 0) {
            throw new IllegalArgumentException("Branch '" + branch.getId() + "' has reactance equal to zero");
        }
        rho = piModel.getR1();
        if (piModel.getZ() == 0) {
            throw new IllegalArgumentException("Branch '" + branch.getId() + "' has Z equal to zero");
        }
        zInvSquare = 1 / (piModel.getZ() * piModel.getZ());
        r = piModel.getR();
        x = piModel.getX();
        double alpha = piModel.getA1();
        cosA = Math.cos(Math.toRadians(alpha));
        sinA = Math.sin(Math.toRadians(alpha));
        cos2A = Math.cos(Math.toRadians(2 * alpha));
        sin2A = Math.sin(Math.toRadians(2 * alpha));

        gPi1 = piModel.getG1();
        bPi1 = piModel.getB1();
        gPi2 = piModel.getG2();
        bPi2 = piModel.getB2();
    }

    @Override
    public List<Variable<VariableType>> getVariables() {
        return variables;
    }

    @Override
    public ElementType getElementType() {
        return ElementType.BRANCH;
    }

    @Override
    public int getElementNum() {
        return branch.getNum();
    }

    @Override
    public double eval() {
        throw new UnsupportedOperationException("Not needed");
    }

    @Override
    public double der(Variable<VariableType> variable) {
        throw new UnsupportedOperationException("Not needed");
    }

    @Override
    public boolean hasRhs() {
        return false;
    }

    @Override
    public double rhs() {
        return 0;
    }
}
