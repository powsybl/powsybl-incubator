/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.iidm.network.ShuntCompensator;

import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface LfBus {

    String getId();

    int getNum();

    LfBusType getType();

    double getTargetP();

    double getTargetQ();

    double getTargetV();

    double getV();

    void setV(double v);

    double getAngle();

    void setAngle(double angle);

    double getNominalV();

    List<ShuntCompensator> getShuntCompensators();
}
