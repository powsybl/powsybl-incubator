/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.security.*;

import java.util.Objects;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SimpleDcSecurityAnalysisFactory implements SecurityAnalysisFactory {

    private final MatrixFactory matrixFactory;

    public SimpleDcSecurityAnalysisFactory(MatrixFactory matrixFactory) {
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    @Override
    public SecurityAnalysis create(Network network, ComputationManager computationManager, int priority) {
        return create(network, new LimitViolationFilter(), computationManager, priority);
    }

    @Override
    public SecurityAnalysis create(Network network, LimitViolationFilter filter, ComputationManager computationManager, int priority) {
        return create(network, new DcLimitViolationDetector(), filter, computationManager, priority);
    }

    @Override
    public SecurityAnalysis create(Network network, LimitViolationDetector detector, LimitViolationFilter filter,
                                   ComputationManager computationManager, int priority) {
        return new SimpleDcSecurityAnalysis(network, detector, filter, matrixFactory);
    }
}
