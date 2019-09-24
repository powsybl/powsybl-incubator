/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;

import java.util.Objects;

/**
 * Columns  1- 3   Loss zone number  (I)
 * Columns  5-16   Loss zone name (A)
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Record
public class IeeeCdfLossZone {

    private int number;

    private String name;

    /**
     * Loss zone number  (I)
     */
    @Field(offset = 1, length = 3, align = Align.RIGHT)
    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * Loss zone name (A)
     */
    @Field(offset = 5, length = 12, align = Align.RIGHT)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IeeeCdfLossZone) {
            IeeeCdfLossZone other = (IeeeCdfLossZone) obj;
            return number == other.number
                    && Objects.equals(name, other.name);
        }
        return false;
    }
}
