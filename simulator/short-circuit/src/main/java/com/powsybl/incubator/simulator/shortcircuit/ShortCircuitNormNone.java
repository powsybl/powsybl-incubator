/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.*;
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

        T3wCoefs t3wCoefs = getKtT3Wi(t3w);
        extension.getLeg1().setKtR(t3wCoefs.ktr1);
        extension.getLeg1().setKtX(t3wCoefs.ktx1);
        extension.getLeg2().setKtR(t3wCoefs.ktr2);
        extension.getLeg2().setKtX(t3wCoefs.ktx2);
        extension.getLeg3().setKtR(t3wCoefs.ktr3);
        extension.getLeg3().setKtX(t3wCoefs.ktx3);

        extension.getLeg1().setKtRo(t3wCoefs.ktro1);
        extension.getLeg1().setKtXo(t3wCoefs.ktxo1);
        extension.getLeg2().setKtRo(t3wCoefs.ktro2);
        extension.getLeg2().setKtXo(t3wCoefs.ktxo2);
        extension.getLeg3().setKtRo(t3wCoefs.ktro3);
        extension.getLeg3().setKtXo(t3wCoefs.ktxo3);

        if (t3wCoefs.updateCoefo) {
            extension.getLeg1().setLegCoeffRo(t3wCoefs.coefro1);
            extension.getLeg1().setLegCoeffXo(t3wCoefs.coefxo1);
            extension.getLeg2().setLegCoeffRo(t3wCoefs.coefro2);
            extension.getLeg2().setLegCoeffXo(t3wCoefs.coefxo2);
            extension.getLeg3().setLegCoeffRo(t3wCoefs.coefro3);
            extension.getLeg3().setLegCoeffXo(t3wCoefs.coefxo3);
        }

    }

    public T3wCoefs getKtT3Wi(ThreeWindingsTransformer t3w) {
        return new T3wCoefs(1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1.);
    }

    public void adjustLoadfromInfo(Load load) {
        // TODO : must be modified once load will be disaggregated

    }

    public void computeLoadsZeq(Network network) {
        // uses available info of each load to deduce the equivalent impedance to be used in the admittance matrix
        for (Load load : network.getLoads()) {

            Pair<Double, Double> rdXd = getZeqLoad(load);
            double rd = rdXd.getFirst();
            double xd = rdXd.getSecond();

            LoadShortCircuit extension = load.getExtension(LoadShortCircuit.class);
            if (extension == null) {
                load.newExtension(LoadShortCircuitAdder.class)
                        .add();
                LoadShortCircuit extensionLoad = load.getExtension(LoadShortCircuit.class);
                extensionLoad.setLoadShortCircuitType(LoadShortCircuit.LoadShortCircuitType.CONSTANT_LOAD);
                extension = extensionLoad;
            }
            extension.setXdEquivalent(xd);
            extension.setRdEquivalent(rd);
        }
    }

    public Pair<Double, Double> getZeqLoad(Load load) {
        double pLoad = load.getP0();
        double qLoad = load.getQ0();
        double uNom = load.getTerminal().getVoltageLevel().getNominalV();
        double s2 = pLoad * pLoad + qLoad * qLoad;
        // using formula P(MW) = Re(Z) * |V|² / |Z|² and Q(MVAR) = Im(Z) * |V|² / |Z|²  or  Z = |V|² / (P-jQ)
        // We compute the equivalent impedance at Unom
        double xd = 0.;
        double rd = 0.;
        if (s2 > EPSILON) {
            xd = qLoad * uNom * uNom / s2;
            rd = pLoad * uNom * uNom / s2;
        }

        return new Pair<>(rd, xd);
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
                    //adjustGenValuesWithFeederInputs(gen);
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
        computeLoadsZeq(network);

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

        this.network = network;
        applyNormToT2W(network); // the application of the norm to t2w includes generators with t2w associated to them
        applyNormToGenerators(network);
        applyNormToLoads(network);
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
