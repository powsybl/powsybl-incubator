/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.openloadflow.ac.outerloop.AcLoadFlowParameters;
import com.powsybl.openloadflow.network.FirstSlackBusSelector;
import com.powsybl.openloadflow.network.LfNetwork;
import com.powsybl.openloadflow.network.LfNetworkParameters;
import com.powsybl.openloadflow.network.impl.LfNetworkLoaderImpl;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class TheveninEquivalent {

    private final TheveninEquivalentParameters parameters;

    private final Network network;

    private final List<LfNetwork> networks;

    public  AdmittanceLinearResolution admittanceLinearResolution;

    public TheveninEquivalent(Network network, TheveninEquivalentParameters parameters) {
        this.network = Objects.requireNonNull(network);
        this.networks = LfNetwork.load(network, new LfNetworkLoaderImpl(), new LfNetworkParameters(new FirstSlackBusSelector()));
        this.parameters = Objects.requireNonNull(parameters);
    }

    public AdmittanceLinearResolution getAdmittanceLinearResolution() {
        return admittanceLinearResolution;
    }

    public void run() {

        AdmittanceLinearResolutionParameters parameters = generateAdmittanceLinearResolutionParam();
        AdmittanceLinearResolution thEq = new AdmittanceLinearResolution(network, parameters);
        thEq.run();

        this.admittanceLinearResolution = thEq;

    }

    private static Pair<String, Integer > buildFaultBranchFromBusId(String busId, Network tmpNetwork) {
        // TODO : remove code duplication with AbstractShortCircuitEngine
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

    private static Pair<String, Integer> buildFaultT3WbranchFromBusId(String busId, Network tmpNetwork) {
        // TODO : remove code duplication with AbstractShortCircuitEngine
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

    private AdmittanceLinearResolutionParameters generateAdmittanceLinearResolutionParam() {

        boolean voltageUpdate = parameters.isVoltageUpdate(); // TODO: check that in example, no voltage update is asked

        AcLoadFlowParameters acLoadFlowParameters = parameters.getAcLoadFlowParameters();

        AdmittanceEquationSystem.AdmittanceVoltageProfileType admittanceVoltageProfileType = AdmittanceEquationSystem.AdmittanceVoltageProfileType.CALCULATED; //TODO: put nominal if Thevenin requires nominal voltage use
        if (parameters.getTheveninVoltageProfileType() == TheveninEquivalentParameters.TheveninVoltageProfileType.NOMINAL) {
            admittanceVoltageProfileType = AdmittanceEquationSystem.AdmittanceVoltageProfileType.NOMINAL;
        }

        AdmittanceEquationSystem.AdmittancePeriodType periodType = AdmittanceEquationSystem.AdmittancePeriodType.ADM_TRANSIENT;
        if (parameters.getTheveninPeriodType() == TheveninEquivalentParameters.TheveninPeriodType.THEVENIN_SUB_TRANSIENT) {
            periodType = AdmittanceEquationSystem.AdmittancePeriodType.ADM_SUB_TRANSIENT;
        } else if (parameters.getTheveninPeriodType() == TheveninEquivalentParameters.TheveninPeriodType.THEVENIN_STEADY_STATE) {
            periodType = AdmittanceEquationSystem.AdmittancePeriodType.ADM_STEADY_STATE;
        }

        List<ShortCircuitFault> faultList = new ArrayList<>();
        for (ShortCircuitFault scfe : parameters.getFaults()) {
            String busName = scfe.getBusLocation();
            Pair<String, Integer > branchFaultInfo = buildFaultBranchFromBusId(busName, network);

            if (branchFaultInfo.getKey().equals("")) {
                // Bus not found in branches, try three windings transformers
                branchFaultInfo = buildFaultT3WbranchFromBusId(busName, network);
            }
            if (scfe.getType() == ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND) {
                scfe.setIidmBusInfo(branchFaultInfo);
                faultList.add(scfe);
            }
        }

        return new AdmittanceLinearResolutionParameters(acLoadFlowParameters, parameters.getMatrixFactory(),
                faultList, voltageUpdate, admittanceVoltageProfileType, periodType,
                AdmittanceEquationSystem.AdmittanceType.ADM_THEVENIN, parameters.isTheveninIgnoreShunts(),
                parameters.getAdditionalDataInfo(), parameters.getNorm());
    }
}
