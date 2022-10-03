/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.*;
import com.powsybl.incubator.simulator.util.extensions.*;
import com.powsybl.incubator.simulator.util.extensions.iidm.*;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNormNone implements ShortCircuitNorm {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShortCircuitNormNone.class);

    public static final double EPSILON = 0.000001;

    public Network network;

    @Override
    public Network getNetwork() {
        return network;
    }

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
    public void setKg(Generator gen, double kg) {

        GeneratorNorm extensionGenNorm = gen.getExtension(GeneratorNorm.class);
        if (extensionGenNorm != null) {
            extensionGenNorm.setkG(kg);
        } else {
            gen.newExtension(GeneratorNormAdder.class)
                    .withKg(kg)
                    .add();
        }

    }

    @Override
    public void setKtT3Wi(ThreeWindingsTransformer t3w) {
        T3wCoefs t3wCoefs = getKtT3Wi(t3w);

        ThreeWindingsTransformerNorm extensionNorm = t3w.getExtension(ThreeWindingsTransformerNorm.class);
        if (extensionNorm == null) {
            t3w.newExtension(ThreeWindingsTransformerNormAdder.class)
                    .add();
            extensionNorm = t3w.getExtension(ThreeWindingsTransformerNorm.class);
        }

        extensionNorm.getLeg1().setKtR(t3wCoefs.ktr1);
        extensionNorm.getLeg1().setKtX(t3wCoefs.ktx1);
        extensionNorm.getLeg2().setKtR(t3wCoefs.ktr2);
        extensionNorm.getLeg2().setKtX(t3wCoefs.ktx2);
        extensionNorm.getLeg3().setKtR(t3wCoefs.ktr3);
        extensionNorm.getLeg3().setKtX(t3wCoefs.ktx3);

        extensionNorm.getLeg1().setKtRo(t3wCoefs.ktro1);
        extensionNorm.getLeg1().setKtXo(t3wCoefs.ktxo1);
        extensionNorm.getLeg2().setKtRo(t3wCoefs.ktro2);
        extensionNorm.getLeg2().setKtXo(t3wCoefs.ktxo2);
        extensionNorm.getLeg3().setKtRo(t3wCoefs.ktro3);
        extensionNorm.getLeg3().setKtXo(t3wCoefs.ktxo3);

        if (t3wCoefs.updateCoefo) {
            extensionNorm.setOverloadHomopolarCoefs(true);
            extensionNorm.getLeg1().setLegCoeffRoOverload(t3wCoefs.coefro1);
            extensionNorm.getLeg1().setLegCoeffXoOverload(t3wCoefs.coefxo1);
            extensionNorm.getLeg2().setLegCoeffRoOverload(t3wCoefs.coefro2);
            extensionNorm.getLeg2().setLegCoeffXoOverload(t3wCoefs.coefxo2);
            extensionNorm.getLeg3().setLegCoeffRoOverload(t3wCoefs.coefro3);
            extensionNorm.getLeg3().setLegCoeffXoOverload(t3wCoefs.coefxo3);
        }

    }

    @Override
    public T3wCoefs getKtT3Wi(ThreeWindingsTransformer t3w) {
        return new T3wCoefs(1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.);
    }

    public Pair<Double, Double>  getAdjustedLoadfromInfo(Load load, double defaultRd, double defaultXd) {

        return new Pair<>(defaultRd, defaultXd);
    }

    public void applyNormToT2W(Network network) {
        // Work on two windings transformers
        for (TwoWindingsTransformer t2w : network.getTwoWindingsTransformers()) {
            double kNorm = getKtT2W(t2w);

            TwoWindingsTransformerNorm extensionNorm = t2w.getExtension(TwoWindingsTransformerNorm.class);
            if (extensionNorm != null) {
                extensionNorm.setkNorm(kNorm);
            } else {
                t2w.newExtension(TwoWindingsTransformerNormAdder.class)
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
                    //adjustGenValuesWithFeederInputs(gen);
                } else {
                    // this includes standard rotating machines
                    double kg = getKg(gen);
                    setKg(gen, kg);
                }
            }
        }
    }

    public void applyNormToT3w(Network network) {
        // Work on three Windings transformers
        for (ThreeWindingsTransformer t3w : network.getThreeWindingsTransformers()) {
            setKtT3Wi(t3w); // adjust coeffs to respect norm
        }
    }

    @Override
    public void applyNormToNetwork(Network network) {

        this.network = network;
        applyNormToT2W(network); // the application of the norm to t2w includes generators with t2w associated to them
        applyNormToGenerators(network);
        applyNormToT3w(network);
    }

    public double getCheckedCoef(String id, double ztk, double zt) {

        if (zt == 0.) {
            if (ztk != 0.) {
                LOGGER.warn("Transformer {} has r or x equal to zero and computed rk or xk is non null, short circuit calculation might be wrong", id);
            }
            return  1.;
        } else {
            return ztk / zt;
        }
    }
}
