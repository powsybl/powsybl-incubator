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
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.*;
import com.powsybl.loadflow.simple.dc.SimpleDcLoadFlowFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

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
    private Generator generatorFr;
    private Load loadFr;
    private Branch branchFrBe1;
    private Branch branchFrBe2;
    private String initialState = "InitialState";
    private String initialVariantNew = "InitialVariantNew";

    @Before
    public void setUp() {
        simpleNetwork = Importers.loadNetwork("testSimpleNetwork.xiidm", CountryAreaTest.class.getResourceAsStream("/testSimpleNetwork.xiidm"));

        countryAreaFR = new CountryArea(Country.FR);
        countryAreaBE = new CountryArea(Country.BE);

        computationManager = LocalComputationManager.getDefault();

        parameters = new BalanceComputationParameters();
        balanceComputationFactory = new BalanceComputationFactoryImpl();

        loadFlowFactory = new SimpleDcLoadFlowFactory();

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_FR"), Scalable.onLoad("LOAD_FR")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 40f),
                Arrays.asList(Scalable.onGenerator("GENERATOR_BE"), Scalable.onLoad("LOAD_BE")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        generatorFr = simpleNetwork.getGenerator("GENERATOR_FR");
        loadFr = simpleNetwork.getLoad("LOAD_FR");
        branchFrBe1 = simpleNetwork.getBranch("FRANCE_BELGIUM_1");
        branchFrBe2 = simpleNetwork.getBranch("FRANCE_BELGIUM_2");
    }

    @Test
    public void testDivergentLoadFLow() {

        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1200.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1200.);

        LoadFlowFactory loadFlowFactoryMock = Mockito.mock(LoadFlowFactory.class);
        BalanceComputationImpl balanceComputation = Mockito.spy(new BalanceComputationImpl(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, computationManager, loadFlowFactoryMock));
        LoadFlowResult loadFlowResult = new LoadFlowResultImpl(false, new HashMap<>(), "logs");
        doReturn(loadFlowResult).when(balanceComputation).runLoadFlow(anyObject(), anyString());

        BalanceComputationResult result = balanceComputation.run(simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(0, result.getIterationCount());
    }

    @Test
    public void testBalancedNetworkMockito() {

        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1200.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1200.);

        LoadFlowFactory loadFlowFactoryMock = Mockito.mock(LoadFlowFactory.class);
        LoadFlow loadFlowMock = new LoadFlow() {
            @Override
            public CompletableFuture<LoadFlowResult> run(String s, LoadFlowParameters loadFlowParameters) {
                generatorFr.getTerminal().setP(3000);
                loadFr.getTerminal().setP(1800);

                branchFrBe1.getTerminal1().setP(-516);
                branchFrBe1.getTerminal2().setP(516);

                branchFrBe2.getTerminal1().setP(-683);
                branchFrBe2.getTerminal2().setP(683);
                return CompletableFuture.completedFuture(new LoadFlowResultImpl(true, Collections.emptyMap(), null));
            }

            @Override
            public String getName() {
                return "test load flow";
            }

            @Override
            public String getVersion() {
                return "1.0";
            }
        };

        BalanceComputationImpl balanceComputation = new BalanceComputationImpl(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, computationManager, loadFlowFactoryMock);
        BalanceComputationImpl balanceComputationSpy = Mockito.spy(balanceComputation);
        doReturn(loadFlowMock.run("", LoadFlowParameters.load()).join())
                .when(balanceComputationSpy).runLoadFlow(anyObject(), anyString());

        BalanceComputationResult result = balanceComputationSpy.run(simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
        assertTrue(result.getBalancedScalingMap().values().stream().allMatch(v -> v == 0.));

    }

    @Test
    public void testUnBalancedNetworkMockito() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1300.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1400.);

        LoadFlowFactory loadFlowFactoryMock = Mockito.mock(LoadFlowFactory.class);
        LoadFlow loadFlowMock = new LoadFlow() {
            @Override
            public CompletableFuture<LoadFlowResult> run(String s, LoadFlowParameters loadFlowParameters) {
                generatorFr.getTerminal().setP(3000);
                loadFr.getTerminal().setP(1800);
                branchFrBe1.getTerminal1().setP(-516);
                branchFrBe1.getTerminal2().setP(516);
                branchFrBe2.getTerminal1().setP(-683);
                branchFrBe2.getTerminal2().setP(683);
                return CompletableFuture.completedFuture(new LoadFlowResultImpl(true, Collections.emptyMap(), null));
            }

            @Override
            public String getName() {
                return "test load flow";
            }

            @Override
            public String getVersion() {
                return "1.0";
            }
        };

        BalanceComputationImpl balanceComputation = new BalanceComputationImpl(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, computationManager, loadFlowFactoryMock);
        BalanceComputationImpl balanceComputationSpy = Mockito.spy(balanceComputation);
        doReturn(loadFlowMock.run("", LoadFlowParameters.load()).join())
                .when(balanceComputationSpy).runLoadFlow(anyObject(), anyString());

        BalanceComputationResult result = balanceComputationSpy.run(simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());

        assertEquals(initialState, simpleNetwork.getVariantManager().getWorkingVariantId());

    }

    @Test
    public void testBalancedNetworkAfter1Scaling() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1300.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1300.);

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

        BalanceComputation balanceComputation = balanceComputationFactory.create(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());
        assertEquals(initialState, simpleNetwork.getVariantManager().getWorkingVariantId());

    }

    @Test
    public void testDifferentStateId() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1300.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1300.);

        BalanceComputation balanceComputation = balanceComputationFactory.create(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        String initialVariant = simpleNetwork.getVariantManager().getWorkingVariantId();
        simpleNetwork.getVariantManager().cloneVariant(initialVariant, initialVariantNew);
        BalanceComputationResult result = balanceComputation.run(initialVariantNew, parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());
        assertEquals(initialState, simpleNetwork.getVariantManager().getWorkingVariantId());

        LoadFlow loadFlow = loadFlowFactory.create(simpleNetwork, computationManager, 1);
        loadFlow.run(initialVariantNew, new LoadFlowParameters()).join();
        assertEquals(1300, countryAreaFR.getNetPosition(simpleNetwork), 0.);
        assertEquals(-1300, countryAreaBE.getNetPosition(simpleNetwork), 0.);

        loadFlow.run(initialState, new LoadFlowParameters()).join();
        assertEquals(1200, countryAreaFR.getNetPosition(simpleNetwork), 0.);
        assertEquals(-1200, countryAreaBE.getNetPosition(simpleNetwork), 0.);
    }

    @Test
    public void testUnBalancedNetworkDifferentState() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1300.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, -1400.);

        BalanceComputation balanceComputation = balanceComputationFactory.create(simpleNetwork, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        String initialVariant = simpleNetwork.getVariantManager().getWorkingVariantId();

        simpleNetwork.getVariantManager().cloneVariant(initialVariant, initialVariantNew);
        BalanceComputationResult result = balanceComputation.run(initialVariantNew, parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());
        assertEquals(initialState, simpleNetwork.getVariantManager().getWorkingVariantId());

        LoadFlow loadFlow = loadFlowFactory.create(simpleNetwork, computationManager, 1);

        loadFlow.run(initialState, new LoadFlowParameters()).join();
        assertEquals(1200, countryAreaFR.getNetPosition(simpleNetwork), 0.);
        assertEquals(-1200, countryAreaBE.getNetPosition(simpleNetwork), 0.);

        loadFlow.run(initialVariantNew, new LoadFlowParameters()).join();
        assertEquals(1200, countryAreaFR.getNetPosition(simpleNetwork), 0.);
        assertEquals(-1200, countryAreaBE.getNetPosition(simpleNetwork), 0.);

    }

}
