/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit.cgmes;

import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.incubator.simulator.shortcircuit.*;
import com.powsybl.incubator.simulator.util.extensions.iidm.*;
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
public class MiniGridTest {

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
        Network network = Importers.loadNetwork(CgmesConformity1Catalog.miniBusBranch().dataSource(), parameters);
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
        Network network = Importers.loadNetwork(CgmesConformity1Catalog.miniBusBranch().dataSource(), parameters);
        ShortCircuitNormIec shortCircuitNormIec = new ShortCircuitNormIec();

        shortCircuitNormIec.applyNormToNetwork(network); // this modifies the characteristics of some iidm equipments

        Map<String, String> genNameToId = new HashMap<>();
        genNameToId.put("Q1", "089c1945-4101-487f-a557-66c013b748f6");
        genNameToId.put("Q2", "3de9e1ad-4562-44df-b268-70ed0517e9e7");
        genNameToId.put("G2", "2970a2b7-b840-4e9c-b405-0cb854cd2318");
        genNameToId.put("G1", "ca67be42-750e-4ebf-bfaa-24d446e59a22");
        genNameToId.put("G3", "392ea173-4f8e-48fa-b2a3-5c3721e93196");

        Map<String, String> loadNameToId = new HashMap<>();
        loadNameToId.put("M1orM3", "062ece1f-ade5-4d20-9c3a-fd8f12d12ec1");
        loadNameToId.put("M2a", "ba62884d-8800-41a8-9c26-698297d7ebaa");
        loadNameToId.put("M2b", "f184d87b-5565-45ee-89b4-29e8a42d3ad1");

        Map<String, String> t2wNameToId = new HashMap<>();
        t2wNameToId.put("T2", "f1e72854-ec35-46e9-b614-27db354e8dbb");
        t2wNameToId.put("T1", "813365c3-5be7-4ef0-a0a7-abd1ae6dc174");
        t2wNameToId.put("T5", "ceb5d06a-a7ff-4102-a620-7f3ea5fb4a51");
        t2wNameToId.put("T6", "6c89588b-3df5-4120-88e5-26164afb43e9");

        Map<String, String> t3wNameToId = new HashMap<>();
        t3wNameToId.put("T4", "411b5401-0a43-404a-acb4-05c3d7d0c95c");
        t3wNameToId.put("T3", "5d38b7ed-73fd-405a-9cdb-78425e003773");

        Map<String, String> lineNameToId = new HashMap<>();
        lineNameToId.put("L5", "1e7f52a9-21d0-4ebe-9a8a-b29281d5bfc9");
        lineNameToId.put("L6", "56757c2b-550e-4843-886a-ed193f6eb21e");
        lineNameToId.put("L4", "e95a6228-ceac-4f0a-8b52-d35367b364dc");
        lineNameToId.put("L1", "d5d1cc4a-6297-4386-b6ce-16dc26f15feb");
        lineNameToId.put("L2", "efdd7f46-67e6-46e3-9dcd-a3b6f8c613a4");
        lineNameToId.put("L3a", "35df6abe-3087-4c27-a90a-12b5065333f3");
        lineNameToId.put("L3b", "05597934-b248-491e-803a-68ce6290f502");

        double xG1 = 26.336676;
        double rG1 = 0.498795;
        double coeffRoG1 = 0.439059 / rG1; // grounded = true

        //double coeffXoG1 = 13.340874 / xG1;

        double xL1 = 7.8;
        double rL1 = 2.4;
        double coeffRoL1 = 6.4 / rL1;
        double coeffXoL1 = 25.2 / xL1;

        double xL2 = 3.9;
        double rL2 = 1.2;
        double coeffRoL2 = 3.2 / rL2;
        double coeffXoL2 = 12.6 / xL2;

        double xL3 = 0.975; // TODO : check if this 2 times or only once
        double rL3 = 0.3;
        double coeffRoL3 = 1.3 / rL3;
        double coeffXoL3 = 4.65 / xL3;

        double xL4 = 3.88;
        double rL4 = 0.96;
        double coeffRoL4 = 2.2 / rL4;
        double coeffXoL4 = 11. / xL4;

