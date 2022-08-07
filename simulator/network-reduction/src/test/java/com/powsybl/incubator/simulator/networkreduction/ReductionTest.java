/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.networkreduction;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.incubator.simulator.util.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.DenseMatrix;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.openloadflow.ac.outerloop.AcLoadFlowParameters;
import com.powsybl.openloadflow.equations.EquationSystem;
import com.powsybl.openloadflow.equations.VariableSet;
import com.powsybl.openloadflow.graph.EvenShiloachGraphDecrementalConnectivityFactory;
import com.powsybl.openloadflow.network.LfNetwork;
import com.powsybl.openloadflow.network.util.VoltageInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
class ReductionTest {

    private LoadFlowParameters loadFlowParameters;

    private OpenLoadFlowParameters loadFlowParametersExt;

    private MatrixFactory matrixFactory;

    private LoadFlow.Runner loadFlowRunner;

    @BeforeEach
    void setUp() {
        loadFlowParameters = new LoadFlowParameters();
        loadFlowParametersExt = OpenLoadFlowParameters.get(loadFlowParameters)
                .setAddRatioToLinesWithDifferentNominalVoltageAtBothEnds(false);
        loadFlowParameters.addExtension(OpenLoadFlowParameters.class, loadFlowParametersExt);

        matrixFactory = new DenseMatrixFactory();
        loadFlowRunner = new LoadFlow.Runner(new OpenLoadFlowProvider(matrixFactory));
    }

    /**
     * Check behaviour of the load flow for simple manipulations on eurostag example 1 network.
     * - line opening
     * - load change
     */
    @Test
    void computeCurrentInjectorTest() {
        Network network = IeeeCdfNetworkFactory.create14();

        List<String> voltageLevels = new ArrayList<>();

        ReductionParameters reductionParameters = new ReductionParameters(loadFlowParameters, matrixFactory, voltageLevels, ReductionEngine.ReductionType.WARD_INJ);

        ReductionEngine re = new ReductionEngine(network, reductionParameters);
        LfNetwork lfNetwork = re.getLfNetworks().get(0);

        OpenLoadFlowParameters loadflowParametersExt = OpenLoadFlowParameters.get(loadFlowParameters);
        AcLoadFlowParameters acLoadFlowParameters = OpenLoadFlowParameters.createAcParameters(network, loadFlowParameters, loadflowParametersExt, matrixFactory, new EvenShiloachGraphDecrementalConnectivityFactory<>(), Reporter.NO_OP);
        AdmittanceEquationSystem.AdmittanceVoltageProfileType admittanceVoltageProfileType = AdmittanceEquationSystem.AdmittanceVoltageProfileType.CALCULATED;
        //AdditionalDataInfo additionalDataInfo = new AdditionalDataInfo();
        AdmittanceEquationSystem.AdmittancePeriodType admittancePeriodType = AdmittanceEquationSystem.AdmittancePeriodType.ADM_TRANSIENT;
        EquationSystemFeeders feeders = new EquationSystemFeeders();
        EquationSystem<VariableType, EquationType> equationSystem = AdmittanceEquationSystem.create(lfNetwork, new VariableSet<>(), AdmittanceEquationSystem.AdmittanceType.ADM_INJ, admittanceVoltageProfileType, admittancePeriodType, false, feeders, acLoadFlowParameters);

        VoltageInitializer voltageInitializer = reductionParameters.getVoltageInitializer();

        AdmittanceMatrix a = new AdmittanceMatrix(equationSystem, reductionParameters.getMatrixFactory(), lfNetwork);
        Matrix a1 = a.getMatrix();
        Matrix mV = a.getVoltageVector(lfNetwork, voltageInitializer);
        //System.out.println("===> v =");
        //mV.print(System.out);

        Matrix mI = mV.times(a1);
        //System.out.println("===> i =");
        //mI.print(System.out);

        double[] x = re.rowVectorToDouble(mI);

        assertEquals(2.1919, x[0], 0.01);
        assertEquals(0.1581, x[1], 0.01);
        assertEquals(0.1506, x[2], 0.01);
        assertEquals(-0.2990, x[3], 0.01);
    }

