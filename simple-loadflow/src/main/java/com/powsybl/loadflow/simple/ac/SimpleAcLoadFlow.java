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
import com.powsybl.loadflow.simple.ac.macro.AcLoadFlowObserver;
import com.powsybl.loadflow.simple.ac.macro.AcLoadFlowResult;
import com.powsybl.loadflow.simple.ac.macro.AcloadFlowEngine;
import com.powsybl.loadflow.simple.ac.macro.MacroAction;
import com.powsybl.loadflow.simple.ac.nr.NewtonRaphsonStatus;
import com.powsybl.loadflow.simple.ac.nr.VoltageInitializer;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.loadflow.simple.network.SlackBusSelector;
import com.powsybl.loadflow.simple.network.impl.LfNetworks;
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
        return ImmutableMap.of("iterations", Integer.toString(result.getNewtonRaphsonIterations()),
                               "status", result.getNewtonRaphsonStatus().name());
    }

    @Override
    public CompletableFuture<LoadFlowResult> run(String workingStateId, LoadFlowParameters parameters) {
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);

        return CompletableFuture.supplyAsync(() -> {
            network.getVariantManager().setWorkingVariant(workingStateId);

            SimpleAcLoadFlowParameters parametersExt = parameters.getExtension(SimpleAcLoadFlowParameters.class);
            if (parametersExt == null) {
                parametersExt = new SimpleAcLoadFlowParameters();
            }

            SlackBusSelector slackBusSelector = parametersExt.getSlackBusSelectionMode().getSelector();

            VoltageInitializer voltageInitializer = VoltageInitializer.getFromParameters(parameters);

            List<MacroAction> macroActions = new ArrayList<>();
            if (parametersExt.isDistributedSlack()) {
                macroActions.add(new DistributedSlackAction());
            }

            List<LfNetwork> lfNetworks = LfNetworks.create(network, slackBusSelector);

            // only process main (largest) connected component
            LfNetwork lfNetwork = lfNetworks.get(0);

            AcLoadFlowResult result = new AcloadFlowEngine(lfNetwork, voltageInitializer, macroActions, matrixFactory, getObserver())
                    .run();

            // update network state
            LfNetworks.resetState(network);
            lfNetwork.updateState();

            return new LoadFlowResultImpl(result.getNewtonRaphsonStatus() == NewtonRaphsonStatus.CONVERGED, createMetrics(result), null);
        });
    }
}
