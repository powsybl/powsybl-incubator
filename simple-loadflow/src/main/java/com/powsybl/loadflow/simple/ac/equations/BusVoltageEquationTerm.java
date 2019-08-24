/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.loadflow.simple.equations.AbstractTargetEquationTerm;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.VariableType;
import com.powsybl.loadflow.simple.network.LfBus;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BusVoltageEquationTerm extends AbstractTargetEquationTerm {

    public BusVoltageEquationTerm(LfBus bus, EquationContext context) {
        super(Objects.requireNonNull(bus).getNum(), VariableType.BUS_V, context);
    }
}
