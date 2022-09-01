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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNormIec extends ShortCircuitNormNone {

    public static final double LOW_VOLTAGE_KV_THRESHOLD = 1.;
    public static final double MEDIUM_VOLTAGE_KV_THRESHOLD = 35.;

    public static final double LOW_VOLTAGE_6PERCENT_CMAX = 1.05;
    public static final double LOW_VOLTAGE_10PERCENT_CMAX = 1.10;
    public static final double MEDIUM_VOLTAGE_CMAX = 1.10;
    public static final double HIGH_VOLTAGE_MAX_CMAX = 1.10;

    public static final double LOW_VOLTAGE_CMIN = 0.95;
    public static final double MEDIUM_VOLTAGE_CMIN = 1.0;
    public static final double HIGH_VOLTAGE_MAX_CMIN = 1.0;

    List<Generator> generatorsWithTfo;

    @Override
    public double getCmaxVoltageFactor(double nominalVoltage) {
        double cmax;
        if (nominalVoltage <= LOW_VOLTAGE_KV_THRESHOLD) {
            cmax = LOW_VOLTAGE_6PERCENT_CMAX; //TODO : make the distinction with 10% tolerance parameter
        } else if (nominalVoltage <= MEDIUM_VOLTAGE_KV_THRESHOLD) {
            cmax = MEDIUM_VOLTAGE_CMAX;
        } else {
            cmax = HIGH_VOLTAGE_MAX_CMAX;
        }
        return cmax;
    }

    @Override
    public double getCminVoltageFactor(double nominalVoltage) {
        double cmin;
        if (nominalVoltage <= LOW_VOLTAGE_KV_THRESHOLD) {
            cmin =  LOW_VOLTAGE_CMIN;
        } else if (nominalVoltage <= MEDIUM_VOLTAGE_KV_THRESHOLD) {
            cmin = MEDIUM_VOLTAGE_CMIN;
        } else {
            cmin = HIGH_VOLTAGE_MAX_CMIN;
        }
        return cmin;
    }

    @Override
    public double getKtT2W(TwoWindingsTransformer t2w) {
        double cmax = getCmaxVoltageFactor(t2w.getTerminal1().getVoltageLevel().getNominalV());
        double ratedSt2w = t2w.getRatedS();
        double ratedU2 = t2w.getRatedU2(); //TODO : check that the assumption to use ratedU2 is always correct
        double xt2w = t2w.getX();

        return 0.95 * cmax / (1. + 0.6 * xt2w * ratedSt2w / (ratedU2 * ratedU2));
    }

    public double getKtT3Wij(ThreeWindingsTransformer t3w, int iNumLeg, int jNumLeg) {

        ThreeWindingsTransformer.Leg it3wLeg;
        if (iNumLeg == 1) {
            it3wLeg = t3w.getLeg1();
        } else if (iNumLeg == 2) {
            it3wLeg = t3w.getLeg2();
        } else if (iNumLeg == 3) {
            it3wLeg = t3w.getLeg3();
        } else {
            throw new IllegalArgumentException("Three Winding Transformer " + t3w.getId() + " input leg must be 1, 2 or 3 but was : " + iNumLeg);
        }

        ThreeWindingsTransformer.Leg jt3wLeg;
        if (jNumLeg == 1) {
            jt3wLeg = t3w.getLeg1();
        } else if (jNumLeg == 2) {
            jt3wLeg = t3w.getLeg2();
        } else if (jNumLeg == 3) {
            jt3wLeg = t3w.getLeg3();
        } else {
            throw new IllegalArgumentException("Three Winding Transformer " + t3w.getId() + " input leg must be 1, 2 or 3 but was : " + iNumLeg);
        }

        double nominalV = Math.max(it3wLeg.getTerminal().getVoltageLevel().getNominalV(), jt3wLeg.getTerminal().getVoltageLevel().getNominalV());
        double cmax = getCmaxVoltageFactor(nominalV);
        double iratedSt3w = it3wLeg.getRatedS();
        double jratedSt3w = jt3wLeg.getRatedS();
        double ratedU0 = t3w.getRatedU0(); // we suppose that all impedances are based on an impedance base (Sb_leg, U0)

        double iXt3w = it3wLeg.getX();
        double jXt3w = jt3wLeg.getX();

        // We use the following formula:
        // Zab(pu) = Zab / Zbase = Zab * Sbase / Ubase²
        // We suppose that :
        // - Sbase = min(Sa, Sb)   ,
        // - Ubase = U0 because all impedances are expressed in ohms at the U0 voltage level
        // - Zab (ohms at U0) = Za (ohms at U0) + Zb (ohms at U0)
        //
        // Then Zab (pu) = (Za + Zb) * min(Sa,Sb) / U0²
        double yBase = Math.min(iratedSt3w, jratedSt3w) / (ratedU0 * ratedU0);

        return 0.95 * cmax / (1 + 0.6 * (iXt3w + jXt3w) * yBase);

    }

    @Override
    public void setKtT3Wi(ThreeWindingsTransformer t3w) {

        double ktabIec = getKtT3Wij(t3w, 1, 2);
        double ktacIec = getKtT3Wij(t3w, 1, 3);
        double ktbcIec = getKtT3Wij(t3w, 2, 3);

        String t3WId = t3w.getId();

        double ra = t3w.getLeg1().getR();
        double xa = t3w.getLeg1().getX();
        double rb = t3w.getLeg2().getR();
        double xb = t3w.getLeg2().getX();
        double rc = t3w.getLeg3().getR();
        double xc = t3w.getLeg3().getX();

        double raT3k = 0.5 * (ktabIec * (ra + rb) + ktacIec * (ra + rc) - ktbcIec * (rb + rc));
        double xaT3k = 0.5 * (ktabIec * (xa + xb) + ktacIec * (xa + xc) - ktbcIec * (xb + xc));
        double rbT3k = 0.5 * (ktabIec * (ra + rb) - ktacIec * (ra + rc) + ktbcIec * (rb + rc));
        double xbT3k = 0.5 * (ktabIec * (xa + xb) - ktacIec * (xa + xc) + ktbcIec * (xb + xc));
        double rcT3k = 0.5 * (-ktabIec * (ra + rb) + ktacIec * (ra + rc) + ktbcIec * (rb + rc));
        double xcT3k = 0.5 * (-ktabIec * (xa + xb) + ktacIec * (xa + xc) + ktbcIec * (xb + xc));

        double kTaX = getCheckedCoef(t3WId, xaT3k, xa);
        double kTaR = getCheckedCoef(t3WId, raT3k, ra);

        double kTbX = getCheckedCoef(t3WId, xbT3k, xb);
        double kTbR = getCheckedCoef(t3WId, rbT3k, rb);

        double kTcX = getCheckedCoef(t3WId, xcT3k, xc);
        double kTcR = getCheckedCoef(t3WId, rcT3k, rc);

        ThreeWindingsTransformerShortCircuit extension = t3w.getExtension(ThreeWindingsTransformerShortCircuit.class);
        if (extension == null) {
            t3w.newExtension(ThreeWindingsTransformerShortCircuitAdder.class)
                    .add();
            extension = t3w.getExtension(ThreeWindingsTransformerShortCircuit.class);
        }
        extension.setKt1R(kTaR);
        extension.setKt1X(kTaX);
        extension.setKt2R(kTbR);
        extension.setKt2X(kTbX);
        extension.setKt3R(kTcR);
        extension.setKt3X(kTcX);

        // dealing homopolar part
        double ra0 = extension.getLeg1Ro(); // TODO : remove attributes to avoid ambiguity with coefRo and coefXo
        double xa0 = extension.getLeg1Xo();
        double rb0 = extension.getLeg2Ro();
        double xb0 = extension.getLeg2Xo();
        double rc0 = extension.getLeg3Ro();
        double xc0 = extension.getLeg3Xo();

        double ra0T3k = 0.5 * (ktabIec * (ra0 + rb0) + ktacIec * (ra0 + rc0) - ktbcIec * (rb0 + rc0));
        double xa0T3k = 0.5 * (ktabIec * (xa0 + xb0) + ktacIec * (xa0 + xc0) - ktbcIec * (xb0 + xc0));
        double rb0T3k = 0.5 * (ktabIec * (ra0 + rb0) - ktacIec * (ra0 + rc0) + ktbcIec * (rb0 + rc0));
        double xb0T3k = 0.5 * (ktabIec * (xa0 + xb0) - ktacIec * (xa0 + xc0) + ktbcIec * (xb0 + xc0));
        double rc0T3k = 0.5 * (-ktabIec * (ra0 + rb0) + ktacIec * (ra0 + rc0) + ktbcIec * (rb0 + rc0));
        double xc0T3k = 0.5 * (-ktabIec * (xa0 + xb0) + ktacIec * (xa0 + xc0) + ktbcIec * (xb0 + xc0));

        double coefaX0 = getCheckedCoef(t3WId, xa0, xa);
        double coefaR0 = getCheckedCoef(t3WId, ra0, ra);
        double coefbX0 = getCheckedCoef(t3WId, xb0, xb);
        double coefbR0 = getCheckedCoef(t3WId, rb0, rb);
        double coefcX0 = getCheckedCoef(t3WId, xc0, xc);
        double coefcR0 = getCheckedCoef(t3WId, rc0, rc);

        //
        double kTaR0 = getCheckedCoef(t3WId, ra0T3k, ra0);
        double kTaX0 = getCheckedCoef(t3WId, xa0T3k, xa0);
        double kTbR0 = getCheckedCoef(t3WId, rb0T3k, rb0);
        double kTbX0 = getCheckedCoef(t3WId, xb0T3k, xb0);
        double kTcR0 = getCheckedCoef(t3WId, rc0T3k, rc0);
        double kTcX0 = getCheckedCoef(t3WId, xc0T3k, xc0);

        extension.setKt1Xo(kTaX0);
        extension.setKt1Ro(kTaR0);
        extension.setKt2Xo(kTbX0);
        extension.setKt2Ro(kTbR0);
        extension.setKt3Xo(kTcX0);
        extension.setKt3Ro(kTcR0);

        extension.setLeg1CoeffRo(coefaR0);
        extension.setLeg1CoeffXo(coefaX0);
        extension.setLeg2CoeffRo(coefbR0);
        extension.setLeg2CoeffXo(coefbX0);
        extension.setLeg3CoeffRo(coefcR0);
        extension.setLeg3CoeffXo(coefcX0);

    }

    public double getKs(TwoWindingsTransformer t2w, Generator gen) {

        double unq = Math.max(t2w.getTerminal1().getVoltageLevel().getNominalV(), t2w.getTerminal2().getVoltageLevel().getNominalV());
        double ratedU1 = t2w.getRatedU1();
        double ratedU2 = t2w.getRatedU2();
        double ratedUthv = Math.max(ratedU1, ratedU2);
        double ratedUlv = Math.min(ratedU1, ratedU2);

        double ratedSgen = gen.getRatedS();
        double ratedUgen = gen.getTerminal().getVoltageLevel().getNominalV(); // default value
        double cosPhiGen = 0.85;

        double cmax = getCmaxVoltageFactor(unq);

        double subTransXd = 0.;
        GeneratorShortCircuit extension = gen.getExtension(GeneratorShortCircuit.class);
        if (extension != null) {
            subTransXd = extension.getDirectSubtransX();
            //transXd = extension.getDirectTransX();
        }

        GeneratorShortCircuit2 extensions2 = gen.getExtension(GeneratorShortCircuit2.class);
        double voltageRegulationRange = 0.;
        if (extensions2 != null) {
            ratedUgen = extensions2.getRatedU();
            cosPhiGen = extensions2.getCosPhi();
            voltageRegulationRange = extensions2.getVoltageRegulationRange();
        }

        double yBaseGen = ratedSgen / (ratedUgen * ratedUgen);

        double subTransXdpu = subTransXd * yBaseGen;

        double ratedSt2w = t2w.getRatedS();
        double yBaseTfo = ratedSt2w / (ratedU2 * ratedU2);
        double xt2wpu = t2w.getX() * yBaseTfo;

        double ks = 1.0;
        RatioTapChanger ratioTapChanger = t2w.getRatioTapChanger();

        if (ratioTapChanger == null) {
            ks = unq * ratedUlv / ratedUgen / ratedUthv / (1 + voltageRegulationRange / 100.) * cmax / (1. + subTransXdpu * Math.sqrt(1. - cosPhiGen * cosPhiGen));

        } else {
            ks = Math.pow(unq * ratedUlv / ratedUgen / ratedUthv, 2.) * cmax / (1. + Math.abs(subTransXdpu - xt2wpu) * Math.sqrt(1. - cosPhiGen * cosPhiGen));

        }

        return ks;
    }

    public Generator getAssociatedGenerator(Network network, TwoWindingsTransformer t2w) {

        TwoWindingsTransformerShortCircuit extension = t2w.getExtension(TwoWindingsTransformerShortCircuit.class);
        Generator tfoGenerator = null;
        boolean isGen = false;

        if (extension != null) {
            isGen = extension.isPartOfGeneratingUnit();
        }

        if (isGen) {
            // search for the associated generating unit
            double ur1 = t2w.getRatedU1();
            double ur2 = t2w.getRatedU2();
            // we suppose that the generating unit must be connected to the lowest rated voltage terminal
            Terminal terminal;
            if (ur1 < ur2) {
                terminal = t2w.getTerminal1();
            } else {
                terminal = t2w.getTerminal2();
                // TODO : handle case where ur1 = ur2
            }

            Bus busTfo = terminal.getBusBreakerView().getBus(); // TODO : handle not only bus breaker view
            for (Generator generator : network.getGenerators()) {
                Terminal termGen = generator.getTerminal();
                Bus busGen = termGen.getBusBreakerView().getBus(); // TODO : handle not only bus breaker view
                if (busGen == busTfo) {
                    tfoGenerator = generator;
                    break;
                }
            }

        }
        return  tfoGenerator;
    }

    @Override
    public String getNormType() {
        return "IEC";
    }

    @Override
    public double getKg(Generator gen) {
        double nominalU = gen.getTerminal().getVoltageLevel().getNominalV();
        double ratedS = gen.getRatedS();
        double cmax = getCmaxVoltageFactor(nominalU);

        double cosPhi = 0.85; //default value
        double ratedU = nominalU; // default value is nominal voltage
        double subTransXd = 0.;
        //double transXd = 0.;

        GeneratorShortCircuit extension = gen.getExtension(GeneratorShortCircuit.class);
        if (extension != null) {
            subTransXd = extension.getDirectSubtransX();
            //transXd = extension.getDirectTransX();
        } else {
            return 1.0;
        }

        GeneratorShortCircuit2 extensions2 = gen.getExtension(GeneratorShortCircuit2.class);
        if (extensions2 != null) {
            ratedU = extensions2.getRatedU();
            cosPhi = extensions2.getCosPhi();
        } else {
            return 1.0;
        }

        double zBase = ratedU * ratedU / ratedS;
        if (zBase == 0.) {
            return 1.0;
        }
        double subTransXdpu = subTransXd / zBase;

        double kg = nominalU / ratedU * cmax / (1. + subTransXdpu * Math.sqrt(1. - cosPhi * cosPhi));

        return kg;
    }

    @Override
    public void adjustLoadfromInfo(Load load) {
        LoadShortCircuit extension = load.getExtension(LoadShortCircuit.class);
        if (extension == null) {
            throw new PowsyblException("Load '" + load.getId() + "' could generate Z for short circuit because of missing extension input data");
        }

        double ratedMechanicalPower = extension.getRatedMechanicalP();
        double ratedPowerFactor = extension.getRatedPowerFactor(); // cosPhi
        double ratedS = extension.getRatedS();
        double ratedU = extension.getRatedU();
        double efficiency = extension.getEfficiency() / 100.; // conversion from percentages
        double iaIrRatio = extension.getIaIrRatio();
        double rxLockedRotorRatio = extension.getRxLockedRotorRatio();
        int polePairNumber = extension.getPolePairNumber();

        // Zn = 1/(Ilr/Irm) * Urm / (sqrt3 * Irm) = 1/(Ilr/Irm) * Urm² / (Prm / (efficiency * cosPhi))
        // Xn = Zn / sqrt(1+ (Rm/Xm)²)
        double zn = 1. / iaIrRatio * ratedU * ratedU / (ratedMechanicalPower / (efficiency * ratedPowerFactor));
        double xn = zn / Math.sqrt(rxLockedRotorRatio * rxLockedRotorRatio + 1.);
        double rn = xn * rxLockedRotorRatio;

        // zn is transformed into a load that will give the equivalent zn in the admittance matrix
        // using formula P(MW) = Re(Z) * |V|² / |Z|² and Q(MVA) = Im(Z) * |V|² / |Z|²
        // TODO: once load will not be aggregated in the lfNetwork,
        //  the info regarding the load with Asynchronous machine info should remain carried as Zn to fill the admittance matrix
        double uNom = load.getTerminal().getVoltageLevel().getNominalV();
        double pEqScLoad = rn * uNom * uNom / (zn * zn);
        double qEqScLoad = xn * uNom * uNom / (zn * zn);

        load.setQ0(qEqScLoad);
        load.setP0(pEqScLoad);
    }

    @Override
    public void applyNormToT2W(Network network) {
        // Work on two windings transformers
        generatorsWithTfo = new ArrayList<>();
        for (TwoWindingsTransformer t2w : network.getTwoWindingsTransformers()) {
            Generator genTfo = getAssociatedGenerator(network, t2w);
            TwoWindingsTransformerShortCircuit extension = t2w.getExtension(TwoWindingsTransformerShortCircuit.class);
            double kNorm;
            if (genTfo != null) {
                generatorsWithTfo.add(genTfo);
                kNorm = getKs(t2w, genTfo);
                setGenKg(genTfo, kNorm);
            } else {
                kNorm = getKtT2W(t2w);
            }

            if (extension != null) { // grounding only exist if extension exists
                extension.setkNorm(kNorm);
            } else {
                t2w.newExtension(TwoWindingsTransformerShortCircuitAdder.class)
                        .withKnorm(kNorm)
                        .add();
            }
        }
    }

    @Override
    public void applyNormToGenerators(Network network) {
        // Work on generators
        for (Generator gen : network.getGenerators()) {
            if (generatorsWithTfo.contains(gen)) {
                continue; //those generators have already been adjusted with the associated two windings transformer
            }

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

}
