/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuitAdder;
import com.powsybl.incubator.simulator.util.ReferenceNetwork;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.shortcircuit.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitBalancedTest {

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
    void computeIccTest() {
        Network nt2 = create2n(NetworkFactory.findDefault());
        LoadFlowResult resultnt2 = loadFlowRunner.run(nt2, parameters);

        List<ShortCircuitFault> tmpV = new ArrayList<>();
        ShortCircuitFault sc2 = new ShortCircuitFault("B2", true, "sc2", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        tmpV.add(sc2);

        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.TRANSIENT;

        ShortCircuitEngineParameters.VoltageProfileType vp = ShortCircuitEngineParameters.VoltageProfileType.CALCULATED;
        ShortCircuitEngineParameters.AnalysisType at = ShortCircuitEngineParameters.AnalysisType.SELECTIVE;

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        ShortCircuitNormCourcirc shortCircuitNormCourcirc = new ShortCircuitNormCourcirc();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, at, tmpV, true, vp, false, periodType, shortCircuitNormCourcirc);
        ShortCircuitBalancedEngine scbEngine = new ShortCircuitBalancedEngine(nt2, scbParameters);

        scbEngine.run();

        scbEngine.resultsPerFault.get(sc2).updateFeedersResult();

        assertEquals(-0.4316661015058293, scbEngine.resultsPerFault.get(sc2).getIdx(), 0.000001);
        assertEquals(-4.617486568622836, scbEngine.resultsPerFault.get(sc2).getIdy(), 0.000001);
        assertEquals(-0.5197272846952616, scbEngine.resultsPerFault.get(sc2).getIxFeeder("VL_1_0", "G1"), 0.000001);

    }

    @Test
    void openShortCircuitProvider2n() {

        //set up LF info
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);
        Network nt2 = create2n(NetworkFactory.findDefault());
        LoadFlowResult resultnt2 = LoadFlow.run(nt2, loadFlowParameters);

        //set up ShortCircuitProvider info
        ShortCircuitAnalysisProvider provider = new OpenShortCircuitProvider(new DenseMatrixFactory());
        ComputationManager cm = LocalComputationManager.getDefault();
        ShortCircuitParameters scp = new ShortCircuitParameters();

        //CompletableFuture<ShortCircuitAnalysisResult> scar = provider.run(nt2, scp, cm);
        List<Fault> faults = new ArrayList<>();
        BusFault bf1 = new BusFault("F1", "B1");
        BusFault bf2 = new BusFault("F2", "B2");
        faults.add(bf1);
        faults.add(bf2);

        ShortCircuitAnalysisResult scar = provider.run(nt2, faults, scp, cm, Collections.emptyList()).join();

        List<FaultResult> frs = scar.getFaultResults();

        String providerName = provider.getName();
        String providerVersion = provider.getVersion();

        assertEquals(4.646530628204346, frs.get(1).getCurrent().getDirectMagnitude(), 0.00001);
        assertEquals(5.100971698760986, frs.get(0).getCurrent().getDirectMagnitude(), 0.00001);
        assertEquals("OpenShortCircuit", providerName);
        assertEquals("0.1", providerVersion);

    }

    @Test
    void openShortCircuitProvider4n() {

        //set up LF info
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);
        Network nt4 = create4n(NetworkFactory.findDefault());
        LoadFlowResult resultnt4 = LoadFlow.run(nt4, loadFlowParameters);

        //set up ShortCircuitProvider info
        ShortCircuitAnalysisProvider provider = new OpenShortCircuitProvider(new DenseMatrixFactory());
        ComputationManager cm = LocalComputationManager.getDefault();
        ShortCircuitParameters scp = new ShortCircuitParameters();

        //CompletableFuture<ShortCircuitAnalysisResult> scar = provider.run(nt2, scp, cm);
        List<Fault> faults = new ArrayList<>(); // TODO

        BusFault bf1 = new BusFault("F1", "B1");
        BusFault bf2 = new BusFault("F2", "B2");
        BusFault bf3 = new BusFault("F3", "B3");
        BusFault bf4 = new BusFault("F4", "B4");

        faults.add(bf1);
        faults.add(bf2);
        faults.add(bf3);
        faults.add(bf4);

        ShortCircuitAnalysisResult scar = provider.run(nt4, faults, scp, cm, Collections.emptyList()).join();

        List<FaultResult> frs = scar.getFaultResults();

        assertEquals(6.143869876861572, frs.get(0).getCurrent().getDirectMagnitude(), 0.00001);
        assertEquals(6.491052150726318, frs.get(1).getCurrent().getDirectMagnitude(), 0.00001);
        assertEquals(6.222183704376221, frs.get(2).getCurrent().getDirectMagnitude(), 0.00001);
        assertEquals(5.909138679504394, frs.get(3).getCurrent().getDirectMagnitude(), 0.00001);

    }

    @Test
    void openShortCircuitProvider2nTfo() {

        //set up LF info
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);
        Network nt2 = create2nTfo(NetworkFactory.findDefault());
        LoadFlowResult resultnt2 = LoadFlow.run(nt2, loadFlowParameters);

        //set up ShortCircuitProvider info
        ShortCircuitAnalysisProvider provider = new OpenShortCircuitProvider(new DenseMatrixFactory());
        ComputationManager cm = LocalComputationManager.getDefault();
        ShortCircuitParameters scp = new ShortCircuitParameters();

        //CompletableFuture<ShortCircuitAnalysisResult> scar = provider.run(nt2, scp, cm);
        List<Fault> faults = new ArrayList<>(); // TODO

        BusFault bf1 = new BusFault("F1", "B1");
        BusFault bf2 = new BusFault("F2", "B2");
        faults.add(bf1);
        faults.add(bf2);

        ShortCircuitAnalysisResult scar = provider.run(nt2, faults, scp, cm, Collections.emptyList()).join();

        List<FaultResult> frs = scar.getFaultResults();

        assertEquals(4.888257026672363, frs.get(1).getCurrent().getDirectMagnitude(), 0.00001);
        assertEquals(5.100976467132568, frs.get(0).getCurrent().getDirectMagnitude(), 0.00001);

    }

    @Test
    void shortCircuitSystematic() {

        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);

        Network nt2 = create2n(NetworkFactory.findDefault());
        LoadFlowResult resultnt2 = LoadFlow.run(nt2, loadFlowParameters);

        MatrixFactory  matrixFactory = new DenseMatrixFactory();

        List<ShortCircuitFault> tmpV = new ArrayList<>();

        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.TRANSIENT;

        ShortCircuitNormCourcirc shortCircuitNormCourcirc = new ShortCircuitNormCourcirc();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, ShortCircuitEngineParameters.AnalysisType.SYSTEMATIC, tmpV, false, ShortCircuitEngineParameters.VoltageProfileType.NOMINAL, false, periodType, shortCircuitNormCourcirc);
        ShortCircuitBalancedEngine scbEngine = new ShortCircuitBalancedEngine(nt2, scbParameters);

        scbEngine.run();
        List<Double> val = new ArrayList<>();
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> res : scbEngine.resultsPerFault.entrySet()) {
            val.add(res.getValue().getIdx());
        }

        assertEquals(0.0996007987855852, val.get(0), 0.00001);
        assertEquals(0.0999999987871081, val.get(1), 0.00001);

    }

    @Test
    void shortCircuitSubTransientReference() {

        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);

        Network network = ReferenceNetwork.createShortCircuitReference();

        MatrixFactory  matrixFactory = new DenseMatrixFactory();

        List<ShortCircuitFault> faultList = new ArrayList<>();
        ShortCircuitFault sc1 = new ShortCircuitFault("B7", true, "sc1", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc1);

        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.SUB_TRANSIENT;

        ShortCircuitNormCourcirc shortCircuitNormCourcirc = new ShortCircuitNormCourcirc();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, ShortCircuitEngineParameters.AnalysisType.SELECTIVE, faultList, true, ShortCircuitEngineParameters.VoltageProfileType.NOMINAL, false, periodType, shortCircuitNormCourcirc);
        ShortCircuitBalancedEngine scbEngine = new ShortCircuitBalancedEngine(network, scbParameters);

        scbEngine.run();
        List<Double> val = new ArrayList<>();
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> res : scbEngine.resultsPerFault.entrySet()) {
            val.add(res.getValue().getIcc());
        }

        // here Icc = 1/sqrt(3)*Eth(pu)/Zth(pu100)*Sb100/Vb*1000
        // here new Icc = 1/sqrt(3)*Eth(pu)/Zth(pu100)*Sb100*1000
        // and Idocumentation = Ib*Eth(pu)/Zth(pu15) then Idocumentation = Icc * Ib * sqrt(3) * Vb / (1000 * Sb15)  with Ib = 18.064
        // in the documentation, expected Idocumentation ~ 35.656 kA
        assertEquals(35.69309945355154, val.get(0) * 18.064 * 0.277 * Math.sqrt(3) / (1000. * 15.) / 0.277, 0.00001);

    }

    @Test
    void shortCircuitIec31() {

        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);

        Network network = ReferenceNetwork.createShortCircuitIec31();

        MatrixFactory  matrixFactory = new DenseMatrixFactory();

        List<ShortCircuitFault> faultList = new ArrayList<>();
        ShortCircuitFault sc1 = new ShortCircuitFault("B3", true, "sc1",  0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc1);

        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.SUB_TRANSIENT;
        ShortCircuitNormIec shortCircuitNormIec = new ShortCircuitNormIec();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, ShortCircuitEngineParameters.AnalysisType.SELECTIVE, faultList, true, ShortCircuitEngineParameters.VoltageProfileType.NOMINAL, false, periodType, shortCircuitNormIec);
        ShortCircuitBalancedEngine scbEngine = new ShortCircuitBalancedEngine(network, scbParameters);

        scbEngine.run();
        List<Double> val = new ArrayList<>();
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> res : scbEngine.resultsPerFault.entrySet()) {
            val.add(res.getValue().getIk());
        }

        // here Icc = 1/sqrt(3)*Eth(pu)/Zth(pu100)*Sb100/Vb*1000
        // and I"k = 1/sqrt(3) * cmax * Un /(Zeq) and expected I"k = 34.62 kA
        assertEquals(34.62398968800272, val.get(0), 0.00001);

    }

    @Test
    void shortCircuitIec31TestNetwork() {

        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setTwtSplitShuntAdmittance(true);

        Network network = ReferenceNetwork.createShortCircuitIec31testNetwork();

        MatrixFactory  matrixFactory = new DenseMatrixFactory();

        List<ShortCircuitFault> faultList = new ArrayList<>();
        ShortCircuitFault sc1 = new ShortCircuitFault("B1", true, "sc1", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc1);
        ShortCircuitFault sc2 = new ShortCircuitFault("B2", true, "sc2", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc2);
        ShortCircuitFault sc3 = new ShortCircuitFault("B3", true, "sc3", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc3);
        ShortCircuitFault sc4 = new ShortCircuitFault("B4", true, "sc4", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc4);
        ShortCircuitFault sc5 = new ShortCircuitFault("B5", true, "sc5", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc5);
        ShortCircuitFault sc6 = new ShortCircuitFault("B6", true, "sc6", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc6);
        ShortCircuitFault sc7 = new ShortCircuitFault("B7", true, "sc7", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc7);
        ShortCircuitFault sc8 = new ShortCircuitFault("B8", true, "sc8", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND);
        faultList.add(sc8);

        ShortCircuitEngineParameters.PeriodType periodType = ShortCircuitEngineParameters.PeriodType.TRANSIENT;
        ShortCircuitNormIec shortCircuitNormIec = new ShortCircuitNormIec();
        ShortCircuitEngineParameters scbParameters = new ShortCircuitEngineParameters(loadFlowParameters, matrixFactory, ShortCircuitEngineParameters.AnalysisType.SELECTIVE, faultList, true, ShortCircuitEngineParameters.VoltageProfileType.NOMINAL, false, periodType, shortCircuitNormIec);
        ShortCircuitBalancedEngine scbEngine = new ShortCircuitBalancedEngine(network, scbParameters);

        scbEngine.run();
        List<Double> values = new ArrayList<>();
        for (Map.Entry<ShortCircuitFault, ShortCircuitResult> res : scbEngine.resultsPerFault.entrySet()) {
            values.add(res.getValue().getIk());
        }

        // I"k = 1/sqrt(3) * cmax * Un /(Zeq)
        assertEquals(40.64478476116188, values.get(0), 0.001); // bus 1 : expected in doc = 40.6447 kA
        assertEquals(31.783052222534174, values.get(1), 0.001); // bus 2 : expected in doc =  31.7831 kA
        assertEquals(19.672955775750143, values.get(2), 0.001); // bus 3 : expected in doc =  19.673 kA
        assertEquals(16.227655866910894, values.get(3), 0.001); // bus 4 : expected in doc =  16.2277 kA
        assertEquals(33.18941481677016, values.get(4), 0.001); // bus 5 : expected in doc =  33.1894 kA
        assertEquals(37.56287899040728, values.get(5), 0.001); // bus 6 : expected in doc =  37.5629 kA
        assertEquals(25.589463480212533, values.get(6), 0.001); // bus 7 : expected in doc =  25.5895 kA
        assertEquals(13.577771545200052, values.get(7), 0.001); // bus 8 : expected in doc =  13.5778 kA

        //assertEquals(4039.8610235151364, values.get(8), 0.1); // T3 U0 node : for check only
        //assertEquals(4039.8610235151364, values.get(8), 0.1); // T4 U0 node : for check only

    }

    public static Network create2n(NetworkFactory networkFactory) {
        Objects.requireNonNull(networkFactory);

        double p0l2 = 10;
        double q0l2 = 10;
        double pgen = 10;
        double xl = 2.;

        Network network = networkFactory.createNetwork("2n", "test");
        network.setCaseDate(DateTime.parse("2018-03-05T13:30:30.486+01:00"));
        Substation substation1 = network.newSubstation()
                .setId("S1")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl1 = substation1.newVoltageLevel()
                .setId("VL_1")
                .setNominalV(100.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(200)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus bus1 = vl1.getBusBreakerView().newBus()
                .setId("B1")
                .add();
        bus1.setV(100.0).setAngle(0.);
        Generator gen1 = vl1.newGenerator()
                .setId("G1")
                .setBus(bus1.getId())
                .setMinP(0.0)
                .setMaxP(150)
                .setTargetP(pgen)
                .setTargetV(100.0)
                .setVoltageRegulatorOn(true)
                .add();

        gen1.newExtension(GeneratorShortCircuitAdder.class)
                .withDirectSubtransX(20)
                .withDirectTransX(20)
                .withStepUpTransformerX(0.)
                .add();

        Substation substation2 = network.newSubstation()
                .setId("S2")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl2 = substation2.newVoltageLevel()
                .setId("VL_2")
                .setNominalV(100.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(200)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus bus2 = vl2.getBusBreakerView().newBus()
                .setId("B2")
                .add();
        bus2.setV(100.0).setAngle(0);
        vl2.newLoad()
                .setId("LOAD_2")
                .setBus(bus2.getId())
                .setP0(p0l2)
                .setQ0(q0l2)
                .add();

        network.newLine()
                .setId("B1_B2")
                .setVoltageLevel1(vl1.getId())
                .setBus1(bus1.getId())
                .setConnectableBus1(bus1.getId())
                .setVoltageLevel2(vl2.getId())
                .setBus2(bus2.getId())
                .setConnectableBus2(bus2.getId())
                .setR(0.0)
                .setX(xl)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();

        return network;
    }

    public static Network create4n(NetworkFactory networkFactory) {
        Objects.requireNonNull(networkFactory);
        //      2                               3
        //  (~)-|--------------X23--------------|-[X]  Po= 10.  Qo = 100.
        //      |--+                         +--|
        //         |                        /   |--+
        //         |                       /       |
        //         |                      /        |
        //        X12                    /         |
        //         |      +-----X13-----+         X34
        //         |     /                         |
        //      1  |    /                          |
        //      |--+   /                           |
        //      |-----+                         |--+
        //   +--|--------------X14--------------|-----[X] Po= 20.  Qo = 10.
        //   |                                  |--B4
        //   B1

        Network network = networkFactory.createNetwork("4n", "test");
        network.setCaseDate(DateTime.parse("2018-03-05T13:30:30.486+01:00"));
        Substation substation1 = network.newSubstation()
                .setId("S1")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl1 = substation1.newVoltageLevel()
                .setId("VL_1")
                .setNominalV(100.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(200)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus bus1 = vl1.getBusBreakerView().newBus()
                .setId("B1")
                .add();
        bus1.setV(100.0).setAngle(0.);
        // test with shunt (could be removed)
        vl1.newShuntCompensator()
                .setId("SHUNT_1")
                .setBus(bus1.getId())
                .setSectionCount(1)
                .setVoltageRegulatorOn(false)
                .newLinearModel().setMaximumSectionCount(1).setBPerSection(-0.003).add()
                .add();

        Substation substation2 = network.newSubstation()
                .setId("S2")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl2 = substation2.newVoltageLevel()
                .setId("VL_2")
                .setNominalV(100.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(200)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus bus2 = vl2.getBusBreakerView().newBus()
                .setId("B2")
                .add();
        bus2.setV(100.0).setAngle(0);
        Generator gen2 = vl2.newGenerator()
                .setId("G2")
                .setBus(bus2.getId())
                .setMinP(0.0)
                .setMaxP(150)
                .setTargetP(30)
                .setTargetV(100.0)
                .setVoltageRegulatorOn(true)
                .add();
        gen2.newExtension(GeneratorShortCircuitAdder.class)
                .withDirectTransX(20.)
                .withDirectSubtransX(20.)
                .withStepUpTransformerX(0.)
                .add();

        Substation substation3 = network.newSubstation()
                .setId("S3")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl3 = substation3.newVoltageLevel()
                .setId("VL_3")
                .setNominalV(100.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(200)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus bus3 = vl3.getBusBreakerView().newBus()
                .setId("B3")
                .add();
        bus3.setV(100.0).setAngle(0.);
        vl3.newLoad()
                .setId("LOAD_3")
                .setBus(bus3.getId())
                .setP0(10.0)
                .setQ0(100.)
                .add();

        Substation substation4 = network.newSubstation()
                .setId("S4")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl4 = substation4.newVoltageLevel()
                .setId("VL_4")
                .setNominalV(100.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(200)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus bus4 = vl4.getBusBreakerView().newBus()
                .setId("B4")
                .add();
        bus4.setV(100.0).setAngle(0.);
        vl4.newShuntCompensator()
                .setId("SHUNT_4")
                .setBus(bus4.getId())
                .setSectionCount(1)
                .setVoltageRegulatorOn(false)
                .newLinearModel().setMaximumSectionCount(1).setBPerSection(-0.00105).add()
                .add();
        vl4.newLoad()
                .setId("LOAD_4")
                .setBus(bus4.getId())
                .setP0(20.)
                .setQ0(10.)
                .add();

        network.newLine()
                .setId("B1_B2")
                .setVoltageLevel1(vl1.getId())
                .setBus1(bus1.getId())
                .setConnectableBus1(bus1.getId())
                .setVoltageLevel2(vl2.getId())
                .setBus2(bus2.getId())
                .setConnectableBus2(bus2.getId())
                .setR(0.0)
                .setX(1 / 0.5)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();
        network.newLine()
                .setId("B1_B3")
                .setVoltageLevel1(vl1.getId())
                .setBus1(bus1.getId())
                .setConnectableBus1(bus1.getId())
                .setVoltageLevel2(vl3.getId())
                .setBus2(bus3.getId())
                .setConnectableBus2(bus3.getId())
                .setR(0.0)
                .setX(1 / 0.4)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();
        network.newLine()
                .setId("B1_B4")
                .setVoltageLevel1(vl1.getId())
                .setBus1(bus1.getId())
                .setConnectableBus1(bus1.getId())
                .setVoltageLevel2(vl4.getId())
                .setBus2(bus4.getId())
                .setConnectableBus2(bus4.getId())
                .setR(0.0)
                .setX(1 / 0.4)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();
        network.newLine()
                .setId("B2_B3")
                .setVoltageLevel1(vl2.getId())
                .setBus1(bus2.getId())
                .setConnectableBus1(bus2.getId())
                .setVoltageLevel2(vl3.getId())
                .setBus2(bus3.getId())
                .setConnectableBus2(bus3.getId())
                .setR(0.0)
                .setX(1 / 0.6)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();
        network.newLine()
                .setId("B3_B4")
                .setVoltageLevel1(vl3.getId())
                .setBus1(bus3.getId())
                .setConnectableBus1(bus3.getId())
                .setVoltageLevel2(vl4.getId())
                .setBus2(bus4.getId())
                .setConnectableBus2(bus4.getId())
                .setR(0.0)
                .setX(1 / 0.5)
                .setG1(0.0)
                .setB1(0.0)
                .setG2(0.0)
                .setB2(0.0)
                .add();

        return network;
    }

    public static Network create2nTfo(NetworkFactory networkFactory) {
        Objects.requireNonNull(networkFactory);

        double p0l2 = 10;
        double q0l2 = 10;
        double pGen = 10;
        double xl = 2.;

        Network network = networkFactory.createNetwork("2nTfo", "test");
        network.setCaseDate(DateTime.parse("2018-03-05T13:30:30.486+01:00"));
        Substation substation1 = network.newSubstation()
                .setId("S1")
                .setCountry(Country.FR)
                .add();
        VoltageLevel vl1 = substation1.newVoltageLevel()
                .setId("VL_1")
                .setNominalV(100.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(200)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus bus1 = vl1.getBusBreakerView().newBus()
                .setId("B1")
                .add();
        bus1.setV(100.0).setAngle(0.);

        Generator gen1 = vl1.newGenerator()
                .setId("G1")
                .setBus(bus1.getId())
                .setMinP(0.0)
                .setMaxP(150)
                .setTargetP(pGen)
                .setTargetV(100.0)
                .setVoltageRegulatorOn(true)
                .add();

        gen1.newExtension(GeneratorShortCircuitAdder.class)
                .withDirectSubtransX(20)
                .withDirectTransX(20)
                .withStepUpTransformerX(0.)
                .add();

        VoltageLevel vl2 = substation1.newVoltageLevel()
                .setId("VL_2")
                .setNominalV(150.0)
                .setLowVoltageLimit(0)
                .setHighVoltageLimit(200)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus bus2 = vl2.getBusBreakerView().newBus()
                .setId("B2")
                .add();
        bus2.setV(150.0).setAngle(0);
        vl2.newLoad()
                .setId("LOAD_2")
                .setBus(bus2.getId())
                .setP0(p0l2)
                .setQ0(q0l2)
                .add();

        TwoWindingsTransformer t2w = substation1.newTwoWindingsTransformer()
                .setId("B1_B2")
                .setVoltageLevel1(vl1.getId())
                .setBus1(bus1.getId())
                .setConnectableBus1(bus1.getId())
                .setVoltageLevel2(vl2.getId())
                .setBus2(bus2.getId())
                .setConnectableBus2(bus2.getId())
                .setR(0.0)
                .setX(xl)
                .setRatedU1(100.0)
                .setRatedU2(150.0)
                .setG(0.0)
                .setB(0.0)
                .add();

        return network;
    }
}
