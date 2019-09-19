/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SeasonFormatter implements FixedFormatter<IeeeCdfTitle.Season> {
    @Override
    public IeeeCdfTitle.Season parse(String str, FormatInstructions instructions) {
        switch (str.charAt(0)) {
            case 'S':
                return IeeeCdfTitle.Season.SUMMER;
            case 'W':
                return IeeeCdfTitle.Season.WINTER;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public String format(IeeeCdfTitle.Season season, FormatInstructions instructions) {
        switch (season) {
            case SUMMER:
                return "S";
            case WINTER:
                return "W";
            default:
                throw new AssertionError();
        }
    }
}
