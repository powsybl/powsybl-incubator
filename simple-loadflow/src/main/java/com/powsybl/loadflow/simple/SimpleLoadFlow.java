/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple;

import com.google.common.base.Stopwatch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.loadflow.simple.dc.DcEquationSystemMaker;
import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.network.NetworkContext;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.LUDecomposition;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * A dummy DC load flow implementation for test purposes.
 *
 * It only updates active power flow on branches, and only takes into account lines, two winding transformers,
 * generators, and loads.
 * Lines open on one side are considered open.
 * The slack bus is the first bus in the network bus ordering.
 *
 *
 * Does not use {@link com.powsybl.computation.ComputationManager}, so that it can be used with a {@code null} one.

 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SimpleLoadFlow implements LoadFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLoadFlow.class);

    private final Network network;

    private final MatrixFactory matrixFactory;

    public SimpleLoadFlow(Network network) {
        this(network, new DenseMatrixFactory());
    }

    public SimpleLoadFlow(Network network, MatrixFactory matrixFactory) {
        this.network = Objects.requireNonNull(network);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    private static void balance(NetworkContext networkContext) {
        double activeGeneration = 0;
        double activeLoad = 0;
        for (Bus b : networkContext.getBuses()) {
            for (Generator g : b.getGenerators()) {
                activeGeneration += g.getTargetP();
            }
            for (Load l : b.getLoads()) {
                activeLoad += l.getP0();
            }
        }

        LOGGER.info("Active generation={} Mw, active load={} Mw", Math.round(activeGeneration), Math.round(activeLoad));
    }

    @Override
    public CompletableFuture<LoadFlowResult> run(String state, LoadFlowParameters loadFlowParameters) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        network.getVariantManager().setWorkingVariant(state);

        NetworkContext networkContext = NetworkContext.of(network);

        balance(networkContext);

        EquationContext equationContext = new EquationContext();

        EquationSystem equationSystem = new DcEquationSystemMaker()
                .make(networkContext, equationContext);

        double[] x = equationSystem.initState(loadFlowParameters.getVoltageInitMode());

        double[] targets = equationSystem.getTargets();

        Matrix j = equationSystem.buildJacobian(matrixFactory, x);

        double[] dx = Arrays.copyOf(targets, targets.length);

        boolean status;
        try {
            try (LUDecomposition lu = j.decomposeLU()) {
                lu.solve(dx);
            }
            status = true;
        } catch (Exception e) {
            status = false;
            LOGGER.error("Failed to solve linear system for simple DC load flow.", e);
        }

        equationSystem.logLargestMistatches(dx);

        networkContext.resetState();
        equationSystem.updateState(dx);

        stopwatch.stop();
        LOGGER.info("DC loadflow complete in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return CompletableFuture.completedFuture(new LoadFlowResultImpl(status, Collections.emptyMap(), null));
    }

    @Override
    public String getName() {
        return "simple-dc-loadflow";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
