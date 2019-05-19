/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.loadflow.simple.equations.EquationTerm;
import com.powsybl.loadflow.simple.equations.Variable;
import com.powsybl.loadflow.simple.network.BranchCharacteristics;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
abstract class AbstractBranchAcFlowEquationTerm implements EquationTerm {

    protected final BranchCharacteristics bc;

    protected final double r1;
    protected final double r2;
    protected final double b1;
    protected final double b2;
    protected final double g1;
    protected final double g2;
    protected final double y;
    protected final double ksi;
    protected final double sinKsi;
    protected final double cosKsi;

    protected AbstractBranchAcFlowEquationTerm(BranchCharacteristics bc) {
        this.bc = Objects.requireNonNull(bc);
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
    }

    @Override
    public double rhs(Variable variable) {
        return 0;
    }
}