/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import java.util.List;

/**
 * An equation term, i.e part of the equation sum.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface EquationTerm {

    /**
     * Get the equation this term is part of.
     * @return the equation this term is part of
     */
    Equation getEquation();

    /**
     * Get the list of variable this equation term depends on.
     * @return the list of variable this equation term depends on.
     */
    List<Variable> getVariables();

    /**
     * Evaluate equation term.
     * @param x the state vector
     * @return value of the equation term
     */
    double eval(double[] x);

    /**
     * Evaluate partial derivative.
     *
     * @param variable the variable the partial derivative is with respect to
     * @param x the state vector
     * @return value of the partial derivative
     */
    double evalDer(Variable variable, double[] x);

    double evalRhs(Variable variable);
}
