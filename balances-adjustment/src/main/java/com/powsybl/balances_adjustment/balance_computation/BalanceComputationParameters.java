/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

/**
 * parameters for balance computation.
 * @author Ameni Walha <ameni.walha at rte-france.com>
 */
public class BalanceComputationParameters {

    /**
     * Threshold for comparing net positions (given in MW).
     * Under this threshold, the network area is balanced
     */
    private final double thresholdNetPosition;

    /**
     * Maximum iteration number for balances adjustment
     */
    private final int maxNumberIterations;

    private static final double DEFAULT_THRESHOLD_NETPOSITION = 1;

    private static final int DEFAULT_MAX_NUMBER_ITERATIONS = 5;


    /**
     * Constructor with default parameters
     */
    public BalanceComputationParameters() {
        this(DEFAULT_THRESHOLD_NETPOSITION, DEFAULT_MAX_NUMBER_ITERATIONS);
    }

    /**
     * Constructor with given parameters
     * @param threshold Threshold for comparing net positions (given in MW)
     * @param max Maximum iteration number for balances adjustment
     */
    public BalanceComputationParameters(double threshold, int max) {
        this.thresholdNetPosition = threshold;
        this.maxNumberIterations = max;
    }

    public double getThresholdNetPosition() {
        return thresholdNetPosition;
    }

    public int getMaxNumberIterations() {
        return maxNumberIterations;
    }
}
