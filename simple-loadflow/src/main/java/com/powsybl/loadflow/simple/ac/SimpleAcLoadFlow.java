/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.google.common.collect.ImmutableMap;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.math.matrix.SparseMatrixFactory;

import java.util.Map;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleAcLoadFlow {

    private final Network network;

    private final MatrixFactory matrixFactory;

    public SimpleAcLoadFlow(Network network, MatrixFactory matrixFactory) {
        this.network = Objects.requireNonNull(network);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    public static SimpleAcLoadFlow create(Network network) {
        return new SimpleAcLoadFlow(network, new SparseMatrixFactory());
    }

    public LoadFlowResult run() {
        NetworkContext networkContext = NetworkContext.of(network);

        NewtonRaphsonResult result = new NewtonRaphson(networkContext, matrixFactory, new DefaultNewtonRaphsonObserver())
                .run(new NewtonRaphsonParameters());

        Map<String, String> metrics = ImmutableMap.of("iterations", Integer.toString(result.getIterations()),
                                                      "status", result.getStatus().name());

        return new LoadFlowResultImpl(result.getStatus() == NewtonRaphsonStatus.CONVERGED, metrics, null);
    }
}
