/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.google.auto.service.AutoService;
import com.google.common.base.Stopwatch;
import com.powsybl.incubator.simulator.util.extensions.AdditionalDataInfo;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.math.matrix.SparseMatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.security.LimitViolation;
import com.powsybl.shortcircuit.*;
import com.powsybl.shortcircuit.interceptors.ShortCircuitAnalysisInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
@AutoService(ShortCircuitAnalysisProvider.class)
public class OpenShortCircuitProvider implements ShortCircuitAnalysisProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenShortCircuitProvider.class);

    private final List<ShortCircuitAnalysisInterceptor> interceptors = new ArrayList<>();

    private final MatrixFactory matrixFactory;

    public OpenShortCircuitProvider() {
        this(new SparseMatrixFactory());
    }

    public OpenShortCircuitProvider(MatrixFactory matrixFactory) {
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
    }

    @Override
    public void addInterceptor(ShortCircuitAnalysisInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    @Override
    public boolean removeInterceptor(ShortCircuitAnalysisInterceptor interceptor) {
        return interceptors.remove(interceptor);
    }

    @Override
    public String getName() {
        return "OpenShortCircuit";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public CompletableFuture<ShortCircuitAnalysisResult> run(Network network, List<Fault> faults, ShortCircuitParameters parameters, ComputationManager computationManager, List<FaultParameters> faultParameters) {
        //public CompletableFuture<ShortCircuitAnalysisResult> run(Network network, List<Fault> faults, ShortCircuitParameters parameters, ComputationManager computationManager, List<FaultParameters> faultParameters, Reporter reporter) {

        Objects.requireNonNull(network);
        Objects.requireNonNull(parameters);
        Stopwatch stopwatch = Stopwatch.createStarted();

        LoadFlowParameters lfParameters = new LoadFlowParameters();
        LoadFlow.Runner  loadFlowRunner = new LoadFlow.Runner(new OpenLoadFlowProvider(matrixFactory));

        LoadFlowResult lfResult = loadFlowRunner.run(network, lfParameters);

        // building of fault lists
        List<ShortCircuitFault> faultsList = new ArrayList<>();
        Map<ShortCircuitFault, Fault> scFaultToFault = new HashMap<>(); // for now we use this map to get the correspondence between short circuit provider and internal modelling of fault

        Pair<Boolean, Boolean> faultTypes = buildFaultLists(network, faults, faultsList, scFaultToFault);
        boolean existBalancedFaults = faultTypes.getKey();
        boolean existUnbalancedFaults = faultTypes.getValue();

        //Parameters that could be added in the short circuit provider API later:
        // Voltage Profile
        //ShortCircuitBalancedParameters.VoltageProfileType vp = ShortCircuitBalancedParameters.VoltageProfileType.CALCULATED;
        ShortCircuitEngineParameters.VoltageProfileType voltageProfile = ShortCircuitEngineParameters.VoltageProfileType.NOMINAL;

        // Selective or Systematic short circuit analysis
        //ShortCircuitBalancedParameters.AnalysisType at = ShortCircuitBalancedParameters.AnalysisType.SYSTEMATIC;
        ShortCircuitEngineParameters.AnalysisType at = ShortCircuitEngineParameters.AnalysisType.SELECTIVE;

        // selection of the period of analysis
        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.TRANSIENT;

        AdditionalDataInfo additionalDataInfo = new AdditionalDataInfo(); //no extra data handled in the provider, we need to enrich the input API if necessary

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        ShortCircuitNorm shortCircuitNorm = new ShortCircuitNorm();

        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, at, faultsList, true, voltageProfile, false, periodType, additionalDataInfo, shortCircuitNorm);

        // lists to store the results
        List<FaultResult> faultResults = new ArrayList<>();
        //List<LimitViolation> lvs = new ArrayList<>();

        if (existBalancedFaults) {
            runBalancedAnalysis(network, scbParameters, scFaultToFault, faultResults);
        }

        if (existUnbalancedFaults) {
            runUnbalancedAnalysis(network, scbParameters, scFaultToFault, faultResults);
        }

        LOGGER.info("Short circuit calculation done in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return CompletableFuture.completedFuture(new ShortCircuitAnalysisResult(faultResults));
    }

    public void runUnbalancedAnalysis(Network network, ShortCircuitEngineParameters scbParameters, Map<ShortCircuitFault, Fault> scFaultToFault, List<FaultResult> faultResults) {
        ShortCircuitUnbalancedEngine scuEngine = new ShortCircuitUnbalancedEngine(network, scbParameters);
        scuEngine.run();

        // the results per faults might be inconsistent if many busses per voltage level
        // TODO : see how this could be improved by allowing results per electrical bus on the short circuit provider
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> scResult : scuEngine.resultsPerFault.entrySet()) {
            ShortCircuitFault scFault = scResult.getKey();

            double iccMagnitude = scResult.getValue().getIcc().getKey();
            double iccAngle = scResult.getValue().getIcc().getValue();

            Fault fault = scFaultToFault.get(scFault);

            List<FeederResult> feederResults = new ArrayList<>();
            List<LimitViolation> limitViolations = new ArrayList<>();
            FortescueValue current = new FortescueValue(iccMagnitude, iccAngle);

            FaultResult fr = new FaultResult(fault, 0., feederResults, limitViolations, current);
            faultResults.add(fr);
        }
    }

    public void runBalancedAnalysis(Network network, ShortCircuitEngineParameters scbParameters, Map<ShortCircuitFault, Fault> scFaultToFault, List<FaultResult> faultResults) {
        ShortCircuitBalancedEngine scbEngine = new ShortCircuitBalancedEngine(network, scbParameters);
        scbEngine.run();

        // the results per faults might be inconsistent if many busses per voltage level
        // TODO : see how this could be improved by allowing results per electrical bus on the short circuit provider
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> scResult : scbEngine.resultsPerFault.entrySet()) {
            ShortCircuitFault scFault = scResult.getKey();

            double iccMagnitude = scResult.getValue().getIk().getKey();
            double iccAngle = scResult.getValue().getIk().getValue();
            double pcc = scResult.getValue().getPcc();

            Fault fault = scFaultToFault.get(scFault);

            // TODO : put here additional results
            List<FeederResult> feederResults = new ArrayList<>();
            List<LimitViolation> limitViolations = new ArrayList<>();
            FortescueValue current = new FortescueValue(iccMagnitude, iccAngle);

            FaultResult fr = new FaultResult(fault, pcc, feederResults, limitViolations, current);
            faultResults.add(fr);
        }
    }

    public Pair<Boolean, Boolean>  buildFaultLists(Network network, List<Fault> faults, List<ShortCircuitFault> balancedFaultsList, Map<ShortCircuitFault, Fault> scFaultToFault) {

        boolean existBalancedFaults = false;
        boolean existUnbalancedFaults = false;

        for (Fault fault : faults) {
            if (fault.getType() == Fault.Type.BRANCH) {
                LOGGER.warn("Short circuit of type BRANCH not yet supported, fault : " + fault.getId() + " is ignored");
                continue;
            }

            if (fault.getFaultType() == Fault.FaultType.SINGLE_PHASE) {
                existUnbalancedFaults = true;
                LOGGER.warn(" Short circuit of type SINGLE_PHASE not yet supported, fault : " + fault.getId() + " is ignored");
                continue;
            } else if (fault.getFaultType() == Fault.FaultType.THREE_PHASE) {
                existBalancedFaults = true;
            } else {
                LOGGER.warn(" Short circuit of unknown type, fault : " + fault.getId() + " is ignored");
                continue;
            }

            // TODO : transform parallel input into a series input
            if (fault.getConnectionType() == Fault.ConnectionType.PARALLEL) {
                LOGGER.warn(" Short circuit connection of type PARALLEL not yet supported, fault : " + fault.getId() + " is ignored");
                continue;
            }

            // TODO : see how to get lfBus from iidm Bus
            String elementId = fault.getElementId();

            double rFault = fault.getRToGround();
            double xFault = fault.getXToGround();
            Bus bus = network.getBusBreakerView().getBus(elementId);
            String busId = bus.getId();
            ShortCircuitFault sc = new ShortCircuitFault(busId, busId, rFault, xFault, ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
            balancedFaultsList.add(sc);

            // TODO improve:
            scFaultToFault.put(sc, fault);

        }

        return new Pair<>(existBalancedFaults, existUnbalancedFaults);
    }

}
