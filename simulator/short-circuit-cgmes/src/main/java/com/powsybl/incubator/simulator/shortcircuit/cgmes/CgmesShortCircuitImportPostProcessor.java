/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit.cgmes;

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.conversion.CgmesImportPostProcessor;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuitAdder;
import com.powsybl.incubator.simulator.util.extensions.iidm.*;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static final double EPSILON = 0.000001;

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
            double ratedPowerFactor = propertyBag.asDouble("ratedPowerFactor");

            Generator generator = network.getGenerator(id);
            if (generator == null) {
                throw new PowsyblException("Generator '" + id + "' not found");
            }
            double genRatedS = generator.getRatedS();
            if (genRatedS == 0) {
                throw new PowsyblException("Generator '" + generator.getId() + "' has a rated S equal to zero");
            }
            if (ratedU == 0) {
                throw new PowsyblException("Generator '" + generator.getId() + "' has a rated U equal to zero");
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
            if (Math.abs(r) > EPSILON) {
                coeffRo = r0 / r;
                coeffR2 = r2 / r;
            }

            double coeffXo = x0;
            double coeffX2 = x2;
            if (Math.abs(subTransXdPu) > EPSILON) {
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
                    .withRatedU(ratedU)
                    .withCosPhi(ratedPowerFactor)
                    .add();
        }
    }

    private void processAsynchronousMachines(Network network, TripleStore tripleStore) {
        for (PropertyBag propertyBag : tripleStore.query(queryCatalog.get("asynchronousMachineShortCircuit"))) {
            String id = propertyBag.getId("ID");
            Load load = network.getLoad(id);
            if (load == null) {
                throw new PowsyblException("Load '" + id + "' not found");
            }
            // FIXME create a shortcircuit load extension ?
            // TODO
            // load.newExtension(AsynchronousGeneratorCircuitAdder.class)
            //     ...
            //     .add();
        }
    }

    private void processAcLineSegments(Network network, TripleStore tripleStore) {
        for (PropertyBag propertyBag : tripleStore.query(queryCatalog.get("acLineSegmentShortCircuit"))) {
            String id = propertyBag.getId("ID");
            double r0 = propertyBag.asDouble("r0");
            double x0 = propertyBag.asDouble("x0");
            double g0ch = propertyBag.asDouble("g0ch");
            double b0ch = propertyBag.asDouble("b0ch");
            Line line = network.getLine(id);
            if (line != null) {
                // TODO
                line.newExtension(LineShortCircuitAdder.class)
                        // TODO
//                        .withCoeffRo()
//                        .withCoeffXo()
                        .add();
            } else {
                DanglingLine danglingLine = network.getDanglingLine(id);
                if (danglingLine != null) {
                    // FIXME LineShortCircuitAdder should be compatible with dangling lines
                    // TODO
                } else {
                    throw new PowsyblException("Line or dangling line not found: '" + id + "'");
                }
            }
        }
    }

    private void processPowerTransformerEnds(Network network, TripleStore tripleStore) {
        for (PropertyBag propertyBag : tripleStore.query(queryCatalog.get("powerTransformerEndShortCircuit"))) {
            String id = propertyBag.getId("ID");
            int endNumber = propertyBag.asInt("endNumber");
            double r0 = propertyBag.asDouble("r0");
            double x0 = propertyBag.asDouble("x0");
            double g0 = propertyBag.asDouble("g0");
            double b0 = propertyBag.asDouble("b0");
            double rground = propertyBag.asDouble("rground");
            double xground = propertyBag.asDouble("xground");
            boolean grounded = propertyBag.asBoolean("grounded", false);
            TwoWindingsTransformer t2wt = network.getTwoWindingsTransformer(id);
            if (t2wt != null) {
                TwoWindingsTransformerShortCircuit extension = t2wt.getExtension(TwoWindingsTransformerShortCircuit.class);
                if (extension == null) {
                    t2wt.newExtension(TwoWindingsTransformerShortCircuitAdder.class)
                            // TODO
                            .add();
                    extension = t2wt.getExtension(TwoWindingsTransformerShortCircuit.class);
                }
                // TODO
            } else {
                ThreeWindingsTransformer t3wt = network.getThreeWindingsTransformer(id);
                if (t3wt != null) {
                    ThreeWindingsTransformerShortCircuit extension = t3wt.getExtension(ThreeWindingsTransformerShortCircuit.class);
                    if (extension == null) {
                        t3wt.newExtension(ThreeWindingsTransformerShortCircuitAdder.class)
                                // TODO
                                .add();
                        extension = t3wt.getExtension(ThreeWindingsTransformerShortCircuit.class);
                    }
                    // TODO
                } else {
                    throw new PowsyblException("2 or 3 windings transformer not found: '" + id + "'");
                }
            }
        }
    }

    @Override
    public void process(Network network, TripleStore tripleStore) {
        LOGGER.info("Loading CGMES short circuit data...");
        processSynchronousMachines(network, tripleStore);
        processAsynchronousMachines(network, tripleStore);
        processAcLineSegments(network, tripleStore);
        processPowerTransformerEnds(network, tripleStore);
    }
}
