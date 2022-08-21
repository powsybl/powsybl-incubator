package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.Load;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
@AutoService(ExtensionAdderProvider.class)
public class LoadShortCircuitAdderImplProvider
        implements ExtensionAdderProvider<Load, LoadShortCircuit, LoadShortCircuitAdder> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public String getExtensionName() {
        return LoadShortCircuit.NAME;
    }

    @Override
    public Class<LoadShortCircuitAdder> getAdderClass() {
        return LoadShortCircuitAdder.class;
    }

    @Override
    public LoadShortCircuitAdder newAdder(Load load) {
        return new LoadShortCircuitAdder(load);
    }
}
