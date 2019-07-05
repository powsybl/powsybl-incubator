/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.ac;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.loadflow.LoadFlowParameters;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SimpleAcLoadFlowParameters extends AbstractExtension<LoadFlowParameters> {

    private SlackBusSelectionMode slackBusSelectionMode = SlackBusSelectionMode.MOST_MESHED;

    private boolean distributedSlack = true;

    @Override
    public String getName() {
        return "SimpleLoadFlowParameters";
    }

    public SlackBusSelectionMode getSlackBusSelectionMode() {
        return slackBusSelectionMode;
    }

    public SimpleAcLoadFlowParameters setSlackBusSelectionMode(SlackBusSelectionMode slackBusSelectionMode) {
        this.slackBusSelectionMode = Objects.requireNonNull(slackBusSelectionMode);
        return this;
    }

    public boolean isDistributedSlack() {
        return distributedSlack;
    }

    public SimpleAcLoadFlowParameters setDistributedSlack(boolean distributedSlack) {
        this.distributedSlack = distributedSlack;
        return this;
    }
}
