/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit.cgmes;

import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.iidm.network.Network;
import com.powsybl.incubator.simulator.shortcircuit.*;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
class MiniGridTest {

    private Map<String, String> busNameToId;

    @BeforeEach
    void setUp() {
        busNameToId = new HashMap<>();
        busNameToId.put("Bus1", "adee76cd-b2b9-48ac-8fd4-0d205a435f59");
        busNameToId.put("Bus9", "87c0d153-e308-4b2b-92a4-4fad53ab1ff9");
        busNameToId.put("Bus2", "b3d3b4ad-02af-4490-8748-70f6c9a23734");
        busNameToId.put("Bus8", "03163ede-7eec-457f-8641-365982227d7c");
        busNameToId.put("Bus3", "c8726716-e182-4373-b83e-8f60070078cb");
        busNameToId.put("Bus5", "37edd845-456f-4c3e-98d5-19af0c1cef1e");
        busNameToId.put("Bus6", "764e0b8a-f2af-4092-b6aa-b4a19e55db98");
        busNameToId.put("Bus7", "cd84fa40-ef63-422d-8ee0-d0a0f806719e");
        busNameToId.put("Bus10", "c7eda3d2-e92d-4935-8166-5e045d3de045");
        busNameToId.put("Bus11", "7f5515b2-ca6b-45af-93ee-f196686f0c66");
        busNameToId.put("Bus4", "c0adab49-d445-4609-a1a3-ebe4ef297cc8");
    }

