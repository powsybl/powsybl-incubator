/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.powsybl.iidm.api.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.math.matrix.SparseMatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleAcLoadFlow implements LoadFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAcLoadFlow.class);

    private final Network network;

    private final MatrixFactory matrixFactory;

    public SimpleAcLoadFlow(Network network, MatrixFactory matrixFactory) {
        this.network = Objects.requireNonNull(network);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    public static SimpleAcLoadFlow create(Network network) {
        return new SimpleAcLoadFlow(network, new SparseMatrixFactory());
    }

    @Override
    public String getName() {
        return "Simple loadflow";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public CompletableFuture<LoadFlowResult> run(String workingStateId, LoadFlowParameters parameters) {
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);

        Stopwatch stopwatch = Stopwatch.createStarted();

        SimpleAcLoadFlowParameters parametersExt = parameters.getExtension(SimpleAcLoadFlowParameters.class);
        if (parametersExt == null) {
            parametersExt = new SimpleAcLoadFlowParameters();
        }

        NetworkContext networkContext = NetworkContext.of(network).get(0);
        if (parametersExt.getSlackBusSelection() == SimpleAcLoadFlowParameters.SlackBusSelection.MOST_MESHED) {
            networkContext.setMostMeshedBusAsSlack();
        }

        NewtonRaphsonParameters nrParameters = new NewtonRaphsonParameters()
                .setVoltageInitMode(parameters.getVoltageInitMode());
        NewtonRaphsonObserver observer = NewtonRaphsonObserver.of(new NewtonRaphsonObserverLogger(), new NewtonRaphsonProfiler());
        NewtonRaphsonResult result = new NewtonRaphson(networkContext, matrixFactory, observer)
                .run(nrParameters);

        stopwatch.stop();
        LOGGER.info("Ac loadflow ran in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        Map<String, String> metrics = ImmutableMap.of("iterations", Integer.toString(result.getIterations()),
                "status", result.getStatus().name());

        return CompletableFuture.completedFuture(new LoadFlowResultImpl(result.getStatus() == NewtonRaphsonStatus.CONVERGED, metrics, null));
    }
}