        double xL5 = 5.79;
        double rL5 = 1.8;
        double coeffRoL5 = 3.3 / rL5;
        double coeffXoL5 = 16.5 / xL5;

        double xL6 = 0.086;
        double rL6 = 0.082;
        double coeffRoL6 = 1.0; // not used
        double coeffXoL6 = 1.0; // not used

        double rhoB2 = 1. / (120. * 120.);
        double rT3a = 0.045714 * rhoB2;
        double xT3a = 8.0969989 * rhoB2;
        double rT3b = 0.053563 * rhoB2;
        double xT3b = -0.079062 * rhoB2;
        double rT3c = 0.408560 * rhoB2;
        double xT3c = 20.292035 * rhoB2;

        double coeffRoT4 = 0.107281 / (rT3b + rT3c) * rhoB2;
        double coeffXoT4 = 18.195035 / (xT3b + xT3c) * rhoB2;

        double coeffF2Ro = 6.6;
        double coeffF2Xo = 3.3;

        //G1 homopolar data
        Generator g1 = network.getGenerator(genNameToId.get("G1"));
        GeneratorShortCircuit2 g1Extensions2 = g1.getExtension(GeneratorShortCircuit2.class);

        double coeffXoG1 = 1.0;

        g1Extensions2.setCoeffRo(coeffRoG1);
        g1Extensions2.setCoeffXo(coeffXoG1); // TODO : check the influence of separated T1
        g1Extensions2.setToGround(false);

        Generator q2 = network.getGenerator(genNameToId.get("Q2"));
        GeneratorShortCircuit2 q2Extensions2 = q2.getExtension(GeneratorShortCircuit2.class);

        q2Extensions2.setCoeffRo(coeffF2Ro);
        q2Extensions2.setCoeffXo(coeffF2Xo);
        q2Extensions2.setToGround(true);

        // Lines
        Line l1 = network.getLine(lineNameToId.get("L1"));
        Line l2 = network.getLine(lineNameToId.get("L2"));
        Line l3a = network.getLine(lineNameToId.get("L3a"));
        Line l3b = network.getLine(lineNameToId.get("L3b"));
        Line l4 = network.getLine(lineNameToId.get("L4"));
        Line l5 = network.getLine(lineNameToId.get("L5"));
        Line l6 = network.getLine(lineNameToId.get("L6"));

        // Tfo 3w
        ThreeWindingsTransformer twt3 = network.getThreeWindingsTransformer(t3wNameToId.get("T3"));
        ThreeWindingsTransformer twt4 = network.getThreeWindingsTransformer(t3wNameToId.get("T4"));

        twt3.newExtension(ThreeWindingsTransformerShortCircuitAdder.class)
                .withLeg1ConnectionType(LegConnectionType.Y_GROUNDED)
                .withLeg2ConnectionType(LegConnectionType.Y)
                .withLeg3ConnectionType(LegConnectionType.DELTA)
                .add();

        twt4.newExtension(ThreeWindingsTransformerShortCircuitAdder.class)
                .withLeg1FreeFluxes(true)
                .withLeg1ConnectionType(LegConnectionType.Y)
                .withLeg2FreeFluxes(true)
                .withLeg2CoeffRo(coeffRoT4)
                .withLeg2CoeffXo(coeffXoT4)
                .withLeg2ConnectionType(LegConnectionType.Y_GROUNDED)
                .withLeg3FreeFluxes(true)
                .withLeg3CoeffRo(coeffRoT4)
                .withLeg3CoeffXo(coeffXoT4)
                .withLeg3ConnectionType(LegConnectionType.DELTA)
                .add();

        // tfo 2w
        TwoWindingsTransformer t5 = network.getTwoWindingsTransformer(t2wNameToId.get("T5"));
        TwoWindingsTransformer t6 = network.getTwoWindingsTransformer(t2wNameToId.get("T6"));
        TwoWindingsTransformer t1 = network.getTwoWindingsTransformer(t2wNameToId.get("T1"));
        TwoWindingsTransformer t2 = network.getTwoWindingsTransformer(t2wNameToId.get("T2"));

