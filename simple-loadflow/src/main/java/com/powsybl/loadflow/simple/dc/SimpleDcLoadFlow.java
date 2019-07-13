/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc;

import com.google.common.base.Stopwatch;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.loadflow.simple.ac.nr.VoltageInitializer;
import com.powsybl.loadflow.simple.dc.equations.DcEquationSystem;
import com.powsybl.loadflow.simple.equations.EquationSystem;
import com.powsybl.loadflow.simple.network.FirstSlackBusSelector;
import com.powsybl.loadflow.simple.network.LfBus;
import com.powsybl.loadflow.simple.network.LfNetwork;
import com.powsybl.loadflow.simple.network.impl.LfNetworks;
import com.powsybl.math.matrix.LUDecomposition;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.math.matrix.SparseMatrixFactory;
import com.powsybl.tools.PowsyblCoreVersion;
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
public class SimpleDcLoadFlow implements LoadFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDcLoadFlow.class);

    private static final String NAME = "Simple DC loadflow";

    private final Network network;

    private final MatrixFactory matrixFactory;

    public SimpleDcLoadFlow(Network network) {
        this(network, new SparseMatrixFactory());
    }

    public SimpleDcLoadFlow(Network network, MatrixFactory matrixFactory) {
        this.network = Objects.requireNonNull(network);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    public static SimpleDcLoadFlow create(Network network) {
        return new SimpleDcLoadFlow(network);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return new PowsyblCoreVersion().getMavenProjectVersion();
    }

    private static void balance(LfNetwork network) {
        double activeGeneration = 0;
        double activeLoad = 0;
        for (LfBus b : network.getBuses()) {
            activeGeneration += b.getGenerationTargetP();
            activeLoad += b.getLoadTargetP();
        }

        LOGGER.info("Active generation={} Mw, active load={} Mw", Math.round(activeGeneration), Math.round(activeLoad));
    }

    @Override
    public CompletableFuture<LoadFlowResult> run(String workingStateId, LoadFlowParameters loadFlowParameters) {
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(loadFlowParameters);

        return CompletableFuture.supplyAsync(() -> {
            Stopwatch stopwatch = Stopwatch.createStarted();

            network.getVariantManager().setWorkingVariant(workingStateId);

            LfNetwork lfNetwork = LfNetworks.create(network, new FirstSlackBusSelector()).get(0);

            balance(lfNetwork);

            EquationSystem equationSystem = DcEquationSystem.create(lfNetwork);

            VoltageInitializer voltageInitializer = VoltageInitializer.getFromParameters(loadFlowParameters);

            double[] x = equationSystem.initState(voltageInitializer);

            double[] targets = equationSystem.initTargets();

            equationSystem.updateEquationTerms(x);
            Matrix j = equationSystem.buildJacobian(matrixFactory);

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

            equationSystem.updateEquationTerms(dx);
            equationSystem.updateState(dx);

            LfNetworks.resetState(network);
            lfNetwork.updateState();

            stopwatch.stop();
            LOGGER.info("DC loadflow complete in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return new LoadFlowResultImpl(status, Collections.emptyMap(), null);
        });
    }
}
