/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.incubator.simulator.util.AdmittanceEquationSystem;
import com.powsybl.incubator.simulator.util.CalculationLocation;
import com.powsybl.incubator.simulator.util.extensions.ShortCircuitExtensions;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.ac.outerloop.AcLoadFlowParameters;
import com.powsybl.openloadflow.graph.EvenShiloachGraphDecrementalConnectivityFactory;
import com.powsybl.openloadflow.network.LfNetwork;
import com.powsybl.openloadflow.network.LfNetworkParameters;
import com.powsybl.openloadflow.network.impl.LfNetworkLoaderImpl;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public abstract class AbstractShortCircuitEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractShortCircuitEngine.class);

    protected final Network network;

    protected final ShortCircuitEngineParameters parameters;

    protected final List<LfNetwork> lfNetworks;

    protected final Map<ShortCircuitFault, ShortCircuitResult> resultsPerFault = new LinkedHashMap<>();

    protected List<CalculationLocation> solverFaultList; // list of faults provided to the solver (not including biphased common support faults)

    protected List<CalculationLocation> solverBiphasedFaultList; // list of biphased common support faults provided to the solver

    protected final AcLoadFlowParameters acLoadFlowParameters;

    protected AbstractShortCircuitEngine(Network network, ShortCircuitEngineParameters parameters) {
        this.network = Objects.requireNonNull(network);
        this.parameters = Objects.requireNonNull(parameters);
        this.lfNetworks = LfNetwork.load(network, new LfNetworkLoaderImpl(), new LfNetworkParameters());
        this.acLoadFlowParameters = getAcLoadFlowParametersFromParam();
        ShortCircuitNorm shortCircuitNorm = parameters.getNorm();
        ShortCircuitExtensions.add(network, lfNetworks, shortCircuitNorm.getNormExtensions());
    }

    protected AcLoadFlowParameters getAcLoadFlowParametersFromParam() {
        OpenLoadFlowParameters loadflowParametersExt = OpenLoadFlowParameters.get(parameters.getLoadFlowParameters());
        return OpenLoadFlowParameters.createAcParameters(parameters.getLoadFlowParameters(), loadflowParametersExt, parameters.getMatrixFactory(), new EvenShiloachGraphDecrementalConnectivityFactory<>(), false, false);
    }

    protected AdmittanceEquationSystem.AdmittancePeriodType getAdmittancePeriodTypeFromParam() {
        AdmittanceEquationSystem.AdmittancePeriodType admittancePeriodType = AdmittanceEquationSystem.AdmittancePeriodType.ADM_TRANSIENT;
        if (parameters.getPeriodType() == ShortCircuitEngineParameters.PeriodType.SUB_TRANSIENT) {
            admittancePeriodType = AdmittanceEquationSystem.AdmittancePeriodType.ADM_SUB_TRANSIENT;
        } else if (parameters.getPeriodType() == ShortCircuitEngineParameters.PeriodType.STEADY_STATE) {
            admittancePeriodType = AdmittanceEquationSystem.AdmittancePeriodType.ADM_STEADY_STATE;
        }
        return admittancePeriodType;
    }

    protected AdmittanceEquationSystem.AdmittanceVoltageProfileType getAdmittanceVoltageProfileTypeFromParam() {
        AdmittanceEquationSystem.AdmittanceVoltageProfileType admittanceVoltageProfileType = AdmittanceEquationSystem.AdmittanceVoltageProfileType.NOMINAL;
        if (parameters.getVoltageProfileType() == ShortCircuitEngineParameters.VoltageProfileType.CALCULATED) {
            admittanceVoltageProfileType = AdmittanceEquationSystem.AdmittanceVoltageProfileType.CALCULATED;
        }
        return admittanceVoltageProfileType;
    }

    protected void buildSystematicList(ShortCircuitFault.ShortCircuitType type) {
        List<ShortCircuitFault> scfSystematic = new ArrayList<>();
        parameters.setVoltageUpdate(false);
        for (Bus bus : network.getBusBreakerView().getBuses()) {
            ShortCircuitFault sc = new ShortCircuitFault(bus.getId(), bus.getId(),  0., 0., type); //TODO : check validity of voltage levels if no connexity
            scfSystematic.add(sc);
        }
        parameters.setShortCircuitFaults(scfSystematic);
    }

    protected Pair<List<CalculationLocation>, List<CalculationLocation>> buildFaultListsFromInputs() {
        // We handle a pre-treatement of faults given in input:
        // - filtering faults because of some inconsistencies on the bus identification
        // - addition of info in each fault to ease the identification in LfNetwork of iidm info

        List<CalculationLocation> faultList = new ArrayList<>();
        List<CalculationLocation> biphasedFaultList = new ArrayList<>();
        Map<String, Pair<String, Integer >> tmpListBus1 = new HashMap<>();
        for (ShortCircuitFault scfe : parameters.getShortCircuitFaults()) {
            String busName = scfe.getBusLocation();
            String bus2Name = scfe.getBus2Location();

            if (bus2Name.isEmpty()) {
                if (scfe.getType() == ShortCircuitFault.ShortCircuitType.BIPHASED_COMMON_SUPPORT) {
                    throw new IllegalArgumentException(" short circuit fault : " + busName + " must have a second voltage level defined because it is a common support fault");
                }
                Pair<String, Integer> branchFaultInfo = buildFaultBranchFromBusId(busName, network); // creates additional info for fault, identifying location through iidm branches instead of iidm busses to easily get lf busses
                scfe.setIidmBusInfo(branchFaultInfo); // the short circuit fault info is now enriched with the couple iidmBranchId + iidmBranchSide and not only the iidm bus name in order to be able to identify the busses in the LfNetwork
                faultList.add(scfe);

            } else {
                if (scfe.getType() != ShortCircuitFault.ShortCircuitType.BIPHASED_COMMON_SUPPORT) {
                    throw new IllegalArgumentException(" short circuit fault : " + busName + " has a second bus defined : " + bus2Name + " but is not a common support fault");
                }

                // Step 1 : get info at bus 1 initialization of bus 2 list
                tmpListBus1.computeIfAbsent(busName, k -> buildFaultBranchFromBusId(busName, network));

                // step 2 : get info at bus 2
                Pair<String, Integer > branchBus2FaultInfo = buildFaultBranchFromBusId(bus2Name, network);
                Pair<String, Integer > branchBus1FaultInfo = tmpListBus1.get(busName);

                scfe.setIidmBusInfo(branchBus1FaultInfo);
                scfe.setIidmBus2Info(branchBus2FaultInfo);
                biphasedFaultList.add(scfe);
            }
        }

        return new Pair<>(faultList, biphasedFaultList);
    }

    protected static Pair<String, Integer > buildFaultBranchFromBusId(String busId, Network tmpNetwork) {
        Pair<String, Integer > branchFaultInfo = buildFaultDipoleFromBusId(busId, tmpNetwork);
        if (branchFaultInfo.getKey().equals("")) {
            // Bus not found in branches, try three windings transformers
            branchFaultInfo = buildFaultT3WbranchFromBusId(busId, tmpNetwork);
        }
        return branchFaultInfo;
    }

    protected static Pair<String, Integer > buildFaultDipoleFromBusId(String busId, Network tmpNetwork) {
        // improve with direct correspondence between iidm busses and lfBusses when available in PowSyBl, because this loop is not very efficient
        Bus bus = tmpNetwork.getBusBreakerView().getBus(busId);
        String branchId = "";
        int branchSide = 0;
        boolean isFound = false;
        for (Branch<?> branch : tmpNetwork.getBranches()) {
            Bus bus1 = branch.getTerminal1().getBusBreakerView().getBus();
            Bus bus2 = branch.getTerminal2().getBusBreakerView().getBus();
            if (bus == bus1) {
                branchId = branch.getId();
                branchSide = 1;
                isFound = true;
                break;
            } else if (bus == bus2) {
                branchId = branch.getId();
                branchSide = 2;
                isFound = true;
                break;
            }
        }

        if (!isFound) {
            LOGGER.warn(" input CC Bus {} could not be associated with a bipole", busId);
        }

        return new Pair<>(branchId, branchSide);
    }

    protected static Pair<String, Integer> buildFaultT3WbranchFromBusId(String busId, Network tmpNetwork) {
        // improve with direct correspondence between iidm busses and lfBusses when available in PowSyBl, because this loop is not very efficient
        Bus bus = tmpNetwork.getBusBreakerView().getBus(busId);
        String branchId = "";
        int legNum = 0;
        boolean isFound = false;
        for (ThreeWindingsTransformer t3w : tmpNetwork.getThreeWindingsTransformers()) {
            Bus bus1 = t3w.getLeg1().getTerminal().getBusBreakerView().getBus();
            Bus bus2 = t3w.getLeg2().getTerminal().getBusBreakerView().getBus();
            Bus bus3 = t3w.getLeg3().getTerminal().getBusBreakerView().getBus();

            if (bus == bus1) {
                branchId = t3w.getId();
                legNum = 1;
                isFound = true;
                break;
            } else if (bus == bus2) {
                branchId = t3w.getId();
                legNum = 2;
                isFound = true;
                break;
            } else if (bus == bus3) {
                branchId = t3w.getId();
                legNum = 3;
                isFound = true;
                break;
            }
        }

        if (!isFound) {
            LOGGER.warn(" input CC Bus {} could not be associated with a tripole", busId);
        }

        return new Pair<>(branchId, legNum);
    }

    public Map<ShortCircuitFault, ShortCircuitResult> getResultsPerFault() {
        return resultsPerFault;
    }

    public abstract void run();

}
