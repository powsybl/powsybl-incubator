/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac.equations;

import com.powsybl.iidm.network.Bus;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationType;
import com.powsybl.loadflow.simple.equations.AbstractTargetEquationTerm;
import com.powsybl.loadflow.simple.equations.VariableType;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BusVoltageEquationTerm extends AbstractTargetEquationTerm {

    public BusVoltageEquationTerm(Bus bus, EquationContext context) {
        super(Objects.requireNonNull(bus.getId()), EquationType.BUS_V, VariableType.BUS_V, context);
    }
}
