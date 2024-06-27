/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.openloadflow.ac.AcLoadFlowParameters;
import com.powsybl.openloadflow.graph.EvenShiloachGraphDecrementalConnectivityFactory;
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
class TheveninTest {

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

        List<CalculationLocation> faultsList = new ArrayList<>();
        CalculationLocation f1 = new CalculationLocation("B4");
        CalculationLocation f2 = new CalculationLocation("B2");
        faultsList.add(f1);
        faultsList.add(f2);

        AcLoadFlowParameters acLoadFlowParameters = OpenLoadFlowParameters.createAcParameters(network,
                parameters, OpenLoadFlowParameters.get(parameters), matrixFactory, new EvenShiloachGraphDecrementalConnectivityFactory<>());

        TheveninEquivalentParameters.TheveninVoltageProfileType avp = TheveninEquivalentParameters.TheveninVoltageProfileType.CALCULATED;
        TheveninEquivalentParameters.TheveninPeriodType periodType = TheveninEquivalentParameters.TheveninPeriodType.THEVENIN_TRANSIENT;
        TheveninEquivalentParameters thParameters = new TheveninEquivalentParameters(acLoadFlowParameters, matrixFactory, faultsList, true, avp, periodType, false); // check how to give a Network bus in input
        TheveninEquivalent thEq = new TheveninEquivalent(network, thParameters);

        thEq.run();

        assertEquals(0.0027661335620416884, thEq.getImpedanceLinearResolution().results.get(1).getRthz11(), 0.000001);
        assertEquals(0.16629396899928067, thEq.getImpedanceLinearResolution().results.get(1).getXthz12(), 0.000001);
        assertEquals(0.0030247992008329934, thEq.getImpedanceLinearResolution().results.get(0).getRthz11(), 0.000001);
        assertEquals(0.1833452236067607, thEq.getImpedanceLinearResolution().results.get(0).getXthz12(), 0.000001);
        //assertEquals(0.16522876711663945, thEq.results.get(0).getDvr1().get(0), 0.000001);

    }

    @Test
    void referenceTransientTest() {
        Network network = ReferenceNetwork.createShortCircuitReference();

        List<CalculationLocation> faultsList = new ArrayList<>();
        CalculationLocation f1 = new CalculationLocation("B2");
        CalculationLocation f2 = new CalculationLocation("B5");
        CalculationLocation f3 = new CalculationLocation("B7");
        CalculationLocation f4 = new CalculationLocation("B8");
        faultsList.add(f1);
        faultsList.add(f2);
        faultsList.add(f3);
        faultsList.add(f4);

        AcLoadFlowParameters acLoadFlowParameters = OpenLoadFlowParameters.createAcParameters(network,
                parameters, OpenLoadFlowParameters.get(parameters), matrixFactory, new EvenShiloachGraphDecrementalConnectivityFactory<>());

        TheveninEquivalentParameters.TheveninVoltageProfileType avp = TheveninEquivalentParameters.TheveninVoltageProfileType.NOMINAL;
        TheveninEquivalentParameters.TheveninPeriodType periodType = TheveninEquivalentParameters.TheveninPeriodType.THEVENIN_TRANSIENT;
        TheveninEquivalentParameters thParameters = new TheveninEquivalentParameters(acLoadFlowParameters, matrixFactory, faultsList, true, avp, periodType, false); // check how to give a Network bus in input
        TheveninEquivalent thEq = new TheveninEquivalent(network, thParameters);

        thEq.run();

        // results here are with Sbase = 100 MVA we convert them into Sbase = 15 MVA to be in line with the reference doc result :
        assertEquals(0.003683374391319212, thEq.getImpedanceLinearResolution().results.get(0).getRthz11() * 15. / 100., 0.000001); //F1 : doc result = Zth(Sbase15) ~ 0.0036+j0.0712
        assertEquals(0.07118802892811232, thEq.getImpedanceLinearResolution().results.get(0).getXthz12() * 15. / 100., 0.000001);
        assertEquals(0.019949496420349225, thEq.getImpedanceLinearResolution().results.get(1).getRthz11() * 15. / 100., 0.000001); //F2 : doc result = Zth(Sbase15) ~ 0.0199+j0.2534
        assertEquals(0.2534161781273357, thEq.getImpedanceLinearResolution().results.get(1).getXthz12() * 15. / 100., 0.000001);

    }

    @Test
    void referenceSubTransientTest() {
        Network network = ReferenceNetwork.createShortCircuitReference();

        List<CalculationLocation> faultsList = new ArrayList<>();
        CalculationLocation f1 = new CalculationLocation("B2");
        CalculationLocation f2 = new CalculationLocation("B5");
        CalculationLocation f3 = new CalculationLocation("B7");
        CalculationLocation f4 = new CalculationLocation("B8");
        faultsList.add(f1);
        faultsList.add(f2);
        faultsList.add(f3);
        faultsList.add(f4);

        AcLoadFlowParameters acLoadFlowParameters = OpenLoadFlowParameters.createAcParameters(network,
                parameters, OpenLoadFlowParameters.get(parameters), matrixFactory, new EvenShiloachGraphDecrementalConnectivityFactory<>());

        TheveninEquivalentParameters.TheveninVoltageProfileType avp = TheveninEquivalentParameters.TheveninVoltageProfileType.NOMINAL;
        TheveninEquivalentParameters.TheveninPeriodType periodType = TheveninEquivalentParameters.TheveninPeriodType.THEVENIN_SUB_TRANSIENT;
        TheveninEquivalentParameters thParameters = new TheveninEquivalentParameters(acLoadFlowParameters, matrixFactory, faultsList, true, avp, periodType, false); // check how to give a Network bus in input
        TheveninEquivalent thEq = new TheveninEquivalent(network, thParameters);

        thEq.run();

        // results here are with Sbase = 100 MVA we convert them into Sbase = 15 MVA to be in line with the reference doc result :
        assertEquals(0.0035803351059196286, thEq.getImpedanceLinearResolution().results.get(0).getRthz11() * 15. / 100., 0.000001); //F1 : doc result = Zth(Sbase15) ~ 0.0035+j0.0666
        assertEquals(0.0666102621341282, thEq.getImpedanceLinearResolution().results.get(0).getXthz12() * 15. / 100., 0.000001);
        assertEquals(0.01766454025768954, thEq.getImpedanceLinearResolution().results.get(1).getRthz11() * 15. / 100., 0.000001); //F2 : doc result = Zth(Sbase15) ~ 0.0175+j0.2313
        assertEquals(0.2313127317660599, thEq.getImpedanceLinearResolution().results.get(1).getXthz12() * 15. / 100., 0.000001);
        assertEquals(0.07967413109647312, thEq.getImpedanceLinearResolution().results.get(2).getRthz11() * 15. / 100., 0.000001); //F2 : doc result = Zth(Sbase15) ~ 0.0796+j0.5
        assertEquals(0.4997813218278794, thEq.getImpedanceLinearResolution().results.get(2).getXthz12() * 15. / 100., 0.000001);

    }

}
