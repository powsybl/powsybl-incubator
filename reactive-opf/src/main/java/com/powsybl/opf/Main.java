/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.opf;

import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.opf.parameters.OpenReacParameters;
import com.powsybl.opf.parameters.OpenReacResults;
import com.powsybl.opf.parameters.ReactiveInvestmentOutput;

import java.util.Properties;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        //        Network network = Importers.importData("XIIDM", "./", "ieee14", new Properties());
        Network network = Importers.importData("XIIDM", "./", "rte-iidm-borne-orig", new Properties());
        OpenReacParameters parameters = new OpenReacParameters();
        OpenReacResults openReacResults = OpenReacRunner.runOpenReac(network,
                network.getVariantManager().getWorkingVariantId(), parameters);
        System.out.println(openReacResults.getStatus());
        for (ReactiveInvestmentOutput.ReactiveInvestment investment : openReacResults.getReactiveInvestments()) {
            System.out.println(
                    "investment : " + investment.id + " " + investment.busId + " " + investment.substationId + " " + investment.slack);
        }
    }

}
