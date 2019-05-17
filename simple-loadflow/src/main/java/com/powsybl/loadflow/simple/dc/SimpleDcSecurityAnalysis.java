/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc;

import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.api.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.security.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SimpleDcSecurityAnalysis extends AbstractSecurityAnalysis {

    private final MatrixFactory matrixFactory;

    public SimpleDcSecurityAnalysis(Network network) {
        this(network, new DenseMatrixFactory());
    }

    public SimpleDcSecurityAnalysis(Network network, MatrixFactory matrixFactory) {
        this(network, new DcLimitViolationDetector(), new LimitViolationFilter(), matrixFactory);
    }

    public SimpleDcSecurityAnalysis(Network network, LimitViolationDetector detector, LimitViolationFilter filter,
                                    MatrixFactory matrixFactory) {
        super(network, detector, filter);
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    @Override
    public CompletableFuture<SecurityAnalysisResult> run(String workingStateId, SecurityAnalysisParameters securityAnalysisParameters,
                                                         ContingenciesProvider contingenciesProvider) {
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(securityAnalysisParameters);
        Objects.requireNonNull(contingenciesProvider);

        LoadFlowParameters loadFlowParameters = securityAnalysisParameters.getLoadFlowParameters();

        LoadFlow loadFlow = new SimpleDcLoadFlow(network, matrixFactory);

        // start post contingency LF from pre-contingency state variables
        LoadFlowParameters postContParameters = loadFlowParameters.copy().setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES);

        LoadFlowResult loadFlowResult = loadFlow.run(workingStateId, loadFlowParameters).join();
        network.getVariantManager().setWorkingVariant(workingStateId);
        SecurityAnalysisResultBuilder resultBuilder = createResultBuilder(workingStateId);

        if (!loadFlowResult.isOk()) {
            resultBuilder.preContingency()
                    .setComputationOk(false)
                    .endPreContingency()
                    .build();
            return CompletableFuture.completedFuture(resultBuilder.build());
        }

        resultBuilder.preContingency()
                .setComputationOk(true);
        violationDetector.checkAll(network, resultBuilder::addViolation);
        resultBuilder.endPreContingency();

        List<Contingency> contingencies = contingenciesProvider.getContingencies(network);

        String postContStateId = workingStateId + "_contingency";

        for (Contingency contingency : contingencies) {

            // run one loadflow per contingency
            network.getVariantManager().cloneVariant(workingStateId, postContStateId);
            network.getVariantManager().setWorkingVariant(postContStateId);

            // apply the contingency on the network
            contingency.toTask().modify(network, null);

            LoadFlowResult contingencyResult = loadFlow.run(postContStateId, postContParameters).join();
            resultBuilder.contingency(contingency)
                    .setComputationOk(contingencyResult.isOk());
            violationDetector.checkAll(network, resultBuilder::addViolation);
            resultBuilder.endContingency();

            network.getVariantManager().removeVariant(postContStateId);
        }
        network.getVariantManager().setWorkingVariant(workingStateId);

        return CompletableFuture.completedFuture(resultBuilder.build());
    }
}
