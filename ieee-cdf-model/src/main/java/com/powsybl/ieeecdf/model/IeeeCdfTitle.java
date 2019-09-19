/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Columns  2- 9   Date, in format DD/MM/YY with leading zeros. If no date provided, use 0b/0b/0b where b is blank.
 * Columns 11-30   Originator's name (A)
 * Columns 32-37   MVA Base (F*)
 * Columns 39-42   Year (I)
 * Column  44      Season (S - Summer, W - Winter)
 * Column  46-73   Case identification (A)
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Record
public class IeeeCdfTitle {

    public enum Season {
        SUMMER,
        WINTER
    }

    private LocalDate date;
    private String originatorName;
    private float mvaBase;
    private int year;
    private Season season;
    private String caseIdentification;

    /**
     * Date
     */
    @Field(offset = 2, length = 8, formatter = LocalDateFormatter.class)
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * Originator's name
     */
    @Field(offset = 11, length = 20)
    public String getOriginatorName() {
        return originatorName;
    }

    public void setOriginatorName(String originatorName) {
        this.originatorName = originatorName;
    }

    /**
     * MVA Base
     */
    @Field(offset = 32, length = 6)
    @FixedFormatDecimal(decimals = 1, useDecimalDelimiter = true)
    public float getMvaBase() {
        return mvaBase;
    }

    public void setMvaBase(float mvaBase) {
        this.mvaBase = mvaBase;
    }

    /**
     * Year
     */
    @Field(offset = 39, length = 4)
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    /**
     *  Season
     */
    @Field(offset = 44, length = 1, formatter = SeasonFormatter.class)
    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    /**
     * Case identification
     */
    @Field(offset = 46, length = 28)
    public String getCaseIdentification() {
        return caseIdentification;
    }

    public void setCaseIdentification(String caseIdentification) {
        this.caseIdentification = caseIdentification;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, originatorName, mvaBase, year, season, caseIdentification);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IeeeCdfTitle) {
            IeeeCdfTitle other = (IeeeCdfTitle) obj;
            return Objects.equals(date, other.date)
                    && Objects.equals(originatorName, other.originatorName)
                    && mvaBase == other.mvaBase
                    && year == other.year
                    && season == other.season
                    && Objects.equals(caseIdentification, other.caseIdentification);
        }
        return false;
    }

    public static void main(String[] args) {
        String a = " 08/19/93 UW ARCHIVE           100.0  1962 W IEEE 14 Bus Test Case";
        FixedFormatManager manager = new FixedFormatManagerImpl();
        IeeeCdfTitle record = manager.load(IeeeCdfTitle.class, a);
        System.out.println(a);
        System.out.println(manager.export(record));
    }
}