    @Test
    void triphasedTest() {
        Properties parameters = new Properties();
        parameters.setProperty("iidm.import.cgmes.post-processors", CgmesShortCircuitImportPostProcessor.NAME);
        //TestGridModelResources testCgm = CgmesConformity1Catalog.miniBusBranch();
        Network network = Network.read(CgmesConformity1Catalog.miniBusBranch().dataSource(), parameters);
        ShortCircuitNormIec shortCircuitNormIec = new ShortCircuitNormIec();

        shortCircuitNormIec.applyNormToNetwork(network); // this modifies the characteristics of some iidm equipments

        // TODO : use it for unit tests
        //double xG1 = 0.4116;
        //double rG1 = 0.002;
        //double xG2 = 0.1764;
        //double rG2 = 0.005;
        //double rG3 = 0.01779;
        //double xG3 = 1.089623;
        //double kG3 = 0.988320;
        //double kt56 = 0.974870;
        //double ktab = 0.928072;
        //double ktac = 0.985856;
        //double ktbc = 1.002890;
        //double xQ1 = 6.319335;
        //double rQ1 = 0.631933;
        //double xQ2 = 4.344543;
        //double rQ2 = 0.434454;
        //double pM1ScLoad = 2.89929;
        //double qM1ScLoad = 28.9929;
        //double pM2ScLoad = 2.40235;
        //double qM2ScLoad = 24.0235;
        //double kG1 = 0.99597;
        //double kG2 = 0.876832;

        List<ShortCircuitFault> faultList = new ArrayList<>();
        ShortCircuitFault sc1 = new ShortCircuitFault(busNameToId.get("Bus1"), "sc1", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc1);
        ShortCircuitFault sc2 = new ShortCircuitFault(busNameToId.get("Bus2"), "sc2", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc2);
        ShortCircuitFault sc3 = new ShortCircuitFault(busNameToId.get("Bus3"), "sc3", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc3);
        ShortCircuitFault sc4 = new ShortCircuitFault(busNameToId.get("Bus4"), "sc4", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc4);
        ShortCircuitFault sc5 = new ShortCircuitFault(busNameToId.get("Bus5"), "sc5", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc5);
        ShortCircuitFault sc6 = new ShortCircuitFault(busNameToId.get("Bus6"), "sc6", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc6);
        ShortCircuitFault sc7 = new ShortCircuitFault(busNameToId.get("Bus7"), "sc7", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc7);
        ShortCircuitFault sc8 = new ShortCircuitFault(busNameToId.get("Bus8"), "sc8", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc8);

        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.SUB_TRANSIENT;
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);
        MatrixFactory refmatrixFactory = new DenseMatrixFactory();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, refmatrixFactory, ShortCircuitEngineParameters.AnalysisType.SELECTIVE, faultList, true, ShortCircuitEngineParameters.VoltageProfileType.NOMINAL, false, periodType, shortCircuitNormIec);
        ShortCircuitBalancedEngine scbEngine = new ShortCircuitBalancedEngine(network, scbParameters);

        scbEngine.run();
        List<Double> values = new ArrayList<>();
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> res : scbEngine.getResultsPerFault().entrySet()) {
            values.add(res.getValue().getIk().getKey());
        }

        // I"k = 1/sqrt(3) * cmax * Un /(Zeq)
        assertEquals(40.64478476116188, values.get(0), 0.001); // bus 1 : expected in IEC doc = 40.6447 kA and in CGMES doc = 40.6375 kA
        assertEquals(31.783052222534174, values.get(1), 0.01); // bus 2 : expected in doc =  31.7831 kA  and in CGMES doc = 31.6939 kA
        assertEquals(19.672955775750143, values.get(2), 0.001); // bus 3 : expected in doc =  19.673 kA and in CGMES doc = 19.5243 kA
        assertEquals(16.227655866910894, values.get(3), 0.001); // bus 4 : expected in doc =  16.2277 kA and in CGMES doc = 16.1686 kA
        assertEquals(33.18941481677016, values.get(4), 0.01); // bus 5 : expected in doc =  33.1894 kA and in CGMES doc = 33.0764 kA
        assertEquals(37.56287899040728, values.get(5), 0.1); // bus 6 : expected in doc =  37.5629 kA and in CGMES doc = 37.5547 kA
        assertEquals(25.589463480212533, values.get(6), 0.1); // bus 7 : expected in doc =  25.5895 kA and in CGMES doc = 25.5862 kA
        assertEquals(13.577771545200052, values.get(7), 0.001); // bus 8 : expected in doc =  13.5778 kA and in CGMES doc = 13.632 kA

    }

    @Test
    void monophasedTest() {

        Properties parameters = new Properties();
        parameters.setProperty("iidm.import.cgmes.post-processors", CgmesShortCircuitImportPostProcessor.NAME);
        //TestGridModelResources testCgm = CgmesConformity1Catalog.miniBusBranch();
        Network network = Network.read(CgmesConformity1Catalog.miniBusBranch().dataSource(), parameters);
        ShortCircuitNormIec shortCircuitNormIec = new ShortCircuitNormIec();

        shortCircuitNormIec.applyNormToNetwork(network); // this modifies the characteristics of some iidm equipments

        // FIXME : check if there is a mistake in the CGMES input data example:
        //  for T4, by definition :
        //  ro_mvk = Kt_bc * (ro_b + ro_c)
        //  Ro_T / R_T = (ro_b + ro_c) / r_ab = 1.0
        //  then ro_mvk = Kt_bc * r_ab * Ro_T / R_T = 0.107281
        //  the input values ro_b and ro_c do not give ro_mvk = 0.107281
        //  for Ro_T / R_T = 1.0 , does it mean that r_b =? ro_b and r_c =? ro_c, maybe not...
        //  keeping the values provided in input, short circuit at bus2 varies from 15.9722 kA ( = the reference) to 15.981 kA
        //  if we want to keep the reference result, we need to modify the ratio of ro_b/r_b and ro_c/r_c equal to : double coeffRoT4 = 0.107281 / (rT4b + rT4c) 120. /120. ;

        List<ShortCircuitFault> faultList = new ArrayList<>();
        ShortCircuitFault sc1 = new ShortCircuitFault(busNameToId.get("Bus2"), "sc1", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);
        faultList.add(sc1);
        ShortCircuitFault sc2 = new ShortCircuitFault(busNameToId.get("Bus3"), "sc2", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);
        faultList.add(sc2);
        ShortCircuitFault sc3 = new ShortCircuitFault(busNameToId.get("Bus4"), "sc3", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);
        faultList.add(sc3);
        ShortCircuitFault sc4 = new ShortCircuitFault(busNameToId.get("Bus5"), "sc4", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);
        faultList.add(sc4);

        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.SUB_TRANSIENT;
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);
        MatrixFactory matrixFactory = new DenseMatrixFactory();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, ShortCircuitEngineParameters.AnalysisType.SELECTIVE, faultList, false, ShortCircuitEngineParameters.VoltageProfileType.NOMINAL, false, periodType, shortCircuitNormIec);
        ShortCircuitUnbalancedEngine scbEngine = new ShortCircuitUnbalancedEngine(network, scbParameters);

        scbEngine.run();
        Map<String, Double> values = new HashMap<>();
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> res : scbEngine.getResultsPerFault().entrySet()) {
            values.put(res.getKey().getFaultId(), res.getValue().getIk().getKey());
        }

        //I"k = sqrt(3) * cmax * Un /(Zeq)
        assertEquals(15.98099, values.get("sc1"), 0.001); // bus 2 : expected doc value : 15.9722 kA FIXME : corrected reference = 15.98099 kA
        assertEquals(10.410558286260768, values.get("sc2"), 0.001); // bus 3 : expected doc value : 10.4106 kA
        assertEquals(9.049787523396647, values.get("sc3"), 0.001); // bus 4 : expected doc value : 9.0498 kA
        assertEquals(17.0467196, values.get("sc4"), 0.001); // bus 5 : expected doc value : 17.0452 kA FIXME : corrected reference = 17.0467196 kA

    }
}
