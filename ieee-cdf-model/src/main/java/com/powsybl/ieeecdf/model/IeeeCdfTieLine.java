/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import com.ancientprogramming.fixedformat4j.annotation.Record;

import java.util.Objects;

/**
 * Columns  1- 4   Metered bus number (I)
 * Columns  7-8    Metered area number (I)
 * Columns  11-14  Non-metered bus number (I)
 * Columns  17-18  Non-metered area number (I)
 * Column   21     Circuit number
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Record
public class IeeeCdfTieLine {

    private int meteredBusNumber;
    private int meteredAreaNumber;
    private int nonMeteredBusNumber;
    private int nonMeteredAreaNumber;
    private int circuitNumber;

    public int getMeteredBusNumber() {
        return meteredBusNumber;
    }

    public void setMeteredBusNumber(int meteredBusNumber) {
        this.meteredBusNumber = meteredBusNumber;
    }

    public int getMeteredAreaNumber() {
        return meteredAreaNumber;
    }

    public void setMeteredAreaNumber(int meteredAreaNumber) {
        this.meteredAreaNumber = meteredAreaNumber;
    }

    public int getNonMeteredBusNumber() {
        return nonMeteredBusNumber;
    }

    public void setNonMeteredBusNumber(int nonMeteredBusNumber) {
        this.nonMeteredBusNumber = nonMeteredBusNumber;
    }

    public int getNonMeteredAreaNumber() {
        return nonMeteredAreaNumber;
    }

    public void setNonMeteredAreaNumber(int nonMeteredAreaNumber) {
        this.nonMeteredAreaNumber = nonMeteredAreaNumber;
    }

    public int getCircuitNumber() {
        return circuitNumber;
    }

    public void setCircuitNumber(int circuitNumber) {
        this.circuitNumber = circuitNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(meteredBusNumber, meteredAreaNumber, nonMeteredBusNumber, nonMeteredAreaNumber,
                            circuitNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IeeeCdfTieLine) {
            IeeeCdfTieLine other = (IeeeCdfTieLine) obj;
            return  meteredBusNumber == other.meteredBusNumber
                    && meteredAreaNumber == other.meteredAreaNumber
                    && nonMeteredBusNumber == other.nonMeteredBusNumber
                    && nonMeteredAreaNumber == other.nonMeteredAreaNumber
                    && circuitNumber == other.circuitNumber;
        }
        return false;
    }
}
