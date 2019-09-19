/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ieeecdf.model;

import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.google.common.base.Strings;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class BusTypeFormatter implements FixedFormatter<IeeeCdfBus.Type> {
    @Override
    public IeeeCdfBus.Type parse(String value, FormatInstructions instructions) {
        return IeeeCdfBus.Type.values()[Integer.parseInt(value.trim())];
    }

    @Override
    public String format(IeeeCdfBus.Type value, FormatInstructions instructions) {
        return Strings.padStart(Integer.toString(value.ordinal()), instructions.getLength(), ' ');
    }
}
