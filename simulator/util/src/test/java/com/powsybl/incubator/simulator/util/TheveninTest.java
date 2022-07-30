/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.openloadflow.ac.outerloop.AcLoadFlowParameters;
import com.powsybl.openloadflow.graph.EvenShiloachGraphDecrementalConnectivityFactory;
import org.apache.commons.math3.util.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.usefultoys.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class TheveninTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TheveninTest.class);

    private LoadFlowParameters parameters;

    private MatrixFactory matrixFactory;

    private LoadFlow.Runner loadFlowRunner;

    @BeforeEach
    void setUp() {
        parameters = new LoadFlowParameters()
                .setTwtSplitShuntAdmittance(true)
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES);
        matrixFactory = new DenseMatrixFactory();
        loadFlowRunner = new LoadFlow.Runner(new OpenLoadFlowProvider(matrixFactory));
    }

    @Test
    void computeZthTest() {
        Network network = Networks.create4n();
        LoadFlowResult resultnt4 = loadFlowRunner.run(network, parameters);

        List<ShortCircuitFault> faultsList = new ArrayList<>();
        ShortCircuitFault f1 = new ShortCircuitFault("B4", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        ShortCircuitFault f2 = new ShortCircuitFault("B2", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        faultsList.add(f1);
        faultsList.add(f2);

        AdditionalDataInfo additionalDataInfo = new AdditionalDataInfo();

        AcLoadFlowParameters acLoadFlowParameters = OpenLoadFlowParameters.createAcParameters(network,
                parameters, OpenLoadFlowParameters.get(parameters), matrixFactory, new EvenShiloachGraphDecrementalConnectivityFactory<>(), Reporter.NO_OP);

        TheveninEquivalentParameters.TheveninVoltageProfileType avp = TheveninEquivalentParameters.TheveninVoltageProfileType.CALCULATED;
        TheveninEquivalentParameters.TheveninPeriodType periodType = TheveninEquivalentParameters.TheveninPeriodType.THEVENIN_TRANSIENT;
        ShortCircuitNormCourcirc shortCircuitNormCourcirc = new ShortCircuitNormCourcirc();
        TheveninEquivalentParameters thParameters = new TheveninEquivalentParameters(acLoadFlowParameters, matrixFactory, faultsList, true, avp, periodType, false, additionalDataInfo, shortCircuitNormCourcirc); // check how to give a Network bus in input
        TheveninEquivalent thEq = new TheveninEquivalent(network, thParameters);

        thEq.run();

        assertEquals(0.0027661335620416884, thEq.getAdmittanceLinearResolution().results.get(1).getRthz11(), 0.000001);
        assertEquals(0.16629396899928067, thEq.getAdmittanceLinearResolution().results.get(1).getXthz12(), 0.000001);
        assertEquals(0.0030247992008329934, thEq.getAdmittanceLinearResolution().results.get(0).getRthz11(), 0.000001);
        assertEquals(0.1833452236067607, thEq.getAdmittanceLinearResolution().results.get(0).getXthz12(), 0.000001);
        //assertEquals(0.16522876711663945, thEq.results.get(0).getDvr1().get(0), 0.000001);

    }

    @Test
    void referenceTransientTest() {
        Pair<Network, AdditionalDataInfo>  result = ReferenceNetwork.createShortCircuitReference();

        Network network = result.getKey();

        AdditionalDataInfo additionalDataInfo = result.getValue();

        List<ShortCircuitFault> faultsList = new ArrayList<>();
        ShortCircuitFault f1 = new ShortCircuitFault("B2", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        ShortCircuitFault f2 = new ShortCircuitFault("B5", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        ShortCircuitFault f3 = new ShortCircuitFault("B7", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        ShortCircuitFault f4 = new ShortCircuitFault("B8", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        faultsList.add(f1);
        faultsList.add(f2);
        faultsList.add(f3);
        faultsList.add(f4);

        AcLoadFlowParameters acLoadFlowParameters = OpenLoadFlowParameters.createAcParameters(network,
                parameters, OpenLoadFlowParameters.get(parameters), matrixFactory, new EvenShiloachGraphDecrementalConnectivityFactory<>(), Reporter.NO_OP);

        TheveninEquivalentParameters.TheveninVoltageProfileType avp = TheveninEquivalentParameters.TheveninVoltageProfileType.NOMINAL;
        TheveninEquivalentParameters.TheveninPeriodType periodType = TheveninEquivalentParameters.TheveninPeriodType.THEVENIN_TRANSIENT;
        ShortCircuitNormCourcirc shortCircuitNormCourcirc = new ShortCircuitNormCourcirc();
        TheveninEquivalentParameters thParameters = new TheveninEquivalentParameters(acLoadFlowParameters, matrixFactory, faultsList, true, avp, periodType, false, additionalDataInfo, shortCircuitNormCourcirc); // check how to give a Network bus in input
        TheveninEquivalent thEq = new TheveninEquivalent(network, thParameters);

        thEq.run();

        // results here are with Sbase = 100 MVA we convert them into Sbase = 15 MVA to be in line with the reference doc result :
        assertEquals(0.003683374391319212, thEq.getAdmittanceLinearResolution().results.get(0).getRthz11() * 15. / 100., 0.000001); //F1 : doc result = Zth(Sbase15) ~ 0.0036+j0.0712
        assertEquals(0.07118802892811232, thEq.getAdmittanceLinearResolution().results.get(0).getXthz12() * 15. / 100., 0.000001);
        assertEquals(0.019949496420349225, thEq.getAdmittanceLinearResolution().results.get(1).getRthz11() * 15. / 100., 0.000001); //F2 : doc result = Zth(Sbase15) ~ 0.0199+j0.2534
        assertEquals(0.2534161781273357, thEq.getAdmittanceLinearResolution().results.get(1).getXthz12() * 15. / 100., 0.000001);

    }

    @Test
    void referenceSubTransientTest() {
        Pair<Network, AdditionalDataInfo>  result = ReferenceNetwork.createShortCircuitReference();

        Network network = result.getKey();

        AdditionalDataInfo additionalDataInfo = result.getValue();

        List<ShortCircuitFault> faultsList = new ArrayList<>();
        ShortCircuitFault f1 = new ShortCircuitFault("B2", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        ShortCircuitFault f2 = new ShortCircuitFault("B5", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        ShortCircuitFault f3 = new ShortCircuitFault("B7", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        ShortCircuitFault f4 = new ShortCircuitFault("B8", 0., 0., ShortCircuitFault.ShortCircuitType.TRIPHASED_GROUND, true);
        faultsList.add(f1);
        faultsList.add(f2);
        faultsList.add(f3);
        faultsList.add(f4);

        AcLoadFlowParameters acLoadFlowParameters = OpenLoadFlowParameters.createAcParameters(network,
                parameters, OpenLoadFlowParameters.get(parameters), matrixFactory, new EvenShiloachGraphDecrementalConnectivityFactory<>(), Reporter.NO_OP);

        TheveninEquivalentParameters.TheveninVoltageProfileType avp = TheveninEquivalentParameters.TheveninVoltageProfileType.NOMINAL;
        TheveninEquivalentParameters.TheveninPeriodType periodType = TheveninEquivalentParameters.TheveninPeriodType.THEVENIN_SUB_TRANSIENT;
        ShortCircuitNormCourcirc shortCircuitNormCourcirc = new ShortCircuitNormCourcirc();
        TheveninEquivalentParameters thParameters = new TheveninEquivalentParameters(acLoadFlowParameters, matrixFactory, faultsList, true, avp, periodType, false, additionalDataInfo, shortCircuitNormCourcirc); // check how to give a Network bus in input
        TheveninEquivalent thEq = new TheveninEquivalent(network, thParameters);

        thEq.run();

        // results here are with Sbase = 100 MVA we convert them into Sbase = 15 MVA to be in line with the reference doc result :
        assertEquals(0.0035803351059196286, thEq.getAdmittanceLinearResolution().results.get(0).getRthz11() * 15. / 100., 0.000001); //F1 : doc result = Zth(Sbase15) ~ 0.0035+j0.0666
        assertEquals(0.0666102621341282, thEq.getAdmittanceLinearResolution().results.get(0).getXthz12() * 15. / 100., 0.000001);
        assertEquals(0.01766454025768954, thEq.getAdmittanceLinearResolution().results.get(1).getRthz11() * 15. / 100., 0.000001); //F2 : doc result = Zth(Sbase15) ~ 0.0175+j0.2313
        assertEquals(0.2313127317660599, thEq.getAdmittanceLinearResolution().results.get(1).getXthz12() * 15. / 100., 0.000001);
        assertEquals(0.07967413109647312, thEq.getAdmittanceLinearResolution().results.get(2).getRthz11() * 15. / 100., 0.000001); //F2 : doc result = Zth(Sbase15) ~ 0.0796+j0.5
        assertEquals(0.4997813218278794, thEq.getAdmittanceLinearResolution().results.get(2).getXthz12() * 15. / 100., 0.000001);

    }

}
