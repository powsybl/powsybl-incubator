/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.math.matrix.DenseMatrix;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.equations.Variable;
import com.powsybl.openloadflow.equations.VariableSet;
import com.powsybl.openloadflow.network.LfBranch;
import com.powsybl.openloadflow.network.LfBus;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class AdmittanceEquationTermY1 extends AbstractAdmittanceEquationTerm {

    protected double g12;

    protected double b12;

    protected double g1g12sum;

    protected double b1b12sum;

    public AdmittanceEquationTermY1(LfBranch branch, LfBus bus1, LfBus bus2, VariableSet<VariableType> variableSet, AdmittanceEquationSystem.AdmittanceType admittanceType, MatrixFactory mf) {
        super(branch, bus1, bus2, variableSet, mf);
        // Direct component:
        // I1y = (b1 + b12)V1x + (g1 + g12)V1y - b12 * V2x - g12 * V2y
        if (admittanceType == AdmittanceEquationSystem.AdmittanceType.ADM_THEVENIN_HOMOPOLAR) {
            HomopolarModel homopolarModel = HomopolarModel.build(branch);
            if (branch.getBranchType() == LfBranch.BranchType.LINE) {
                // case where branch is a line with available homopolar parameters
                g12 = rho * homopolarModel.getZoInvSquare() * (homopolarModel.getRo() * cosA + homopolarModel.getXo() * sinA);
                b12 = -rho * homopolarModel.getZoInvSquare() * (homopolarModel.getXo() * cosA + homopolarModel.getRo() * sinA);
                g1g12sum = rho * rho * (homopolarModel.getGom() + homopolarModel.getRo() * homopolarModel.getZoInvSquare());
                b1b12sum = rho * rho * (homopolarModel.getBom() - homopolarModel.getXo() * homopolarModel.getZoInvSquare());
            } else if (branch.getBranchType() == LfBranch.BranchType.TRANSFO_2
                    || branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_1
                    || branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_2
                    || branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_3) {
                // case where branch is part of a transformer
                DenseMatrix mo = homopolarModel.computeHomopolarAdmittanceMatrix();
                b1b12sum = mo.get(1, 0);
                g1g12sum = mo.get(1, 1);
                b12 = -mo.get(1, 2);
                g12 = -mo.get(1, 3);
            } else {
                throw new IllegalArgumentException("branch type not yet handled");
            }
        } else {
            g12 = rho * zInvSquare * (r * cosA + x * sinA);
            b12 = -rho * zInvSquare * (x * cosA + r * sinA);
            g1g12sum = rho * rho * (gPi1 + r * zInvSquare);
            b1b12sum = rho * rho * (bPi1 - x * zInvSquare);
        }
    }

    @Override
    public double getCoefficient(Variable<VariableType> variable) {
        if (variable.equals(v1rVar)) {
            return b1b12sum;
        } else if (variable.equals(v2rVar)) {
            return -b12;
        } else if (variable.equals(v1iVar)) {
            return g1g12sum;
        } else if (variable.equals(v2iVar)) {
            return -g12;
        } else {
            throw new IllegalArgumentException("Unknown variable " + variable);
        }
    }

    @Override
    protected String getName() {
        return "yi1";
    }
}