        System.out.println(" T3 : leg1 = " + twt3.getLeg1().getRatedU() + " leg2 = " + twt3.getLeg2().getRatedU() + " leg3 = " + twt3.getLeg3().getRatedU());
        System.out.println(" T4 : leg1 = " + twt4.getLeg1().getRatedU() + " leg2 = " + twt4.getLeg2().getRatedU() + " leg3 = " + twt4.getLeg3().getRatedU());
        System.out.println(" T6 : leg1 = " + t6.getRatedU1() + " leg2 = " + t6.getRatedU2());

        t5.newExtension(TwoWindingsTransformerShortCircuitAdder.class)
                .withLeg1ConnectionType(LegConnectionType.Y)
                .withLeg2ConnectionType(LegConnectionType.Y)
                .add();
        t6.newExtension(TwoWindingsTransformerShortCircuitAdder.class)
                .withLeg1ConnectionType(LegConnectionType.Y)
                .withLeg2ConnectionType(LegConnectionType.Y_GROUNDED)
                .add();

        double xo1ground = 66.;
        double uRatedhv = 115.;
        double uRatedlv = 21.;
        double coefT1 = (13.340874 + xo1ground) / t1.getX() * uRatedlv * uRatedlv / uRatedhv / uRatedhv;
        double coefRoT1 = 0.439059 / t1.getR() * uRatedlv * uRatedlv / uRatedhv / uRatedhv;
        System.out.println(" T1 : xT1 = " + t1.getX() + " rT1 = " + t1.getR() + " coef = " + coefT1 + " Urated1 = " + t1.getRatedU1() + " Urated2 = " + t1.getRatedU2() + "coefXoT1 = " + coefT1);

        t1.newExtension(TwoWindingsTransformerShortCircuitAdder.class)
                .withLeg1ConnectionType(LegConnectionType.Y_GROUNDED)
                .withLeg2ConnectionType(LegConnectionType.DELTA)
                .withCoeffXo(coefT1)
                .withCoeffRo(coefRoT1)
                .add();

        t2.newExtension(TwoWindingsTransformerShortCircuitAdder.class)
                .withLeg1ConnectionType(LegConnectionType.Y)
                .withLeg2ConnectionType(LegConnectionType.DELTA)
                .withCoeffXo(1.0)
                .withCoeffRo(1.0)
                .add();

        l1.newExtension(LineShortCircuitAdder.class)
                .withCoeffRo(coeffRoL1)
                .withCoeffXo(coeffXoL1)
                .add();
        l2.newExtension(LineShortCircuitAdder.class)
                .withCoeffRo(coeffRoL2)
                .withCoeffXo(coeffXoL2)
                .add();
        l3a.newExtension(LineShortCircuitAdder.class)
                .withCoeffRo(coeffRoL3)
                .withCoeffXo(coeffXoL3)
                .add();
        l3b.newExtension(LineShortCircuitAdder.class)
                .withCoeffRo(coeffRoL3)
                .withCoeffXo(coeffXoL3)
                .add();
        l4.newExtension(LineShortCircuitAdder.class)
                .withCoeffRo(coeffRoL4)
                .withCoeffXo(coeffXoL4)
                .add();
        l5.newExtension(LineShortCircuitAdder.class)
                .withCoeffRo(coeffRoL5)
                .withCoeffXo(coeffXoL5)
                .add();
        l6.newExtension(LineShortCircuitAdder.class)
                .withCoeffRo(coeffRoL6)
                .withCoeffXo(coeffXoL6)
                .add();

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
        assertEquals(15.9722, values.get("sc1"), 0.001); // bus 2 : expected doc value : 15.9722 kA
        assertEquals(10.410558286260768, values.get("sc2"), 0.001); // bus 3 : expected doc value : 10.4106 kA
        assertEquals(9.049787523396647, values.get("sc3"), 0.001); // bus 4 : expected doc value : 9.0498 kA
        assertEquals(17.0452, values.get("sc4"), 0.001); // bus 5 : expected doc value : 17.0452 kA

    }
}
