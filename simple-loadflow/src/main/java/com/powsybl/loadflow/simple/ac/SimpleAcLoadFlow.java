/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.google.common.collect.ImmutableMap;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.math.matrix.SparseMatrixFactory;
import com.powsybl.tools.PowsyblCoreVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleAcLoadFlow implements LoadFlow {

    private static final String NAME = "Simple loadflow";

    private final Network network;

    private final MatrixFactory matrixFactory;

    private final List<AcLoadFlowObserver> additionalObservers = Collections.synchronizedList(new ArrayList<>());

    private final List<MacroIteration> macroIterations = Collections.synchronizedList(new ArrayList<>());

    public SimpleAcLoadFlow(Network network) {
        this.network = Objects.requireNonNull(network);
        this.matrixFactory = new SparseMatrixFactory();
    }

    public SimpleAcLoadFlow(Network network, MatrixFactory matrixFactory) {
        this.network = Objects.requireNonNull(network);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    public static SimpleAcLoadFlow create(Network network) {
        return new SimpleAcLoadFlow(network);
    }

    public List<AcLoadFlowObserver> getAdditionalObservers() {
        return additionalObservers;
    }

    public List<MacroIteration> getMacroIterations() {
        return macroIterations;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return new PowsyblCoreVersion().getMavenProjectVersion();
    }

    private AcLoadFlowObserver getObserver() {
        List<AcLoadFlowObserver> observers = new ArrayList<>(additionalObservers.size() + 2);
        observers.add(new AcLoadFlowLogger());
        observers.add(new AcLoadFlowProfiler());
        observers.addAll(additionalObservers);
        return AcLoadFlowObserver.of(observers);
    }

    private static ImmutableMap<String, String> createMetrics(AcLoadFlowResult result) {
        return ImmutableMap.of("iterations", Integer.toString(result.getNewtowRaphsonIterations()),
                               "status", result.getNewtonRaphsonStatus().name());
    }

    @Override
    public CompletableFuture<LoadFlowResult> run(String workingStateId, LoadFlowParameters parameters) {
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);

        return CompletableFuture.supplyAsync(() -> {
            network.getVariantManager().setWorkingVariant(workingStateId);

            AcLoadFlowResult result = new AcloadFlowEngine(network, parameters, matrixFactory, getObserver(), macroIterations)
                    .run();

            return new LoadFlowResultImpl(result.getNewtonRaphsonStatus() == NewtonRaphsonStatus.CONVERGED, createMetrics(result), null);
        });
    }
}
