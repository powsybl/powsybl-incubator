/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.action.util.Scalable;
import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * This class contains the balance adjustment computation process.
 * <p>
 *     The calculation starts with defined network and areas and consists
 *     of several stages :
 * <ul>
 *     <li>Input data validation</li>
 *     <li>LoadFlow computation</li>
 *     <li>Comparison of network area's net position with the target value</li>
 *     <li>Apply injections scaling</li>
 * </ul>
 * </p>
 *
 * @author Ameni Walha <ameni.walha at rte-france.com>
 */
public class BalanceComputationImpl implements BalanceComputation {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceComputationImpl.class);
    private Network network;

    /**
     * The target net position for each network area
     */
    private Map<NetworkArea, Double> networkAreaNetPositionTargetMap;

    /**
     * The scalable for each network area.
     * Scalable contains a list of injections (generator or load)
     * @see com.powsybl.action.util.Scalable;
     */
    private Map<NetworkArea, Scalable> networkAreasScalableMap;

    private ComputationManager computationManager;
    private LoadFlowFactory loadFlowFactory;

    public BalanceComputationImpl(Network network, Map<NetworkArea, Double> networkAreaNetPositionTargetMap, Map<NetworkArea, Scalable> networkAreasScalableMap, ComputationManager computationManager, LoadFlowFactory loadFlowFactory) {

        this.network = Objects.requireNonNull(network);
        this.networkAreaNetPositionTargetMap = Objects.requireNonNull(networkAreaNetPositionTargetMap);
        this.networkAreasScalableMap = Objects.requireNonNull(networkAreasScalableMap);
        this.computationManager = Objects.requireNonNull(computationManager);
        this.loadFlowFactory = Objects.requireNonNull(loadFlowFactory);

    }

    /**
     * Run balances adjustment computation in several iterations
     */
    @Override
    public CompletableFuture<BalanceComputationResult> run(String workingStateId, BalanceComputationParameters parameters) {
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);

        BalanceComputationResult result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED);
        int iterationCounter = 0;
        Map<NetworkArea, Double> previousScalingMap = new HashMap<>();

        LoadFlow loadFlow = loadFlowFactory.create(network, computationManager, 1);

        for (NetworkArea networkArea : networkAreaNetPositionTargetMap.keySet()) {
            previousScalingMap.put(networkArea, 0.);
        }
        // Step 1 : Input data validation
        List<String> inputDataViolations = this.listInputDataViolations();
        if (!inputDataViolations.isEmpty()) {
            inputDataViolations.forEach(error -> LOGGER.error(error));
            throw new PowsyblException("The input data for balance computation is not valid");
        }

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String workingVariantCopyId = workingStateId + " COPY";
        network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);

        Map<NetworkArea, Double> networkAreasResidue;
        List<NetworkArea> unbalancedNetworkAreas;

        while (iterationCounter < parameters.getMaxNumberIterations()
                && result.getStatus() == BalanceComputationResult.Status.FAILED) {

            // Step 2 : compute Loadflow
            LoadFlowResult loadFlowResult = runLoadFlow(loadFlow, workingVariantCopyId);
            if (!loadFlowResult.isOk()) {
                LOGGER.error("Loadflow on network {} does not converge", network.getId());
                result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, iterationCounter);
                return CompletableFuture.completedFuture(result);
            }

            // Step 3 : Balance computation iteration
            iterationCounter++;
            networkAreasResidue = getNetworkAreasResidue(network);
            unbalancedNetworkAreas = listUnbalancedNetworkAreas(parameters, networkAreasResidue);

            if (unbalancedNetworkAreas.isEmpty()) {
                // Change the workingStateId with final scaling
                network.getVariantManager().setWorkingVariant(workingStateId);
                scaleBalancedNetwork(previousScalingMap);
                runLoadFlow(loadFlow, workingStateId);
                result = new BalanceComputationResult(BalanceComputationResult.Status.SUCCESS, iterationCounter, previousScalingMap);

            } else {
                // Step 4 : scaling network areas
                result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, iterationCounter);
                network.getVariantManager().removeVariant(workingVariantCopyId);
                network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId);
                network.getVariantManager().setWorkingVariant(workingVariantCopyId);
                LOGGER.info(" Scaling iteration number {}", iterationCounter);

                Map<NetworkArea, Double> scaleNetworkAreasMap = scaleNetworkAreas(networkAreasResidue, previousScalingMap);
                previousScalingMap = scaleNetworkAreasMap;
            }
        }

        if (result.getStatus() == BalanceComputationResult.Status.SUCCESS) {
            List<String> networkAreasName = networkAreaNetPositionTargetMap.keySet().stream()
                    .map(networkArea -> networkArea.getName()).collect(Collectors.toList());
            LOGGER.info(" Network areas : {} are balanced after {} iterations", networkAreasName, result.getIterationCount());

        } else {
            LOGGER.error(" Network areas are unbalanced after {} iterations", iterationCounter);
        }

        network.getVariantManager().removeVariant(workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(initialVariantId);

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Run <code>LoadFlow</code> on network working state
     */
    LoadFlowResult runLoadFlow(LoadFlow loadFlow, String workingStateId) {
        LoadFlowResult loadFlowResult = loadFlow.run(workingStateId, new LoadFlowParameters()).join();
        LOGGER.info("Running LoadFlow on {} variant manager id", workingStateId);
        return loadFlowResult;
    }

    /**
     * @return the net position residue for each network area
     */
    private Map<NetworkArea, Double> getNetworkAreasResidue(Network network) {
        Map<NetworkArea, Double> networkAreasResidualMap = new HashMap<>();
        for (Map.Entry<NetworkArea, Double> entry : networkAreaNetPositionTargetMap.entrySet()) {
            NetworkArea networkArea = entry.getKey();
            double residue = entry.getValue() - networkArea.getNetPosition(network);
            networkAreasResidualMap.put(networkArea, residue);
        }
        return networkAreasResidualMap;
    }

    /**
     * @return the unbalanced network areas list
     * If net position residue is above the threshold, the network area is unbalanced
     */
    private List<NetworkArea> listUnbalancedNetworkAreas(BalanceComputationParameters parameters, Map<NetworkArea, Double> networkAreasResidualMap) {
        double threshold = parameters.getThresholdNetPosition();

        return networkAreasResidualMap.keySet().stream()
                .filter(networkArea -> Math.abs(networkAreasResidualMap.get(networkArea)) > threshold)
                .collect(Collectors.toList());
    }

    /**
     * @return the list of input data violations
     * If this list is empty, the balance computation continue
     */
    List<String> listInputDataViolations() {
        List<String> listOfViolations = new ArrayList<>();

        List<String> listNullElementsErrors = listNullElementsViolations(networkAreaNetPositionTargetMap, networkAreasScalableMap);
        if (!listNullElementsErrors.isEmpty()) {
            return listNullElementsErrors;
        }
        //Areas Voltage levels validation
        for (NetworkArea networkArea : networkAreaNetPositionTargetMap.keySet()) {

            List<VoltageLevel> areaVoltageLevels = networkArea.getAreaVoltageLevels(network);

            //Areas Voltage levels validation
            if (areaVoltageLevels.isEmpty()) {
                listOfViolations.add("The " + networkArea + " is not found in the network " + network);
            } else {
                List<VoltageLevel> networkVoltageLevels = network.getVoltageLevelStream().collect(Collectors.toList());
                if (!networkVoltageLevels.containsAll(areaVoltageLevels)) {
                    listOfViolations.add("The " + network + " doesn't contain all voltage levels of " + networkArea);
                }
            }

            // Injections validation
            listOfViolations.addAll(listNetworkAreaInjectionsViolations(networkArea, networkAreasScalableMap));

        }

        return listOfViolations;
    }

    /**
     * @return the list of input data violations that relate to the injections definition
     */
    private List<String> listNetworkAreaInjectionsViolations(NetworkArea networkArea, Map<NetworkArea, Scalable> networkAreasScalableMap) {
        List<String> listOfViolations = new ArrayList<>();
        if (!networkAreasScalableMap.containsKey(networkArea)) {
            listOfViolations.add("The " + networkArea.getName() + " is not defined in the scalable network areas map");

        } else {
            Scalable scalable = networkAreasScalableMap.get(networkArea);
            List<Injection> injections = new ArrayList<>();
            List<String> injectionsNotFoundInNetwork = new ArrayList<>();
            scalable.filterInjections(network, injections, injectionsNotFoundInNetwork);
            String s = "The scalable of " + networkArea;

            if (!injectionsNotFoundInNetwork.isEmpty()) {
                listOfViolations.add(s + " contains injections " + injectionsNotFoundInNetwork + " not found in the network");
            }
            if (injections.isEmpty()) {
                listOfViolations.add(s + " doesn't contain injections in network");
            } else {
                List injectionsNotInNetworkArea = injections.stream().filter(injection -> !networkArea.getAreaVoltageLevels(network).contains(injection.getTerminal().getVoltageLevel())).collect(Collectors.toList());
                if (!injectionsNotInNetworkArea.isEmpty()) {
                    listOfViolations.add(s + " contains injections " + injectionsNotInNetworkArea + " not found in the network area");
                }
            }
        }
        return listOfViolations;
    }

    /**
     * Check if there is <code>null</code> elements on input data
     * @return the list of null elements violations
     */
    private List<String> listNullElementsViolations(Map<NetworkArea, Double> networkAreaNetPositionTargetMap, Map<NetworkArea, Scalable> networkAreasScalableMap) {
        List<String> listOfViolations = new ArrayList<>();
        String error;
        if (networkAreaNetPositionTargetMap.containsKey(null)) {
            error = "The net position target map contains null network areas";
            listOfViolations.add(error);
            LOGGER.error(error);
        }

        if (networkAreaNetPositionTargetMap.containsValue(null)) {
            error = "The net position target map contains null values";
            listOfViolations.add(error);
            LOGGER.error(error);
        }

        if (networkAreasScalableMap.containsKey(null)) {
            error = "The scalable network areas map contains null network areas";
            listOfViolations.add(error);
            LOGGER.error(error);
        }
        if (networkAreasScalableMap.containsValue(null)) {
            error = "The scalable network areas map contains null values";
            listOfViolations.add(error);
            LOGGER.error(error);
        }
        return listOfViolations;
    }

    /**
     * Adjusts the generators and loads of network areas to reach the target net positions
     * @see com.powsybl.action.util.Scalable
     * @param networkAreasResidualMap the net position residue for each network area
     * @param previousScalingMap the previous value of adjustment for each network area
     * @return the value of power adjusted for each network area
     */
    private Map<NetworkArea, Double> scaleNetworkAreas(Map<NetworkArea, Double> networkAreasResidualMap, Map<NetworkArea, Double> previousScalingMap) {
        Map<NetworkArea, Double> scalingNetworkAreasMap = new HashMap<>();
        for (NetworkArea networkArea : networkAreaNetPositionTargetMap.keySet()) {
            double residue = networkAreasResidualMap.get(networkArea);
            double asked = previousScalingMap.get(networkArea) + residue;
            Scalable scalable = networkAreasScalableMap.get(networkArea);
            scalingNetworkAreasMap.put(networkArea, asked);

            double done = scalable.scale(network, asked);
            if (done != asked) {
                LOGGER.warn("The scaled power value on networkArea {} is different from the asked value", networkArea.getName());
            }

        }
        return scalingNetworkAreasMap;
    }

    private void scaleBalancedNetwork(Map<NetworkArea, Double> previousScalingMap) {
        for (Map.Entry<NetworkArea, Double> entry : previousScalingMap.entrySet()) {
            double scalingValue = entry.getValue();
            NetworkArea networkArea = entry.getKey();
            Scalable scalable = networkAreasScalableMap.get(networkArea);
            double done = scalable.scale(network, scalingValue);
            LOGGER.info("The scaled power value on networkArea {} is equals {}", networkArea.getName(), done);
        }
    }

}
