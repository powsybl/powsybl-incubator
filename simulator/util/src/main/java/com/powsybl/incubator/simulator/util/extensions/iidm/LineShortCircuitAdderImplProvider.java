/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.Line;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
@AutoService(ExtensionAdderProvider.class)
public class LineShortCircuitAdderImplProvider
        implements ExtensionAdderProvider<Line, LineShortCircuit, LineShortCircuitAdder> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public String getExtensionName() {
        return LineShortCircuit.NAME;
    }

    @Override
    public Class<LineShortCircuitAdder> getAdderClass() {
        return LineShortCircuitAdder.class;
    }

    @Override
    public LineShortCircuitAdder newAdder(Line line) {
        return new LineShortCircuitAdder(line);
    }
}
