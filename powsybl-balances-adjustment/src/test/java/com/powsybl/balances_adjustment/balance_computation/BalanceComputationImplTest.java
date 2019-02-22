/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.action.util.Scalable;
import com.powsybl.balances_adjustment.util.*;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ameni Walha <ameni.walha at rte-france.com>
 */
public class BalanceComputationImplTest {
    private Network testNetwork1;
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
        testNetwork1 = Importers.loadNetwork("testCase.xiidm", CountryAreaTest.class.getResourceAsStream("/testCase.xiidm"));

        countryAreaFR = new CountryArea(Country.FR);
        countryAreaBE = new CountryArea(Country.BE);

        computationManager = LocalComputationManager.getDefault();

        parameters = new BalanceComputationParameters();
        balanceComputationFactory = new BalanceComputationFactoryImpl();

        loadFlowFactory = ComponentDefaultConfig.load().newFactoryImpl(LoadFlowFactory.class);
        //LoadFlowFactory loadFlowFactory = new SimpleLoadFlowFactory();

    }

    @Test
    public void testBalancedNetwork() {

        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1000.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 1500.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("FFR1AA1 _generator"), Scalable.onGenerator("FFR2AA1 _generator"), Scalable.onGenerator("FFR3AA1 _generator")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("BBE1AA1 _generator"), Scalable.onGenerator("BBE3AA1 _generator"), Scalable.onGenerator("BBE2AA1 _generator")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        BalanceComputation balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
        assertTrue(result.getUnbalancedNetworkAreas().isEmpty());
    }

    @Test
    public void testBalancedNetworkAfter1Scaling() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1200.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 1300.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("FFR1AA1 _generator"), Scalable.onGenerator("FFR2AA1 _generator"), Scalable.onGenerator("FFR3AA1 _generator")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("BBE1AA1 _generator"), Scalable.onGenerator("BBE3AA1 _generator"), Scalable.onGenerator("BBE2AA1 _generator")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        BalanceComputation balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());

    }

    @Test
    public void testBalancedNetworkAfter2Scaling() {

        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1200.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 1305.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("FFR1AA1 _generator"), Scalable.onGenerator("FFR2AA1 _generator"), Scalable.onGenerator("FFR3AA1 _generator")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("BBE1AA1 _generator"), Scalable.onGenerator("BBE3AA1 _generator"), Scalable.onGenerator("BBE2AA1 _generator")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        BalanceComputation balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getIterationCount());

    }

    @Test
    public void testUnBalancedNetwork() {

        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1200.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 1500.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("FFR1AA1 _generator"), Scalable.onGenerator("FFR2AA1 _generator"), Scalable.onGenerator("FFR3AA1 _generator")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("BBE1AA1 _generator"), Scalable.onGenerator("BBE3AA1 _generator"), Scalable.onGenerator("BBE2AA1 _generator")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

        BalanceComputation balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowFactory, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());
        assertEquals(2, result.getUnbalancedNetworkAreas().size());
    }
}
