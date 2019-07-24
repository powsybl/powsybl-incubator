/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.math.matrix.LUDecomposition;
import com.powsybl.math.matrix.Matrix;

import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Jacobian {

    static final class PartialDerivative {

        private final EquationTerm equationTerm;

        private final Matrix.Element matrixElement;

        private final Variable variable;

        PartialDerivative(EquationTerm equationTerm, Matrix.Element matrixElement, Variable variable) {
            this.equationTerm = Objects.requireNonNull(equationTerm);
            this.matrixElement = Objects.requireNonNull(matrixElement);
            this.variable = Objects.requireNonNull(variable);
        }

        EquationTerm getEquationTerm() {
            return equationTerm;
        }

        Matrix.Element getMatrixElement() {
            return matrixElement;
        }

        Variable getVariable() {
            return variable;
        }
    }

    private final Matrix matrix;

    private final List<PartialDerivative> partialDerivatives;

    private LUDecomposition lu;

    public Jacobian(Matrix matrix, List<PartialDerivative> partialDerivatives) {
        this.matrix = Objects.requireNonNull(matrix);
        this.partialDerivatives = Objects.requireNonNull(partialDerivatives);
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public void update() {
        matrix.reset();
        for (PartialDerivative partialDerivative : partialDerivatives) {
            EquationTerm equationTerm = partialDerivative.getEquationTerm();
            Matrix.Element element = partialDerivative.getMatrixElement();
            Variable var = partialDerivative.getVariable();
            double value = equationTerm.der(var);
            element.add(value);
        }
    }

    public LUDecomposition decomposeLU() {
        if (lu == null) {
            lu = matrix.decomposeLU();
        } else {
            lu.update();
        }
        return lu;
    }

    public void cleanLU() {
        if (lu != null) {
            lu.close();
            lu = null;
        }
    }
}
