/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.loadflow.simple.equations.EquationSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NewtonRaphsonObserverLogger extends DefaultNewtonRaphsonObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewtonRaphsonObserverLogger.class);

    @Override
    public void endIteration(int iteration, double fxNorm) {
        LOGGER.debug("Iteration {}: {}", iteration, fxNorm);
    }

    @Override
    public void x(double[] x, EquationSystem equationSystem, int iteration) {
        equationSystem.logLargestMismatches(x);
    }
}
