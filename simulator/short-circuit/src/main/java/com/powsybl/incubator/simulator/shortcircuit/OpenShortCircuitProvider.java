/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.google.auto.service.AutoService;
import com.google.common.base.Stopwatch;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.incubator.simulator.util.FeedersAtBusResult;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.math.matrix.SparseMatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.openloadflow.network.LfBus;
import com.powsybl.security.LimitViolation;
import com.powsybl.shortcircuit.*;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
@AutoService(ShortCircuitAnalysisProvider.class)
public class OpenShortCircuitProvider implements ShortCircuitAnalysisProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenShortCircuitProvider.class);

    private final MatrixFactory matrixFactory;

    public OpenShortCircuitProvider() {
        this(new SparseMatrixFactory());
    }

    public OpenShortCircuitProvider(MatrixFactory matrixFactory) {
        this.matrixFactory = Objects.requireNonNull(matrixFactory);
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
        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.SUB_TRANSIENT;

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        ShortCircuitNorm shortCircuitNorm = new ShortCircuitNormNone();

        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, at, faultsList, true, voltageProfile, false, periodType, shortCircuitNorm);

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
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> scFaultResult : scbEngine.resultsPerFault.entrySet()) {
            ShortCircuitFault scFault = scFaultResult.getKey();
            ShortCircuitResult scResult = scFaultResult.getValue();

            double iccMagnitude = scResult.getIk().getKey();
            double iccAngle = scResult.getIk().getValue();
            double pcc = scResult.getPcc();

            Fault fault = scFaultToFault.get(scFault);

            List<FeederResult> feederResultsProvider = new ArrayList<>();
            fillFeederResults(feederResultsProvider, scResult);

            List<LimitViolation> limitViolations = new ArrayList<>();
            FortescueValue current = new FortescueValue(iccMagnitude, iccAngle);

            FaultResult fr = new FaultResult(fault, pcc, feederResultsProvider, limitViolations, current);
            faultResults.add(fr);
        }
    }

    public void fillFeederResults(List<FeederResult> feederResultsProvider, ShortCircuitResult scResult) {
        for (Map.Entry<LfBus, FeedersAtBusResult> busAndFeedersAtBusResult : scResult.getFeedersAtBusResultsDirect().entrySet()) {
            LfBus lfBus = busAndFeedersAtBusResult.getKey();
            FeedersAtBusResult feedersAtBusResult = busAndFeedersAtBusResult.getValue();
            for (com.powsybl.incubator.simulator.util.FeederResult feederResult : feedersAtBusResult.getBusFeedersResult()) {
                double ix = feederResult.getIxContribution();
                double iy = feederResult.getIyContribution();

                double magnitude = Math.sqrt(3. * (ix * ix + iy * iy)) * 100.  / lfBus.getNominalV(); // same dimension as Ik3
                double angle = Math.atan2(iy, ix);
                FortescueValue current = new FortescueValue(magnitude, angle);

                String feederId = lfBus.getId() + "_" + feederResult.getFeeder().getId();

                FeederResult feederResultProvider = new FeederResult(feederId, current);
                feederResultsProvider.add(feederResultProvider);
            }
        }

    }

    public Pair<Boolean, Boolean>  buildFaultLists(Network network, List<Fault> faults, List<ShortCircuitFault> balancedFaultsList, Map<ShortCircuitFault, Fault> scFaultToFault) {

        boolean existBalancedFaults = false;
        boolean existUnbalancedFaults = false;

        for (Fault fault : faults) {
            ShortCircuitFault.ShortCircuitType scType = ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND; // Default type
            if (fault.getType() == Fault.Type.BRANCH) {
                LOGGER.warn("Short circuit of type BRANCH not yet supported, fault : {} is ignored", fault.getId());
                continue;
            }

            if (fault.getFaultType() == Fault.FaultType.SINGLE_PHASE) {
                existUnbalancedFaults = true;
                scType = ShortCircuitFault.ShortCircuitType.MONOPHASED;
            } else if (fault.getFaultType() == Fault.FaultType.THREE_PHASE) {
                existBalancedFaults = true;
            } else {
                LOGGER.warn(" Short circuit of unknown type, fault :  is ignored", fault.getId());
                continue;
            }

            // TODO : transform parallel input into a series input
            if (fault.getConnectionType() == Fault.ConnectionType.PARALLEL) {
                LOGGER.warn(" Short circuit connection of type PARALLEL not yet supported, fault : {} is ignored", fault.getId());
                continue;
            }

            // TODO : see how to get lfBus from iidm Bus
            String elementId = fault.getElementId();

            double rFault = fault.getRToGround();
            double xFault = fault.getXToGround();
            Bus bus = network.getBusBreakerView().getBus(elementId);
            String busId = bus.getId();
            ShortCircuitFault sc = new ShortCircuitFault(busId, busId, rFault, xFault, scType);
            balancedFaultsList.add(sc);

            // TODO improve:
            scFaultToFault.put(sc, fault);

        }

        return new Pair<>(existBalancedFaults, existUnbalancedFaults);
    }

}
