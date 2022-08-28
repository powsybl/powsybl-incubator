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

    private void processExternalNetworkInjection(Network network, TripleStore tripleStore) {
        for (PropertyBag propertyBag : tripleStore.query(queryCatalog.get("externalNetworkInjectionShortCircuit"))) {
            String id = propertyBag.getId("ID");
            double ikQmax = propertyBag.asDouble("maxInitialSymShCCurrent");
            double maxR0ToX0Ratio = propertyBag.asDouble("maxR0ToX0Ratio");
            double maxR1ToX1Ratio = propertyBag.asDouble("maxR1ToX1Ratio");
            double maxZ0ToZ1Ratio = propertyBag.asDouble("maxZ0ToZ1Ratio");
            double voltageFactor = propertyBag.asDouble("voltageFactor");

            /*System.out.println(" ================>  ExternalNetworkInjectionId = " + id + " ikQmax = " + ikQmax
                    + " maxR0ToX0Ratio = " + maxR0ToX0Ratio + " maxR1ToX1Ratio = " + maxR1ToX1Ratio + " maxZ0ToZ1Ratio = " + maxZ0ToZ1Ratio
                    + " voltageFactor = " + voltageFactor);*/

            Generator generator = network.getGenerator(id);
            if (generator == null) {
                throw new PowsyblException("Generator '" + id + "' not found");
            }

            generator.newExtension(GeneratorShortCircuitAdder.class)
                    .withStepUpTransformerX(0.)
                    .withDirectTransX(0.)
                    .withDirectSubtransX(0.)
                    .add();
            generator.newExtension(GeneratorShortCircuitAdder2.class)
                    .withSubTransRd(0.)
                    .withTransRd(0.)
                    .withCoeffRi(0.)
                    .withCoeffXi(0.)
                    .withCoeffRo(0.)
                    .withCoeffXo(0.)
                    .withRatedU(0.)
                    .withCosPhi(0.)
                    .withGeneratorType(GeneratorShortCircuit2.GeneratorType.FEEDER)
                    .withMaxR1ToX1Ratio(maxR1ToX1Ratio) // extensions for feeder type
                    .withCq(voltageFactor)
                    .withIkQmax(ikQmax)
                    .add();
        }
    }

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
            double voltageRegulationRange = propertyBag.asDouble("voltageRegulationRange", 0.);

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

            double zBase = ratedU * ratedU / genRatedS;
            double subTransXd = subTransXdPu * zBase;
            double transXd = transXdPu * zBase;

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
                    .withGeneratorType(GeneratorShortCircuit2.GeneratorType.ROTATING_MACHINE)
                    .withVoltageRegulationRange(voltageRegulationRange)
                    .add();
        }
    }

    private void processAsynchronousMachines(Network network, TripleStore tripleStore) {
        for (PropertyBag propertyBag : tripleStore.query(queryCatalog.get("asynchronousMachineShortCircuit"))) {
            String id = propertyBag.getId("ID");
            double ratedMechanicalPower = propertyBag.asDouble("ratedMechanicalPower");
            double ratedPowerFactor = propertyBag.asDouble("ratedPowerFactor");
            double ratedS = propertyBag.asDouble("ratedS");
            double ratedU = propertyBag.asDouble("ratedU");
            double efficiency = propertyBag.asDouble("efficiency");
            double iaIrRatio = propertyBag.asDouble("iaIrRatio");
            double rxLockedRotorRatio = propertyBag.asDouble("rxLockedRotorRatio");
            int polePairNumber = propertyBag.asInt("polePairNumber");

            Load load = network.getLoad(id);
            if (load == null) {
                throw new PowsyblException("Load '" + id + "' not found");
            }

            load.newExtension(LoadShortCircuitAdder.class)
                    .withEfficiency(efficiency)
                    .withIaIrRatio(iaIrRatio)
                    .withPolePairNumber(polePairNumber)
                    .withRatedMechanicalP(ratedMechanicalPower)
                    .withRatedS(ratedS)
                    .withRatedU(ratedU)
                    .withRxLockedRotorRatio(rxLockedRotorRatio)
                    .withRatedPowerFactor(ratedPowerFactor)
                    .withLoadShortCircuitType(LoadShortCircuit.LoadShortCircuitType.ASYNCHRONOUS_MACHINE)
                    .add();

            /*System.out.println(" ================>  LoadId = " + id + " ratedMechanicalPower = " + ratedMechanicalPower + " ratedPowerFactor = " + ratedPowerFactor +
                    " ratedS = " + ratedS + " efficiency = " + efficiency + " iaIrRatio = " + iaIrRatio + " rxLockedRotorRatio = " + rxLockedRotorRatio +
                    " Unom = " + ratedU + " polePairNumber = " + polePairNumber);*/

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
                double x = line.getX();
                double r = line.getR();
                double coeffRo = 1.;
                double coeffXo = 1.;
                if (x != 0.) {
                    coeffXo = x0 / x;
                }
                if (r != 0.) {
                    coeffRo = r0 / r;
                }
                line.newExtension(LineShortCircuitAdder.class)
                        .withCoeffRo(coeffRo)
                        .withCoeffXo(coeffXo)
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

    private void processPowerTransformers(Network network, TripleStore tripleStore) {
        for (PropertyBag propertyBag : tripleStore.query(queryCatalog.get("powerTransformerShortCircuit"))) {
            String id = propertyBag.getId("ID");
            boolean isPartOfGeneratingUnit = propertyBag.asBoolean("isPartOfGeneratorUnit", false);

            TwoWindingsTransformer t2wt = network.getTwoWindingsTransformer(id);
            if (t2wt != null) {
                TwoWindingsTransformerShortCircuit extension = t2wt.getExtension(TwoWindingsTransformerShortCircuit.class);
                if (extension == null) {
                    t2wt.newExtension(TwoWindingsTransformerShortCircuitAdder.class)
                            .withIsPartOfGeneratingUnit(isPartOfGeneratingUnit)
                            .add();
                } else {
                    extension.setPartOfGeneratingUnit(isPartOfGeneratingUnit);
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
            double ratedS = propertyBag.asDouble("ratedS");
            double rground = propertyBag.asDouble("rground");
            double xground = propertyBag.asDouble("xground");
            boolean grounded = propertyBag.asBoolean("grounded", false);

            TwoWindingsTransformer t2wt = network.getTwoWindingsTransformer(id);
            if (t2wt != null) {
                t2wt.setRatedS(ratedS); // rated S not not filled in CGMES import example for transformers
                TwoWindingsTransformerShortCircuit extension = t2wt.getExtension(TwoWindingsTransformerShortCircuit.class);
                /*if (extension == null) {
                    t2wt.newExtension(TwoWindingsTransformerShortCircuitAdder.class)
                            .add();
                    //extension = t2wt.getExtension(TwoWindingsTransformerShortCircuit.class);
                }*/
                // TODO
            } else {
                ThreeWindingsTransformer t3wt = network.getThreeWindingsTransformer(id);
                if (t3wt != null) {
                    if (endNumber == 1) {
                        t3wt.getLeg1().setRatedS(ratedS);
                    } else if (endNumber == 2) {
                        t3wt.getLeg2().setRatedS(ratedS);
                    } else if (endNumber == 3) {
                        t3wt.getLeg3().setRatedS(ratedS);
                    } else {
                        throw new PowsyblException("incorrect end number for 3 windings transformer end '" + id + "'");
                    }
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
        processPowerTransformers(network, tripleStore);
        processPowerTransformerEnds(network, tripleStore);
        processExternalNetworkInjection(network, tripleStore);
    }
}