    @Test
    void computeCurrentInjectorWithLfResultsTest() {
        Network network = IeeeCdfNetworkFactory.create14();

        List<String> voltageLevels = new ArrayList<>();

        LoadFlowResult resultLf = loadFlowRunner.run(network, loadFlowParameters);

        ReductionParameters reductionParameters = new ReductionParameters(loadFlowParameters, matrixFactory, voltageLevels, ReductionEngine.ReductionType.WARD_INJ);

        ReductionEngine re = new ReductionEngine(network, reductionParameters);
        LfNetwork lfNetwork = re.getLfNetworks().get(0);

        OpenLoadFlowParameters loadflowParametersExt = OpenLoadFlowParameters.get(loadFlowParameters);
        AcLoadFlowParameters acLoadFlowParameters = OpenLoadFlowParameters.createAcParameters(network, loadFlowParameters, loadflowParametersExt, matrixFactory, new EvenShiloachGraphDecrementalConnectivityFactory<>(), Reporter.NO_OP);
        AdmittanceEquationSystem.AdmittanceVoltageProfileType admittanceVoltageProfileType = AdmittanceEquationSystem.AdmittanceVoltageProfileType.CALCULATED;
        AdmittanceEquationSystem.AdmittancePeriodType admittancePeriodType = AdmittanceEquationSystem.AdmittancePeriodType.ADM_TRANSIENT;
        //AdditionalDataInfo additionalDataInfo = new AdditionalDataInfo();
        EquationSystemFeeders feeders = new EquationSystemFeeders();
        EquationSystem<VariableType, EquationType> equationSystem = AdmittanceEquationSystem.create(lfNetwork, new VariableSet<>(), AdmittanceEquationSystem.AdmittanceType.ADM_INJ, admittanceVoltageProfileType, admittancePeriodType, false, feeders, acLoadFlowParameters);

        VoltageInitializer voltageInitializer = reductionParameters.getVoltageInitializer();

        AdmittanceMatrix a = new AdmittanceMatrix(equationSystem, reductionParameters.getMatrixFactory(), lfNetwork);
        Matrix a1 = a.getMatrix();
        Matrix mV = a.getVoltageVector(lfNetwork, voltageInitializer);
        //System.out.println("===> v =");
        //mV.print(System.out);

        Matrix mI = mV.times(a1);
        //System.out.println("===> i =");
        //mI.print(System.out);

        double[] x = re.rowVectorToDouble(mI);

        assertEquals(2.192, x[0], 0.001);
        assertEquals(0.156, x[1], 0.001);
        assertEquals(0.148, x[2], 0.001);
        assertEquals(-0.309, x[3], 0.001);
    }

    @Test
    void ieee14ReductionTest() {

        Network network = IeeeCdfNetworkFactory.create14();

        List<String> voltageLevels = new ArrayList<>();
        voltageLevels.add("VL12");
        voltageLevels.add("VL13");

        LoadFlowResult resultLf = loadFlowRunner.run(network, loadFlowParameters);

        ReductionParameters reductionParameters = new ReductionParameters(loadFlowParameters, matrixFactory, voltageLevels, ReductionEngine.ReductionType.WARD_INJ);

        ReductionEngine re = new ReductionEngine(network, reductionParameters);

        re.run();

        ReductionEngine.ReductionResults results = re.getReductionResults();

        DenseMatrix m = results.getMinusYeq().toDense();

        assertEquals(2, results.getBusNumToRealIeq().size(), 0);
        assertEquals(3.730432312, m.get(0, 0), 0.000001);
        /*assertEquals(-0.9770922266610, m.get(1, 0), 0.000001);
        assertEquals(-4.0518208119503, m.get(2, 0), 0.000001);
        assertEquals(-1.1284861339383, m.get(3, 0), 0.000001);
        assertEquals(0.9770922266610, m.get(0, 1), 0.000001);
        assertEquals(-10.640432247821, m.get(1, 1), 0.000001);
        assertEquals(1.1284861339383, m.get(2, 1), 0.000001);
        assertEquals(-4.051820811950, m.get(3, 1), 0.000001);
        assertEquals(-3.2261022158310, m.get(0, 2), 0.000001);
        assertEquals(-0.3281562136182, m.get(1, 2), 0.000001);
        assertEquals(-1.3282719530734, m.get(2, 2), 0.000001);
        assertEquals(-0.36576402166078, m.get(3, 2), 0.000001);
        assertEquals(0.3281562136182, m.get(0, 3), 0.000001);
        assertEquals(-3.2261022158310, m.get(1, 3), 0.000001);
        assertEquals(0.3657640216607, m.get(2, 3), 0.000001);
        assertEquals(-1.328271953073, m.get(3, 3), 0.000001);*/

        ReductionEngine.ReductionHypotheses hypo =  re.getReductionHypo();
        assertEquals(0.159817620898, hypo.eqLoads.get(0).pEq, 0.00001);
        /*assertEquals(0.062286236316, hypo.eqLoads.get(0).qEq, 0.00001);
        assertEquals(13.86653446365, hypo.eqShunts.get(0).gEq, 0.0001);
        assertEquals(1.305248440279, hypo.eqShunts.get(0).bEq, 0.0001);
        assertEquals(-0.3030327598342, hypo.eqBranches.get(0).rEq, 0.0001);
        assertEquals(0.05718010758327, hypo.eqBranches.get(0).xEq, 0.0001);*/

    }

