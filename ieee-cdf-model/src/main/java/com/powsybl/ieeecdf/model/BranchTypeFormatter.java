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
public class BranchTypeFormatter implements FixedFormatter<IeeeCdfBranch.Type> {
    @Override
    public IeeeCdfBranch.Type parse(String value, FormatInstructions instructions) {
        return IeeeCdfBranch.Type.values()[Integer.parseInt(value.trim())];
    }

    @Override
    public String format(IeeeCdfBranch.Type value, FormatInstructions instructions) {
        return Integer.toString(value.ordinal());
    }
}
