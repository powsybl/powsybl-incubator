/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuitAdder;
import com.powsybl.incubator.simulator.util.*;
import com.powsybl.incubator.simulator.util.extensions.AdditionalDataInfo;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.apache.commons.math3.util.Pair;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitMonophasedTest {

    private LoadFlowParameters parameters;

    private MatrixFactory matrixFactory;

    private LoadFlow.Runner loadFlowRunner;

    @BeforeEach
    void setUp() {
        parameters = new LoadFlowParameters();
        matrixFactory = new DenseMatrixFactory();
        loadFlowRunner = new LoadFlow.Runner(new OpenLoadFlowProvider(matrixFactory));
    }

    @Test
    void shortCircuitIec31Mono() {

        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);

        Pair<Network, AdditionalDataInfo> result = ReferenceNetwork.createShortCircuitIec31();
        Network network = result.getKey();

        AdditionalDataInfo additionalDataInfo = result.getValue();

        MatrixFactory  matrixFactory = new DenseMatrixFactory();

        List<ShortCircuitFault> faultList = new ArrayList<>();
        ShortCircuitFault sc1 = new ShortCircuitFault("B3", "sc1", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);
        faultList.add(sc1);

        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.SUB_TRANSIENT;
        ShortCircuitNormIec shortCircuitNormIec = new ShortCircuitNormIec();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, ShortCircuitEngineParameters.AnalysisType.SELECTIVE, faultList, false, ShortCircuitEngineParameters.VoltageProfileType.NOMINAL, false, periodType, additionalDataInfo, shortCircuitNormIec);
        ShortCircuitUnbalancedEngine scbEngine = new ShortCircuitUnbalancedEngine(network, scbParameters);

        scbEngine.run();
        List<Double> val = new ArrayList<>();
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> res : scbEngine.resultsPerFault.entrySet()) {
            val.add(res.getValue().getIk());
        }

        // here Icc = 1/sqrt(3)*Eth(pu)/Zth(pu100)*Sb100/Vb*1000
        // here new Icc = 1/sqrt(3)*Eth(pu)/Zth(pu100)*Sb100*1000
        // and I"k = sqrt(3) * cmax * Un /(Zeq) and expected I"k = 35.64 kA with some approximations on the impedance values

        //assertEquals(35.70435548244156, 3 * val.get(0) * 1.05 / 1000. / 0.4, 0.00001);
        assertEquals(35.70435548244156, 3 * val.get(0), 0.00001);

    }

    @Test
    void shortCircuitIecTestNetworkMono() {

        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);

        Pair<Network, AdditionalDataInfo> result = ReferenceNetwork.createShortCircuitIec31testNetwork();
        Network network = result.getKey();

        AdditionalDataInfo additionalDataInfo = result.getValue();

        MatrixFactory  matrixFactory = new DenseMatrixFactory();

        List<ShortCircuitFault> faultList = new ArrayList<>();
        ShortCircuitFault sc1 = new ShortCircuitFault("B2", "sc1", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);
        faultList.add(sc1);
        ShortCircuitFault sc2 = new ShortCircuitFault("B3", "sc2", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);
        faultList.add(sc2);
        ShortCircuitFault sc3 = new ShortCircuitFault("B4", "sc3", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);
        faultList.add(sc3);
        ShortCircuitFault sc4 = new ShortCircuitFault("B5", "sc4", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);
        faultList.add(sc4);

        // additional faults
        ShortCircuitFault sc5 = new ShortCircuitFault("B2", "sc5", 0., 0., ShortCircuitFault.ShortCircuitType.BIPHASED);
        faultList.add(sc5);
        ShortCircuitFault sc6 = new ShortCircuitFault("B3", "sc6", 0., 0., ShortCircuitFault.ShortCircuitType.BIPHASED_GROUND);
        faultList.add(sc6);

        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.TRANSIENT;
        ShortCircuitNormIec shortCircuitNormIec = new ShortCircuitNormIec();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, ShortCircuitEngineParameters.AnalysisType.SELECTIVE, faultList, false, ShortCircuitEngineParameters.VoltageProfileType.NOMINAL, false, periodType, additionalDataInfo, shortCircuitNormIec);
        ShortCircuitUnbalancedEngine scbEngine = new ShortCircuitUnbalancedEngine(network, scbParameters);

        scbEngine.run();
        Map<String, Double> values = new HashMap<>();
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> res : scbEngine.resultsPerFault.entrySet()) {
            values.put(res.getKey().getFaultId(), res.getValue().getIk());
        }

        //I"k = sqrt(3) * cmax * Un /(Zeq)
        assertEquals(15.9722, 3 * values.get("sc1"), 0.00001); // bus 2 : expected doc value : 15.9722 kA
        assertEquals(10.410558286260768, 3 * values.get("sc2"), 0.00001); // bus 3 : expected doc value : 10.4106 kA
        assertEquals(9.049787523396647, 3 * values.get("sc3"), 0.00001); // bus 4 : expected doc value : 9.0498 kA
        assertEquals(17.0452, 3 * values.get("sc4"), 0.00001); // bus 5 : expected doc value : 17.0452 kA

        // test to check that non monophased faults does not have an impact on monophased results
        assertEquals(57.48674948061683, 3 * values.get("sc5"), 0.00001); // biphased not in ref doc
        assertEquals(25.21494837446988, 3 * values.get("sc6"), 0.00001); // biphased not in ref doc

    }

    @Test
    void computeIoTestNew() {
        Pair<Network, AdditionalDataInfo> result = createGiard(NetworkFactory.findDefault());

        Network network = result.getKey();
        AdditionalDataInfo additionalDataInfo = result.getValue();

        LoadFlowResult resultntg = loadFlowRunner.run(network, parameters);

        Map<String, Boolean> machineToGround = new HashMap<>(); //true if the generating unit has its neutral linked to the ground
        machineToGround.put("GA", false);
        machineToGround.put("GB", true);

        additionalDataInfo.setMachineToGround(machineToGround);

        List<ShortCircuitFault> faultList = new ArrayList<>();
        ShortCircuitFault sc1 = new ShortCircuitFault("BP", "sc1", 0., 0., ShortCircuitFault.ShortCircuitType.MONOPHASED);

        faultList.add(sc1);

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.TRANSIENT;
        ShortCircuitNorm shortCircuitNorm = new ShortCircuitNorm();
        ShortCircuitEngineParameters scunbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, ShortCircuitEngineParameters.AnalysisType.SELECTIVE, faultList, true, ShortCircuitEngineParameters.VoltageProfileType.CALCULATED, false, periodType, additionalDataInfo, shortCircuitNorm);
        ShortCircuitUnbalancedEngine scunbEngine = new ShortCircuitUnbalancedEngine(network, scunbParameters);

        scunbEngine.run();

        //assertEquals(2.8286512112174034, scunbEngine.results.get(sc1).getIox() / Math.sqrt(3), 0.000001); // results changed with modification of input data
        //assertEquals(1.6331225382399293, scunbEngine.results.get(sc1).getIoy() / Math.sqrt(3), 0.000001);
        assertEquals(2.7099928109273916, scunbEngine.resultsPerFault.get(sc1).getIox() / Math.sqrt(3), 0.000001);
        assertEquals(1.5646150788908801, scunbEngine.resultsPerFault.get(sc1).getIoy() / Math.sqrt(3), 0.000001);

    }

    public static Pair<Network, AdditionalDataInfo>  createGiard(NetworkFactory networkFactory) {

        //   M1           P             M2            machine M1: isolated neutral with X'd1, Xmi1, Xmo1
        //  (~)-|---Xd1---|---Xd2---|--(~)            machine M2: grounded neutral through Xn, with X'd2, Xmi2, Xmo2
        //          Xi1       Xi2       |             In P, there is a direct monophased fault to the ground (phase 1)
        //          Xo1       Xo2       Xn
        //                              |
        //                            /////
        //
        // Direct schema:
        //                    P
        //  --X'd1--|---Xd1---|---Xd2---|--X'd2--           Xd'1 = 65 ohms  Xd'2 = 130 ohms
        //  ^                                   ^           Xd1 = 30 ohms   Xd2 = 15 ohms
        //  | Ed1                               | Ed2
        //  |                                   |
        // ///                                 ///
        //
        // Inverse            P
        //  --Xmi1--|---Xi1---|---Xi2---|--Xmi2--           Xi = Xd
        //  |                                   |
        //  |                                   |
        // ///                                 ///
        //
        // Homopolar          P
        //  --XmO1--|---Xo1---|---Xo2---|--Xmo2--           Xmo1 = Xd'1 / 3
        //                                      |           Xmo2 = Xd'2 / 3
        //                                    3.Xn          Xo1 = 3 * Xd1
        //                                      |           Xo2 = 3 * Xd2
        //                                     ///          Xn = 25 ohms

        Objects.requireNonNull(networkFactory);

        double pgen = 0.;
        double xd1 = 30.;
        double xd2 = 15.;
        double coeffXo1 = 3.;
        double coeffXo2 = 3.;
        double uac = 420.;
        double ubc = 410.;

        Network network = networkFactory.createNetwork("Giard exemple", "test");
        network.setCaseDate(DateTime.parse("2018-03-05T13:30:30.486+01:00"));
        Substation substationA = network.newSubstation()
                .setId("SA")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vlA = substationA.newVoltageLevel()
                .setId("VL_A")
                .setNominalV(380.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(500)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus busA = vlA.getBusBreakerView().newBus()
                .setId("BA")
                .add();
        busA.setV(uac).setAngle(0.);
        Generator genA = vlA.newGenerator()
                .setId("GA")
                .setBus(busA.getId())
                .setMinP(-10.0)
                .setMaxP(150)
                .setTargetP(pgen)
                .setTargetV(uac)
                .setVoltageRegulatorOn(true)
                .add();

        genA.newExtension(GeneratorShortCircuitAdder.class)
                .withDirectSubtransX(65)
                .withDirectTransX(65)
                .withStepUpTransformerX(0.)
                .add();

        Substation substationP = network.newSubstation()
                .setId("SP")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vlP = substationP.newVoltageLevel()
                .setId("VL_P")
                .setNominalV(380.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(500)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus busP = vlP.getBusBreakerView().newBus()
                .setId("BP")
                .add();
        busP.setV(413.0).setAngle(0);

        Substation substationB = network.newSubstation()
                .setId("SB")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vlB = substationB.newVoltageLevel()
                .setId("VL_B")
                .setNominalV(380.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(500)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus busB = vlB.getBusBreakerView().newBus()
                .setId("BB")
                .add();
        busB.setV(ubc).setAngle(0.);
        Generator genB = vlB.newGenerator()
                .setId("GB")
                .setBus(busB.getId())
                .setMinP(-10.0)
                .setMaxP(150)
                .setTargetP(pgen)
                .setTargetV(ubc)
                .setVoltageRegulatorOn(true)
                .add();

        genB.newExtension(GeneratorShortCircuitAdder.class)
                .withDirectSubtransX(130)
                .withDirectTransX(130)
                .withStepUpTransformerX(0.)
                .add();

        network.newLine()
                .setId("BA_BP")
                .setVoltageLevel1(vlA.getId())
                .setBus1(busA.getId())
                .setConnectableBus1(busA.getId())
                .setVoltageLevel2(vlP.getId())
                .setBus2(busP.getId())
                .setConnectableBus2(busP.getId())
                .setR(0.0)
                .setX(xd1)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();

        network.newLine()
                .setId("BP_BB")
                .setVoltageLevel1(vlP.getId())
                .setBus1(busP.getId())
                .setConnectableBus1(busP.getId())
                .setVoltageLevel2(vlB.getId())
                .setBus2(busB.getId())
                .setConnectableBus2(busB.getId())
                .setR(0.0)
                .setX(xd2)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();

        Map<String, Double> machineToTransRd = new HashMap<>(); //map to store the real part of the transient impedance
        Map<String, Double> machineToSubTransRd = new HashMap<>();
        Map<String, Boolean> machineToGround =  new HashMap<>();

        Map<String, AdditionalDataInfo.LegType> leg1type =  new HashMap<>();
        Map<String, AdditionalDataInfo.LegType> leg2type =  new HashMap<>();

        Map<String, Double> lineIdToCoeffRo = new HashMap<>();
        Map<String, Double> lineIdToCoeffXo = new HashMap<>();
        Map<String, Double> tfo2wIdToCoeffRo = new HashMap<>();
        Map<String, Double> tfo2wIdToCoeffXo = new HashMap<>();

        lineIdToCoeffXo.put("BA_BP", coeffXo1);
        lineIdToCoeffXo.put("BP_BB", coeffXo2);

        AdditionalDataInfo additionalDataInfo = new AdditionalDataInfo(machineToTransRd, machineToSubTransRd, machineToGround,
                leg1type, leg2type, lineIdToCoeffRo, lineIdToCoeffXo, tfo2wIdToCoeffRo, tfo2wIdToCoeffXo);

        Pair<Network, AdditionalDataInfo> result = new Pair<>(network, additionalDataInfo);
        return result;
    }
}
