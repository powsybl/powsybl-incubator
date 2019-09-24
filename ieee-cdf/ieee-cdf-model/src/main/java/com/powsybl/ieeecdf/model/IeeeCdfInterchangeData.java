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
 * Columns  1- 2   Area number (I) no zeros! *
 * Columns  4- 7   Interchange slack bus number (I) *
 * Columns  9-20   Alternate swing bus name (A)
 * Columns 21-28   Area interchange export, MW (F) (+ = out) *
 * Columns 30-35   Area interchange tolerance, MW (F) *
 * Columns 38-43   Area code (abbreviated name) (A) *
 * Columns 46-75   Area name (A)
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Record
public class IeeeCdfInterchangeData {

    private int areaNumber;

    private int interchangeSlackBusNumber;

    private String alternateSwingBusName;

    private float areaInterchangeExport;

    private float areaInterchangeTolerance;

    private String areaCode;

    private String areaName;

    /**
     * Area number (I) no zeros! *
     */
    @Field(offset = 1, length = 2, align = Align.RIGHT)
    public int getAreaNumber() {
        return areaNumber;
    }

    public void setAreaNumber(int areaNumber) {
        this.areaNumber = areaNumber;
    }

    public int getInterchangeSlackBusNumber() {
        return interchangeSlackBusNumber;
    }

    public void setInterchangeSlackBusNumber(int interchangeSlackBusNumber) {
        this.interchangeSlackBusNumber = interchangeSlackBusNumber;
    }

    public String getAlternateSwingBusName() {
        return alternateSwingBusName;
    }

    public void setAlternateSwingBusName(String alternateSwingBusName) {
        this.alternateSwingBusName = alternateSwingBusName;
    }

    public float getAreaInterchangeExport() {
        return areaInterchangeExport;
    }

    public void setAreaInterchangeExport(float areaInterchangeExport) {
        this.areaInterchangeExport = areaInterchangeExport;
    }

    public float getAreaInterchangeTolerance() {
        return areaInterchangeTolerance;
    }

    public void setAreaInterchangeTolerance(float areaInterchangeTolerance) {
        this.areaInterchangeTolerance = areaInterchangeTolerance;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(areaNumber, interchangeSlackBusNumber, alternateSwingBusName, areaInterchangeExport,
                            areaInterchangeTolerance, areaCode, areaName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IeeeCdfInterchangeData) {
            IeeeCdfInterchangeData other = (IeeeCdfInterchangeData) obj;
            return areaNumber == other.areaNumber
                    && interchangeSlackBusNumber == other.interchangeSlackBusNumber
                    && Objects.equals(alternateSwingBusName, other.alternateSwingBusName)
                    && areaInterchangeExport == other.areaInterchangeExport
                    && areaInterchangeTolerance == other.areaInterchangeTolerance
                    && Objects.equals(areaCode, other.areaCode)
                    && Objects.equals(areaName, other.areaName);
        }
        return false;
    }
}
