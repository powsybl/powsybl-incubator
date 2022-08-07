/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.openloadflow.network.LfBus;

import java.util.List;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class EquationSystemBusFeeders {

    private List<EquationSystemFeeder> feeders;

    private LfBus feedersBus;

    public EquationSystemBusFeeders(List<EquationSystemFeeder> feeders, LfBus bus) {
        this.feeders = feeders;
        this.feedersBus = bus;
    }

    public List<EquationSystemFeeder> getFeeders() {
        return feeders;
    }

}