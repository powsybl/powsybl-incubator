/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit.cgmes;

import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
//import com.powsybl.cgmes.model.test.TestGridModelResources;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuit;
import com.powsybl.incubator.simulator.shortcircuit.*;
import com.powsybl.incubator.simulator.util.extensions.iidm.GeneratorShortCircuit2;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class MiniGridTest {

    @Test
    void bcTest() {
        Properties parameters = new Properties();
        parameters.setProperty("iidm.import.cgmes.post-processors", CgmesShortCircuitImportPostProcessor.NAME);
        //TestGridModelResources testCgm = CgmesConformity1Catalog.miniBusBranch();
        Network network = Importers.loadNetwork(CgmesConformity1Catalog.miniBusBranch().dataSource(), parameters);
        ShortCircuitNormIec shortCircuitNormIec = new ShortCircuitNormIec();

        Map<String, String> busNameToId = new HashMap<>();
        busNameToId.put("Bus1", "adee76cd-b2b9-48ac-8fd4-0d205a435f59"); //OK
        busNameToId.put("Bus9", "87c0d153-e308-4b2b-92a4-4fad53ab1ff9"); //OK
        busNameToId.put("Bus2", "b3d3b4ad-02af-4490-8748-70f6c9a23734"); //OK
        busNameToId.put("Bus8", "03163ede-7eec-457f-8641-365982227d7c"); //OK
        busNameToId.put("Bus3", "c8726716-e182-4373-b83e-8f60070078cb"); //OK
        busNameToId.put("Bus5", "37edd845-456f-4c3e-98d5-19af0c1cef1e"); //OK
        busNameToId.put("Bus6", "764e0b8a-f2af-4092-b6aa-b4a19e55db98");
        busNameToId.put("Bus7", "cd84fa40-ef63-422d-8ee0-d0a0f806719e");
        busNameToId.put("Bus10", "c7eda3d2-e92d-4935-8166-5e045d3de045");
        busNameToId.put("Bus11", "7f5515b2-ca6b-45af-93ee-f196686f0c66");
        busNameToId.put("Bus4", "c0adab49-d445-4609-a1a3-ebe4ef297cc8");

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
        //double coeffRoQ2 = 6.6; // TODO : set real value
        //double coeffXoQ2 = 3.3; // TODO : set real value
        //double pM1ScLoad = 2.89929;
        //double qM1ScLoad = 28.9929;
        //double pM2ScLoad = 2.40235;
        //double qM2ScLoad = 24.0235;
        //double kG1 = 0.99597;
        //double kG2 = 0.876832;

        // G1, G2
        // Attribute values for G1 et G2 taking into account that T1 and T2 are modeled separately
        Generator g1 = network.getGenerator(genNameToId.get("G1"));
        Generator g2 = network.getGenerator(genNameToId.get("G2"));

        // T1 and T2
        TwoWindingsTransformer t1 = network.getTwoWindingsTransformer(t2wNameToId.get("T1"));
        TwoWindingsTransformer t2 = network.getTwoWindingsTransformer(t2wNameToId.get("T2"));

        double kG1 = shortCircuitNormIec.getKsAggregatedTfoGen(network, t1); // from t1 we get G1 values to get KS
        double kG2 = shortCircuitNormIec.getKsAggregatedTfoGen(network, t2);

        adjustWithKg(g1, kG1);
        adjustWithKg(g2, kG2);

        t1.setX(t1.getX() * kG1);
        t2.setX(t2.getX() * kG2);

        t1.setR(t1.getR() * kG1);
        t2.setR(t2.getR() * kG2);

        // G3
        Generator g3 = network.getGenerator(genNameToId.get("G3"));
        double kG3Iec = shortCircuitNormIec.getKg(g3);
        adjustWithKg(g3, kG3Iec);

        // Q1, Q2
        Generator q1 = network.getGenerator(genNameToId.get("Q1"));
        Generator q2 = network.getGenerator(genNameToId.get("Q2"));
        shortCircuitNormIec.adjustGenValuesWithFeederInputs(q1);
        shortCircuitNormIec.adjustGenValuesWithFeederInputs(q2);

        // M1, M2a and M2b
        // FIXME : We have an issue : CGMES asynchronous machines are imported as loads in iidm.
        //  P and Q of the load used for the load-flow does not match with the equivalent Impedance deduced from the parameters of the asynchronous machine
        //  P and Q are artificially modified to match the impedance used in the short-circuit calculation
        // Impedance to be applied at M1 : Z_M1 = 0.341497 + j3.414968
        // Total Impedance to be applied at M2 : Z_M2 = 0.412137 + j4.121368
        //using formula P(MW) = Re(Z) * |V|² / |Z|² and Q(MVA) = Im(Z) * |V|² / |Z|²

        Load m1Load = network.getLoad(loadNameToId.get("M1orM3"));
        Load m2aLoad = network.getLoad(loadNameToId.get("M2a"));
        Load m2bLoad = network.getLoad(loadNameToId.get("M2b"));
        shortCircuitNormIec.generateZofLoadfromAsynch(m1Load);
        shortCircuitNormIec.generateZofLoadfromAsynch(m2aLoad);
        shortCircuitNormIec.generateZofLoadfromAsynch(m2bLoad);

        //T5 and T6
        TwoWindingsTransformer t5 = network.getTwoWindingsTransformer(t2wNameToId.get("T5"));
        TwoWindingsTransformer t6 = network.getTwoWindingsTransformer(t2wNameToId.get("T6"));

        double kt5Iec = shortCircuitNormIec.getKtT2W(t5);
        double kt6Iec = shortCircuitNormIec.getKtT2W(t6);

        t5.setX(t5.getX() * kt5Iec);
        t6.setX(t6.getX() * kt6Iec);

        t5.setR(t5.getR() * kt5Iec);
        t6.setR(t6.getR() * kt6Iec);

        //T3 and T4
        ThreeWindingsTransformer t3 = network.getThreeWindingsTransformer(t3wNameToId.get("T3"));
        ThreeWindingsTransformer t4 = network.getThreeWindingsTransformer(t3wNameToId.get("T4"));

        List<Double> resultT3 = shortCircuitNormIec.getKtT3Wi(t3); // table contains vector [ kTaR; kTaX; kTbR; kTbX; kTcR; kTcX ]
        shortCircuitNormIec.adjustWithKt3W(t3, resultT3);

        List<Double> resultT4 = shortCircuitNormIec.getKtT3Wi(t4);
        shortCircuitNormIec.adjustWithKt3W(t4, resultT4);

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
        MatrixFactory matrixFactory = new DenseMatrixFactory();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, ShortCircuitEngineParameters.AnalysisType.SELECTIVE, faultList, true, ShortCircuitEngineParameters.VoltageProfileType.NOMINAL, false, periodType, shortCircuitNormIec);
        ShortCircuitBalancedEngine scbEngine = new ShortCircuitBalancedEngine(network, scbParameters);

        scbEngine.run();
        List<Double> values = new ArrayList<>();
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> res : scbEngine.getResultsPerFault().entrySet()) {
            values.add(res.getValue().getIk().getKey());
        }

        //Path filePath = Paths.get("C:\\JBH\\tmp_CGMES\\tutu.xiidm");
        //NetworkXml.write(network, filePath);

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

    public void adjustWithKg(Generator gen, double kg) {
        // This is a temporary function to multiply with kg
        // should disappear when the norm will be properly applied
        GeneratorShortCircuit extension = gen.getExtension(GeneratorShortCircuit.class);
        if (extension != null) {
            extension.setDirectSubtransX(extension.getDirectSubtransX() * kg);
            extension.setDirectTransX(extension.getDirectTransX() * kg);
        }
        GeneratorShortCircuit2 extensions2 = gen.getExtension(GeneratorShortCircuit2.class);
        if (extensions2 != null) {
            extensions2.setSubTransRd(extensions2.getSubTransRd() * kg);
            extensions2.setTransRd(extensions2.getTransRd() * kg);
        }
    }

}
