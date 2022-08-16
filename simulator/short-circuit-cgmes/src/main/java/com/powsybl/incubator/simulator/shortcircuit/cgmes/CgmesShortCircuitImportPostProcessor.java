/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit.cgmes;

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.conversion.CgmesImportPostProcessor;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuitAdder;
//import com.powsybl.incubator.simulator.util.extensions.iidm.GeneratorShortCircuit2;
import com.powsybl.incubator.simulator.util.extensions.iidm.GeneratorShortCircuitAdder2;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
@AutoService(CgmesImportPostProcessor.class)
public class CgmesShortCircuitImportPostProcessor implements CgmesImportPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CgmesShortCircuitImportPostProcessor.class);

    public static final String NAME = "ShortCircuit";

    @Override
    public String getName() {
        return NAME;
    }

    private final QueryCatalog queryCatalog = new QueryCatalog("short-circuit.sparql");

    public double epsilon = 0.000001;

    private void processSynchronousMachines(Network network, TripleStore tripleStore) {
        for (PropertyBag propertyBag : tripleStore.query(queryCatalog.get("synchronousMachineShortCircuit"))) {
            String id = propertyBag.getId("ID");
            double r0 = propertyBag.asDouble("r0");
            double r2 = propertyBag.asDouble("r2");
            double x0 = propertyBag.asDouble("x0");
            double x2 = propertyBag.asDouble("x2");
            double r = propertyBag.asDouble("r"); // given in ohms
            double subTransXdPu = propertyBag.asDouble("satDirectSubtransX"); // value given in pu
            double transXdPu = propertyBag.asDouble("satDirectTransX"); // value given in pu
            double ratedU = propertyBag.asDouble("ratedU");

            Generator generator = network.getGenerator(id);
            double genRatedS = generator.getRatedS();
            if (genRatedS == 0) {
                throw new IllegalArgumentException(" generator : " + generator.getId() + " has a rated S equal to zero");
            }
            if (ratedU == 0) {
                throw new IllegalArgumentException(" generator : " + generator.getId() + " has a rated U equal to zero");
            }

            double zBase = ratedU * ratedU / genRatedS; // TODO : ratedU and not Unom !!!!! for S2 10.5 kV instead of 16 kV
            double subTransXd = subTransXdPu * zBase;
            double transXd = transXdPu * zBase;
/*
            System.out.println(" ================>  GenId = " + id + " ro = " + r0 + " r2 = " + r2 +
                    " xo = " + x0 + " x2 = " + x2 + " r = " + r + " subTransXd = " + subTransXdPu + " transXd = " + transXdPu
                    + " Unom = " + ratedU + " Srated = " + genRatedS);*/

            double coeffRo = r0;
            double coeffR2 = r2;
            if (Math.abs(r) > epsilon) {
                coeffRo = r0 / r;
                coeffR2 = r2 / r;
            }

            double coeffXo = x0;
            double coeffX2 = x2;
            if (Math.abs(subTransXdPu) > epsilon) {
                coeffXo = x0 / subTransXdPu;
                coeffX2 = x2 / subTransXdPu;
            }

            generator.newExtension(GeneratorShortCircuitAdder.class)
                    .withStepUpTransformerX(0.)
                    .withDirectTransX(transXd)
                    .withDirectSubtransX(subTransXd)
                    .add();
            generator.newExtension(GeneratorShortCircuitAdder2.class)
                    .withSubTransRd(r)
                    .withTransRd(r)
                    .withCoeffRi(coeffR2)
                    .withCoeffXi(coeffX2)
                    .withCoeffRo(coeffRo)
                    .withCoeffXo(coeffXo)
                    .add();
        }
    }

    @Override
    public void process(Network network, TripleStore tripleStore) {
        LOGGER.info("Loading CGMES short circuit data...");
        processSynchronousMachines(network, tripleStore);
    }
}
