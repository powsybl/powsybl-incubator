/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.temporal.ChronoField.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LocalDateFormatter implements FixedFormatter<LocalDate> {

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('/')
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('/')
            .appendValue(YEAR, 2)
            .toFormatter();

    private static final String INVALID_DATE = "0 /0 /0 ";

    @Override
    public LocalDate parse(String s, FormatInstructions formatInstructions) {
        if (!s.equals(INVALID_DATE)) {
            return LocalDate.parse(s, FORMATTER);
        }
        return null;
    }

    @Override
    public String format(LocalDate localDate, FormatInstructions formatInstructions) {
        return localDate != null ? FORMATTER.format(localDate) : INVALID_DATE;
    }
}
