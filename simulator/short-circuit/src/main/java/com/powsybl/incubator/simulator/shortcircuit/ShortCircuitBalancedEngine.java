/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.Network;
import com.powsybl.incubator.simulator.util.*;
import com.powsybl.math.matrix.DenseMatrix;
import com.powsybl.openloadflow.network.LfBus;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitBalancedEngine extends AbstractShortCircuitEngine {

    public ShortCircuitBalancedEngine(Network network, ShortCircuitEngineParameters parameters) {
        super(network, parameters);
    }

    @Override
    public void run() { //can handle both selective and systematic analysis with one single matrix inversion

        // building a contingency list with all voltage levels
        // TODO : generate a list with busses
        if (parameters.getAnalysisType() == ShortCircuitEngineParameters.AnalysisType.SYSTEMATIC) {
            buildSystematicList(ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        }

        List<ShortCircuitFault> faultList = new ArrayList<>();
        for (ShortCircuitFault scfe : parameters.getShortCircuitFaults()) {
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

        AdmittanceLinearResolutionParameters linearResolutionParameters = new AdmittanceLinearResolutionParameters(acLoadFlowParameters,
                parameters.getMatrixFactory(), faultList, parameters.isVoltageUpdate(), getAdmittanceVoltageProfileTypeFromParam(), getAdmittancePeriodTypeFromParam(), AdmittanceEquationSystem.AdmittanceType.ADM_THEVENIN,
                parameters.isIgnoreShunts(), parameters.getAdditionalDataInfo(), parameters.getNorm());

        AdmittanceLinearResolution directResolution = new AdmittanceLinearResolution(network, shortCircuitNetwork,  linearResolutionParameters);

        directResolution.run();

        //Build the ShortCircuit results using the Thevenin computation results
        resultsPerFault = new LinkedHashMap<>();
        resultsAllBusses  = new ArrayList<>(); // TODO : see which one to keep depending onthe improvement of the short circuit API
        processAdmittanceLinearResolutionResults(directResolution);

    }

    protected void processAdmittanceLinearResolutionResults(AdmittanceLinearResolution directResolution) {

        for (AdmittanceLinearResolution.AdmittanceLinearResolutionResult linearResolutionResult : directResolution.results) {
            LfBus bus = linearResolutionResult.getBus();
            String tmpBusId = bus.getId();
            ShortCircuitFault scf = null; // = parameters.getShortCircuitFaults().get(tmpVl);
            for (ShortCircuitFault scfe : parameters.getShortCircuitFaults()) { //TODO : improve to avoid double loop
                if (bus.getId().equals(scfe.getLfBusInfo())) {
                    scf = scfe;
                }
            }

            // If = Eth / (Zth + Zf) gives:
            //
            //        ethi*(xf+xth) + ethr*(rf+rth) + j*[ethi*(rf+rth) - ethr*(xf+xth)]
            // If = --------------------------------------------------------------------
            //                          (rf+rth)² + (xf+xth)²
            //
            double vxInit = linearResolutionResult.getEthr();
            double vyInit = linearResolutionResult.getEthi();

            double rth = linearResolutionResult.getRthz11();
            double xth = linearResolutionResult.getXthz12();
            double rf = scf.getZfr();
            double xf = scf.getZfi();

            double denom = (rf + rth) * (rf + rth) + (xf + xth) * (xf + xth);
            double ifr = (vyInit * (xf + xth) + vxInit * (rf + rth)) / denom;
            double ifi = (vyInit * (rf + rth) - vxInit * (xf + xth)) / denom;
            // The post-fault voltage values at faulted bus are computed as follow :
            // [Vr] = [Vr_init] - ifr * [e_dVr] + ifi * [e_dVi]
            // [Vi] = [Vi_init] - ifr * [e_dVi] - ifi * [e_dVr]
            double dvr = -ifr * linearResolutionResult.getEnBus().get(0, 0) + ifi * linearResolutionResult.getEnBus().get(1, 0);
            double dvi = -ifr * linearResolutionResult.getEnBus().get(1, 0) - ifi * linearResolutionResult.getEnBus().get(0, 0);

            ShortCircuitResult res = new ShortCircuitResult(bus.getId(), tmpBusId, scf, ifr, ifi, bus, rth, xth, vxInit, vyInit, dvr, dvi, parameters.getMatrixFactory(), shortCircuitNetwork, linearResolutionResult.getEqSysFeeders(), parameters.getNorm());
            if (parameters.voltageUpdate) {
                //we get the lfNetwork to process the results
                res.addLfNetwork(directResolution.lfNetworkResult);

                res.setVoltageProfileUpdate();
                // The post-fault voltage values are computed as follow :
                // [Vr] = [Vr_init] - ifr * [e_dVr] + ifi * [e_dVi]
                // [Vi] = [Vi_init] - ifr * [e_dVi] - ifi * [e_dVr]
                // we compute the delta values to be added to Vinit if we want the post-fault voltage :
                int nbBusses = directResolution.lfNetworkResult.getBuses().size();
                List<DenseMatrix> busNum2Dv = res.createEmptyFortescueVoltageVector(nbBusses);

                for (Map.Entry<Integer, DenseMatrix> vd : linearResolutionResult.getDv().entrySet()) {
                    int busNum = vd.getKey();
                    double edVr = vd.getValue().get(0, 0);
                    double edVi = vd.getValue().get(1, 0);
                    //System.out.println(" dVth(" + vdr.getKey() + ") = " + edVr + " + j(" + edVi + ")");
                    double deltaVr = -ifr * edVr + ifi * edVi;
                    double deltaVi = -ifr * edVi - ifi * edVr;

                    res.fillVoltageInFortescueVector(busNum, deltaVr, deltaVi);
                }
            }

            resultsPerFault.put(scf, res);
            resultsAllBusses.add(res);
            res.updateVoltageResult();
        }
    }

}
