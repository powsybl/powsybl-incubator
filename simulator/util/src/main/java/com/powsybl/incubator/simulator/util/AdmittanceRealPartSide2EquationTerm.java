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
public class AdmittanceRealPartSide2EquationTerm extends AbstractAdmittanceEquationTerm {

    protected double g21;

    protected double b21;

    protected double g2g21sum;

    protected double b2b21sum;

    public AdmittanceRealPartSide2EquationTerm(LfBranch branch, LfBus bus1, LfBus bus2, VariableSet<VariableType> variableSet, AdmittanceEquationSystem.AdmittanceType admittanceType, MatrixFactory mf) {
        super(branch, bus1, bus2, variableSet, mf);
        // Direct component:
        // I2x = -g21 * V1x + b21 * V1y + (g2 + g21)V2x - (b2 + b21)V2y
        double g12 = rho * zInvSquare * (r * cosA + x * sinA);
        g21 = g12;
        b21 = rho * zInvSquare * (r * sinA - x * cosA);
        g2g21sum = r * zInvSquare + gPi2;
        b2b21sum = -x * zInvSquare + bPi2;
        if (admittanceType == AdmittanceEquationSystem.AdmittanceType.ADM_THEVENIN_HOMOPOLAR) {
            setHomopolarAttributes();
            if (branch.getBranchType() == LfBranch.BranchType.LINE) {
                g21 = rho * homopolarExtension.zoInvSquare * (homopolarExtension.ro * cosA + homopolarExtension.xo * sinA);
                b21 = rho * homopolarExtension.zoInvSquare * (homopolarExtension.ro * sinA - homopolarExtension.xo * cosA);
                g2g21sum = homopolarExtension.ro * homopolarExtension.zoInvSquare + gPi2 * AdmittanceConstants.COEF_XO_XD;
                b2b21sum = -homopolarExtension.xo * homopolarExtension.zoInvSquare + bPi2 * AdmittanceConstants.COEF_XO_XD;
            } else if (branch.getBranchType() == LfBranch.BranchType.TRANSFO_2
                    || branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_1
                    || branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_2
                    || branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_3) {
                // case where branch is part of a transformer
                DenseMatrix mo = computeHomopolarAdmittanceMatrix();
                b2b21sum = -mo.get(2, 3);
                g2g21sum = mo.get(2, 2);
                b21 = mo.get(2, 1);
                g21 = -mo.get(2, 0);
            } else {
                throw new IllegalArgumentException("branch type not yet handled");
            }
        }
    }

    @Override
    public double getCoefficient(Variable<VariableType> variable) {
        if (variable.equals(v1rVar)) {
            return -g21;
        } else if (variable.equals(v2rVar)) {
            return g2g21sum;
        } else if (variable.equals(v1iVar)) {
            return b21;
        } else if (variable.equals(v2iVar)) {
            return -b2b21sum;
        } else {
            throw new IllegalArgumentException("Unknown variable " + variable);
        }
    }

    @Override
    protected String getName() {
        return "yr2";
    }
}
