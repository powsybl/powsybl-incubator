/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
@AutoService(ExtensionAdderProvider.class)
public class TwoWindingsTransformerShortCircuitAdderImplProvider
        implements ExtensionAdderProvider<TwoWindingsTransformer, TwoWindingsTransformerShortCircuit, TwoWindingsTransformerShortCircuitAdder> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public String getExtensionName() {
        return TwoWindingsTransformerShortCircuit.NAME;
    }

    @Override
    public Class<TwoWindingsTransformerShortCircuitAdder> getAdderClass() {
        return TwoWindingsTransformerShortCircuitAdder.class;
    }

    @Override
    public TwoWindingsTransformerShortCircuitAdder newAdder(TwoWindingsTransformer twt) {
        return new TwoWindingsTransformerShortCircuitAdder(twt);
    }
}
