/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.action.util.Scalable;
import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;

import java.util.Map;

/**
 * Balance computation factory interface
 *
 * @author Ameni Walha <ameni.walha at rte-france.com>
 */
public interface BalanceComputationFactory {

    BalanceComputation create(Network network, Map<NetworkArea, Double> networkAreaNetPositionTargetMap, Map<NetworkArea, Scalable> networkAreasScalable, LoadFlow.Runner loadFlowRunner, ComputationManager computationManager, int priority);
}
