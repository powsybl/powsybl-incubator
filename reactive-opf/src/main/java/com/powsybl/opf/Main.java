/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.opf;

import java.util.Properties;

import com.powsybl.ampl.executor.AmplModelRunner;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Network network = Importers.importData("XIIDM", "./", "ieee14", new Properties());
        String variantId = network.getVariantManager().getWorkingVariantId();
        AmplModel reactiveOpf = AmplModel.REACTIVE_OPF;
        ComputationManager manager = LocalComputationManager.getDefault();
        network.getGeneratorStream().forEach(gen -> System.out.println(gen.getNameOrId() + " : " + gen.getTargetV()));
        AmplModelRunner.run(network, variantId, reactiveOpf, manager);
        network.getGeneratorStream().forEach(gen -> System.out.println(gen.getNameOrId() + " : " + gen.getTargetV()));
    }
}
