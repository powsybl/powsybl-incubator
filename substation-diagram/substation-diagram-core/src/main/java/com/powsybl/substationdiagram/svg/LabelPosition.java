/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.substationdiagram.svg;

import java.util.Objects;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class LabelPosition {

    private final String positionName;

    private final double dX;

    private final double dY;

    public LabelPosition(String positionName, double dX, double dY) {
        this.positionName = Objects.requireNonNull(positionName);
        this.dX = dX;
        this.dY = dY;
    }

    public String getPositionName() {
        return positionName;
    }

    public double getdX() {
        return dX;
    }

    public double getdY() {
        return dY;
    }
}
