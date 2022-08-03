package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.Network;
import com.powsybl.incubator.simulator.util.ReferenceNetwork;
import com.powsybl.incubator.simulator.util.ShortCircuitFault;
import com.powsybl.incubator.simulator.util.ShortCircuitNormIec;
import com.powsybl.incubator.simulator.util.ShortCircuitResult;
import com.powsybl.incubator.simulator.util.extensions.AdditionalDataInfo;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.apache.commons.math3.util.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShortCircuitBiphasedGroundTest {

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
        ShortCircuitFault sc1 = new ShortCircuitFault("B3", 0., 0., ShortCircuitFault.ShortCircuitType.BIPHASED_GROUND, false);
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

        assertEquals(52.3687065865033, 3 * val.get(0), 0.00001);

    }
}