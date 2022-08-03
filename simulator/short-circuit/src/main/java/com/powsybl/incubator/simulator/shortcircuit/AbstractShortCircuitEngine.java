/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.incubator.simulator.util.AdmittanceEquationSystem;
import com.powsybl.incubator.simulator.util.ShortCircuitFault;
import com.powsybl.incubator.simulator.util.extensions.ShortCircuitExtensions;
import com.powsybl.incubator.simulator.util.ShortCircuitResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.ac.outerloop.AcLoadFlowParameters;
import com.powsybl.openloadflow.graph.EvenShiloachGraphDecrementalConnectivityFactory;
import com.powsybl.openloadflow.network.FirstSlackBusSelector;
import com.powsybl.openloadflow.network.LfNetwork;
import com.powsybl.openloadflow.network.LfNetworkParameters;
import com.powsybl.openloadflow.network.impl.LfNetworkLoaderImpl;
import org.apache.commons.math3.util.Pair;

import java.util.*;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public abstract class AbstractShortCircuitEngine {
    protected final Network network;

    protected final ShortCircuitEngineParameters parameters;

    protected final List<LfNetwork> lfNetworks;

    public Map<ShortCircuitFault, ShortCircuitResult> resultsPerFault = new HashMap<>();

    public List<ShortCircuitResult> resultsAllBusses;

    protected List<ShortCircuitFault> solverFaultList; // list of faults provided to the solver (not including biphased common support faults)

    protected List<ShortCircuitFault> solverBiphasedFaultList; // list of biphased common support faults provided to the solver

    protected final AcLoadFlowParameters acLoadFlowParameters;

    public AbstractShortCircuitEngine(Network network, ShortCircuitEngineParameters parameters) {
        this.network = Objects.requireNonNull(network);
        this.parameters = Objects.requireNonNull(parameters);
        this.lfNetworks = LfNetwork.load(network, new LfNetworkLoaderImpl(), new LfNetworkParameters(new FirstSlackBusSelector()));
        this.acLoadFlowParameters = getAcLoadFlowParametersFromParam();
        ShortCircuitExtensions.add(network, lfNetworks, parameters.getAdditionalDataInfo());
    }

    protected AcLoadFlowParameters getAcLoadFlowParametersFromParam() {
        OpenLoadFlowParameters loadflowParametersExt = OpenLoadFlowParameters.get(parameters.getLoadFlowParameters());
        AcLoadFlowParameters acLoadFlowParameters = OpenLoadFlowParameters.createAcParameters(parameters.getLoadFlowParameters(), loadflowParametersExt, parameters.getMatrixFactory(), new EvenShiloachGraphDecrementalConnectivityFactory<>(), Reporter.NO_OP, false, false);
        return acLoadFlowParameters;
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
            ShortCircuitFault sc = new ShortCircuitFault(bus.getId(), 0., 0., type, false); //TODO : check validity of voltage levels if no connexity
            scfSystematic.add(sc);
        }
        parameters.setShortCircuitFaults(scfSystematic);
    }

    protected Pair<List<ShortCircuitFault>, List<ShortCircuitFault>> buildFaultListsFromInputs() {
        // We handle a pre-treatement of faults given in input:
        // - filtering faults because of some inconsistencies on the bus identification
        // - addition of info in each fault to ease the identification in LfNetwork of iidm info

        List<ShortCircuitFault> faultList = new ArrayList<>();
        List<ShortCircuitFault> biphasedFaultList = new ArrayList<>();
        Map<String, Pair<String, Integer >> tmpListBus1 = new HashMap<>();
        for (ShortCircuitFault scfe : parameters.getShortCircuitFaults()) {
            String busName = scfe.getBusLocation();
            String bus2Name = scfe.getBusLocationBiPhased();

            if (bus2Name.isEmpty()) { // TODO : adapt
                // TODO : put a condition that it is an unbalanced shortCircuit
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
                if (!tmpListBus1.containsKey(busName)) {
                    Pair<String, Integer> branchBus1FaultInfo = buildFaultBranchFromBusId(busName, network);
                    tmpListBus1.put(busName, branchBus1FaultInfo);
                }

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
        // TODO : improve with direct correspondence between iidm busses and lfBusses when available, because this loop is not very efficient
        Bus bus = tmpNetwork.getBusBreakerView().getBus(busId);
        String branchId = "";
        int branchSide = 0;
        boolean isFound = false;
        for (Branch branch : tmpNetwork.getBranches()) {
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
            System.out.println(" input CC Bus " +  busId + " could not be associated with a bipole");
        }

        return new Pair<>(branchId, branchSide);
    }

    protected static Pair<String, Integer> buildFaultT3WbranchFromBusId(String busId, Network tmpNetwork) {
        // TODO : improve with direct correspondence between iidm busses and lfBusses when available, because this loop is not very efficient
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
            System.out.println(" Bus " +  busId + " could not be associated with a tripole");
        }

        return new Pair<>(branchId, legNum);
    }

    public abstract void run();

}
