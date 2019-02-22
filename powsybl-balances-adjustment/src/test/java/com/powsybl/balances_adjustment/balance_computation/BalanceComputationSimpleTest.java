/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.action.util.Scalable;
import com.powsybl.balances_adjustment.util.CountryArea;
import com.powsybl.balances_adjustment.util.CountryAreaTest;
import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * @author Ameni Walha <ameni.walha at rte-france.com>
 */
public class BalanceComputationSimpleTest {
    private Network simpleNetwork;
    private Map<NetworkArea, Double> networkAreaNetPositionTargetMap;
    private Map<NetworkArea, Scalable> networkAreasScalableMap;
    private ComputationManager computationManager;
    private CountryArea countryAreaFR;
    private CountryArea countryAreaBE;

    private BalanceComputationParameters parameters;
    private BalanceComputationFactory balanceComputationFactory;
    private LoadFlowFactory loadFlowFactory;

    @Before
    public void setUp() {
        simpleNetwork = Importers.loadNetwork("testSimpleNetwork.xiidm", CountryAreaTest.class.getResourceAsStream("/testSimpleNetwork.xiidm"));

        countryAreaFR = new CountryArea(Country.FR);
        countryAreaBE = new CountryArea(Country.BE);

        computationManager = LocalComputationManager.getDefault();

        parameters = new BalanceComputationParameters();
        balanceComputationFactory = new BalanceComputationFactoryImpl();

        loadFlowFactory = ComponentDefaultConfig.load().newFactoryImpl(LoadFlowFactory.class);
    }

    @Test
    public void testDivergentLoadFLow() {

        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1200.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1200.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_FR"), Scalable.onLoad("LOAD_FR")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_BE"), Scalable.onLoad("LOAD_BE")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        LoadFlowFactory loadFlowFactoryMock = Mockito.mock(LoadFlowFactory.class);
        BalanceComputationImpl balanceComputation = Mockito.spy(new BalanceComputationImpl(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, computationManager, loadFlowFactoryMock));
        LoadFlowResult loadFlowResult = new LoadFlowResultImpl(false, new HashMap<>(), "logs");
        doReturn(loadFlowResult).when(balanceComputation).runLoadFlow(anyObject(), anyString());

        BalanceComputationResult result = balanceComputation.run(simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(0, result.getIterationCount());
    }

    @Test
    public void testBalancedNetwork() {

        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1200.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1200.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_FR"), Scalable.onLoad("LOAD_FR")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_BE"), Scalable.onLoad("LOAD_BE")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        BalanceComputation balanceComputation = balanceComputationFactory.create(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
        assertTrue(result.getBalancedScalingMap().values().stream().allMatch(v -> v == 0.));

    }

    @Test
    public void testBalancedNetworkAfter1Scaling() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1300.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1300.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_FR"), Scalable.onLoad("LOAD_FR")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_BE"), Scalable.onLoad("LOAD_BE")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        BalanceComputation balanceComputation = balanceComputationFactory.create(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());
    }

    @Test
    public void testUnBalancedNetwork() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1300.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1400.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_FR"), Scalable.onLoad("LOAD_FR")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_BE"), Scalable.onLoad("LOAD_BE")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        BalanceComputation balanceComputation = balanceComputationFactory.create(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());
        assertEquals("InitialState", simpleNetwork.getVariantManager().getWorkingVariantId());

    }

    @Test
    public void testDifferentStateId() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1300.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1300.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_FR"), Scalable.onLoad("LOAD_FR")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_BE"), Scalable.onLoad("LOAD_BE")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        BalanceComputation balanceComputation = balanceComputationFactory.create(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        String initialVariant = simpleNetwork.getVariantManager().getWorkingVariantId();
        simpleNetwork.getVariantManager().cloneVariant(initialVariant, "InitialVariantNew");
        BalanceComputationResult result = balanceComputation.run("InitialVariantNew", parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());
        assertEquals("InitialState", simpleNetwork.getVariantManager().getWorkingVariantId());

        LoadFlow loadFlow = loadFlowFactory.create(simpleNetwork, computationManager, 1);
        LoadFlowResult loadFlowResult = loadFlow.run("InitialVariantNew", LoadFlowParameters.load()).join();
        assertEquals(1300, countryAreaFR.getNetPosition(simpleNetwork), 0.);
        assertEquals(-1300, countryAreaBE.getNetPosition(simpleNetwork), 0.);

        loadFlowResult = loadFlow.run("InitialState", LoadFlowParameters.load()).join();
        assertEquals(1200, countryAreaFR.getNetPosition(simpleNetwork), 0.);
        assertEquals(-1200, countryAreaBE.getNetPosition(simpleNetwork), 0.);
    }

    @Test
    public void testUnBalancedNetworkDifferentState() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1300.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1400.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_FR"), Scalable.onLoad("LOAD_FR")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_BE"), Scalable.onLoad("LOAD_BE")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        BalanceComputation balanceComputation = balanceComputationFactory.create(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        String initialVariant = simpleNetwork.getVariantManager().getWorkingVariantId();
        simpleNetwork.getVariantManager().cloneVariant(initialVariant, "InitialVariantNew");
        BalanceComputationResult result = balanceComputation.run("InitialVariantNew", parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());
        assertEquals("InitialState", simpleNetwork.getVariantManager().getWorkingVariantId());

        LoadFlow loadFlow = loadFlowFactory.create(simpleNetwork, computationManager, 1);
        LoadFlowResult loadFlowResult = loadFlow.run("InitialState", LoadFlowParameters.load()).join();
        assertEquals(1200, countryAreaFR.getNetPosition(simpleNetwork), 0.);
        assertEquals(-1200, countryAreaBE.getNetPosition(simpleNetwork), 0.);

        loadFlowResult = loadFlow.run("InitialVariantNew", LoadFlowParameters.load()).join();
        assertEquals(1200, countryAreaFR.getNetPosition(simpleNetwork), 0.);
        assertEquals(-1200, countryAreaBE.getNetPosition(simpleNetwork), 0.);

    }
}