    @Test
    void example4nReductionTest() {
        loadFlowParameters.setTwtSplitShuntAdmittance(true);

        //Create a list of voltage of the external network
        List<String> voltageLevels = new ArrayList<>();
        voltageLevels.add("VL_4");

        Network nt4 = Networks.create4n();
        LoadFlowResult resultnt4 = loadFlowRunner.run(nt4, loadFlowParameters);

        MatrixFactory  matrixFactory = new DenseMatrixFactory();
        ReductionParameters reductionParameters = new ReductionParameters(loadFlowParameters, matrixFactory, voltageLevels, ReductionEngine.ReductionType.WARD_INJ);
        ReductionEngine re = new ReductionEngine(nt4, reductionParameters);

        re.run();

        ReductionEngine.ReductionResults results = re.getReductionResults();
        ReductionEngine.ReductionHypotheses hypo =  re.getReductionHypo();

        assertEquals(0.045, hypo.eqBranches.get(0).xEq, 0.00001);
    }

    @Test
    void example4nShuntReductionTest() {
        loadFlowParameters.setTwtSplitShuntAdmittance(true);

        //Create a list of voltage of the external network
        List<String> voltageLevels = new ArrayList<>();
        voltageLevels.add("VL_4");

        Network nt4Shunt = Networks.create4nShunt();
        LoadFlowResult resultnt4Shunt = loadFlowRunner.run(nt4Shunt, loadFlowParameters);

        ReductionParameters reductionParameters = new ReductionParameters(loadFlowParameters, matrixFactory, voltageLevels, ReductionEngine.ReductionType.WARD_SHUNT);
        ReductionEngine re = new ReductionEngine(nt4Shunt, reductionParameters);

        re.run();

        ReductionEngine.ReductionResults results = re.getReductionResults();
        ReductionEngine.ReductionHypotheses hypo =  re.getReductionHypo();

        assertEquals(0.0450525, hypo.eqBranches.get(0).xEq, 0.0000001); // line between B1 and B3
        assertEquals(-0.046612, hypo.eqShunts.get(0).bEq, 0.000001); // shunt at bus 1
        assertEquals(-0.058265, hypo.eqShunts.get(1).bEq, 0.000001); // shunt at bus 3

        //we compare the previous network with the next one removing all equiments of BUS 4 and adding the previous hypotheses
        Network nt4ShuntEq = Networks.create4nShuntEq();
        LoadFlowResult resultnt4Eq = loadFlowRunner.run(nt4ShuntEq, loadFlowParameters);

        double v1 = nt4Shunt.getBusBreakerView().getBus("B1").getV();
        double v2 = nt4Shunt.getBusBreakerView().getBus("B2").getV();
        double v3 = nt4Shunt.getBusBreakerView().getBus("B3").getV();

        double v1Eq = nt4ShuntEq.getBusBreakerView().getBus("B1").getV();
        double v2Eq = nt4ShuntEq.getBusBreakerView().getBus("B2").getV();
        double v3Eq = nt4ShuntEq.getBusBreakerView().getBus("B3").getV();

        assertEquals(v1, v1Eq, 0.000001);
        assertEquals(v2, v2Eq, 0.000001);
        assertEquals(v3, v3Eq, 0.000001);

    }
}
