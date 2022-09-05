/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuit;
import com.powsybl.incubator.simulator.util.extensions.iidm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNormNone implements ShortCircuitNorm {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShortCircuitNormNone.class);

    @Override
    public double getCmaxVoltageFactor(double nominalVoltage) {
        return 1.0;
    }

    @Override
    public double getCminVoltageFactor(double nominalVoltage) {
        return 1.0;
    }

    @Override
    public double getKtT2W(TwoWindingsTransformer t2w) {
        return 1.0;
    }

    @Override
    public String getNormType() {
        return "NONE";
    }

    @Override
    public double getKg(Generator gen) {
        return 1.;
    }

    @Override
    public void setGenKg(Generator gen, double kg) {
        GeneratorShortCircuit2 extension2Gen = gen.getExtension(GeneratorShortCircuit2.class);
        if (extension2Gen != null) {
            extension2Gen.setkG(kg);
        } else {
            gen.newExtension(GeneratorShortCircuitAdder2.class)
                    .withKg(kg)
                    .add();
        }
    }

    @Override
    public void setKtT3Wi(ThreeWindingsTransformer t3w) {
        ThreeWindingsTransformerShortCircuit extension = t3w.getExtension(ThreeWindingsTransformerShortCircuit.class);
        if (extension == null) {
            t3w.newExtension(ThreeWindingsTransformerShortCircuitAdder.class)
                    .add();
            extension = t3w.getExtension(ThreeWindingsTransformerShortCircuit.class);
        }
        extension.getLeg1().setKtR(1.);
        extension.getLeg1().setKtX(1.);
        extension.getLeg2().setKtR(1.);
        extension.getLeg2().setKtX(1.);
        extension.getLeg3().setKtR(1.);
        extension.getLeg3().setKtX(1.);

        extension.getLeg1().setKtRo(1.);
        extension.getLeg1().setKtXo(1.);
        extension.getLeg2().setKtRo(1.);
        extension.getLeg2().setKtXo(1.);
        extension.getLeg3().setKtRo(1.);
        extension.getLeg3().setKtXo(1.);

    }

    public void adjustLoadfromInfo(Load load) {

    }

    public void adjustGenValuesWithFeederInputs(Generator gen) {
        GeneratorShortCircuit extension = gen.getExtension(GeneratorShortCircuit.class);
        if (extension == null) {
            throw new PowsyblException("Generator '" + gen.getId() + "' could not be adjusted with feeder values because of missing extension input data");
        }
        GeneratorShortCircuit2 extensions2 = gen.getExtension(GeneratorShortCircuit2.class);
        if (extensions2 == null) {
            throw new PowsyblException("Generator '" + gen.getId() + "' could not be adjusted with feeder values because of missing extension2 input data");
        }

        GeneratorShortCircuit2.GeneratorType genType = extensions2.getGeneratorType();
        if (genType != GeneratorShortCircuit2.GeneratorType.FEEDER) {
            throw new PowsyblException("Generator '" + gen.getId() + "' has wrong type to be adjusted");
        }
        double ikQmax = extensions2.getIkQmax();
        double maxR1ToX1Ratio = extensions2.getMaxR1ToX1Ratio();
        double cq = extensions2.getCq();

        // Zq = cq * Unomq / (sqrt3 * Ikq)
        // Zq = sqrt(r² + x²) which gives x = Zq / sqrt((R/X)² + 1)
        double zq = cq * gen.getTerminal().getVoltageLevel().getNominalV() / (Math.sqrt(3.) * ikQmax / 1000.); // ikQmax is changed from A to kA
        double xq = zq / Math.sqrt(maxR1ToX1Ratio * maxR1ToX1Ratio + 1.);
        double rq = xq * maxR1ToX1Ratio;

        extension.setDirectTransX(xq);
        extension.setDirectSubtransX(xq);
        extensions2.setTransRd(rq);
        extensions2.setSubTransRd(rq);
    }

    public void applyNormToT2W(Network network) {
        // Work on two windings transformers
        for (TwoWindingsTransformer t2w : network.getTwoWindingsTransformers()) {
            TwoWindingsTransformerShortCircuit extension = t2w.getExtension(TwoWindingsTransformerShortCircuit.class);
            double kNorm = getKtT2W(t2w);
            if (extension != null) { // grounding only exist if extension exists
                extension.setkNorm(kNorm);
            } else {
                t2w.newExtension(TwoWindingsTransformerShortCircuitAdder.class)
                        .withKnorm(kNorm)
                        .add();
            }
        }
    }

    public void applyNormToGenerators(Network network) {
        // Work on generators
        for (Generator gen : network.getGenerators()) {

            GeneratorShortCircuit2 extensions2 = gen.getExtension(GeneratorShortCircuit2.class);
            if (extensions2 != null) {
                GeneratorShortCircuit2.GeneratorType genType = extensions2.getGeneratorType();
                if (genType == GeneratorShortCircuit2.GeneratorType.FEEDER) {
                    adjustGenValuesWithFeederInputs(gen);
                } else {
                    // this includes standard rotating machines
                    double kg = getKg(gen);
                    setGenKg(gen, kg);
                }
            }
        }
    }

    public void applyNormToLoads(Network network) {
        // Work on loads
        for (Load load : network.getLoads()) {
            LoadShortCircuit extension = load.getExtension(LoadShortCircuit.class);
            if (extension == null) {
                continue; // we do not modify loads with no additional short circuit info
            }
            adjustLoadfromInfo(load);
        }
    }

    public void applyNormToT3w(Network network) {
        // Work on three Windings transformers
        for (ThreeWindingsTransformer t3w : network.getThreeWindingsTransformers()) {
            setKtT3Wi(t3w); // adjust coeffs to respect IEC norm
        }
    }

    @Override
    public void applyNormToNetwork(Network network) {

        applyNormToT2W(network); // the application of the norm to t2w includes generators with t2w associated to them
        applyNormToGenerators(network);
        applyNormToLoads(network);
        applyNormToT3w(network);
    }

    public double getCheckedCoef(String id, double ztk, double zt) {

        if (zt == 0.) {
            if (ztk != 0.) {
                LOGGER.warn("Transformer " + id +  " has r or x equal to zero and computed rk or xk is non null, short circuit calculation might be wrong");
            }
            return  1.;
        } else {
            return ztk / zt;
        }
    }
}
