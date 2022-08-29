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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNormIec implements ShortCircuitNorm {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShortCircuitNormIec.class);

    public static final double LOW_VOLTAGE_KV_THRESHOLD = 1.;
    public static final double MEDIUM_VOLTAGE_KV_THRESHOLD = 35.;

    public static final double LOW_VOLTAGE_6PERCENT_CMAX = 1.05;
    public static final double LOW_VOLTAGE_10PERCENT_CMAX = 1.10;
    public static final double MEDIUM_VOLTAGE_CMAX = 1.10;
    public static final double HIGH_VOLTAGE_MAX_CMAX = 1.10;

    public static final double LOW_VOLTAGE_CMIN = 0.95;
    public static final double MEDIUM_VOLTAGE_CMIN = 1.0;
    public static final double HIGH_VOLTAGE_MAX_CMIN = 1.0;

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

    @Override
    public double getKtT3W(ThreeWindingsTransformer t3w, int numLeg) {

        ThreeWindingsTransformer.Leg t3wLeg;
        if (numLeg == 1) {
            t3wLeg = t3w.getLeg1();
        } else if (numLeg == 2) {
            t3wLeg = t3w.getLeg2();
        } else if (numLeg == 3) {
            t3wLeg = t3w.getLeg3();
        } else {
            throw new IllegalArgumentException("Three Winding Transformer " + t3w.getId() + " input leg must be 1, 2 or 3 but was : " + numLeg);
        }

        double cmax = getCmaxVoltageFactor(t3wLeg.getTerminal().getVoltageLevel().getNominalV());
        double ratedSt3w = t3wLeg.getRatedS();
        double ratedU0 = t3w.getRatedU0(); // we suppose that all impedances are based on an impedance base (Sb_leg, U0)
        double xt3w = t3wLeg.getX();

        return 0.95 * cmax / (1 + 0.6 * xt3w * ratedSt3w / (ratedU0 * ratedU0));
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

    public List<Double> getKtT3Wi(ThreeWindingsTransformer t3w) {

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

        List<Double> result = new ArrayList<>();

        result.add(kTaR);
        result.add(kTaX);
        result.add(kTbR);
        result.add(kTbX);
        result.add(kTcR);
        result.add(kTcX);

        // dealing homopolar part
        ThreeWindingsTransformerShortCircuit extension = t3w.getExtension(ThreeWindingsTransformerShortCircuit.class);
        if (extension != null) {
            double ra0 = extension.getLeg1Ro();
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

            double coefaX0 = getCheckedCoef(t3WId, xa0T3k, xaT3k);
            double coefaR0 = getCheckedCoef(t3WId, ra0T3k, raT3k);

            double coefbX0 = getCheckedCoef(t3WId, xb0T3k, xbT3k);
            double coefbR0 = getCheckedCoef(t3WId, rb0T3k, rbT3k);

            double coefbX0bis = xb0T3k / xbT3k;

            double coefcX0 = getCheckedCoef(t3WId, xc0T3k, xcT3k);
            double coefcR0 = getCheckedCoef(t3WId, rc0T3k, rcT3k);

            result.add(coefaR0);
            result.add(coefaX0);
            result.add(coefbR0);
            result.add(coefbX0);
            result.add(coefcR0);
            result.add(coefcX0);

        } else {
            result.add(0.);
            result.add(0.);
            result.add(0.);
            result.add(0.);
            result.add(0.);
            result.add(0.);
        }

        return result;
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

    public void adjustWithKt3W(ThreeWindingsTransformer t3w, List<Double> result) {
        ThreeWindingsTransformer.Leg leg1 = t3w.getLeg1();
        ThreeWindingsTransformer.Leg leg2 = t3w.getLeg2();
        ThreeWindingsTransformer.Leg leg3 = t3w.getLeg3();

        leg1.setR(leg1.getR() * result.get(0));
        leg1.setX(leg1.getX() * result.get(1));
        leg2.setR(leg2.getR() * result.get(2));
        leg2.setX(leg2.getX() * result.get(3));
        leg3.setR(leg3.getR() * result.get(4));
        leg3.setX(leg3.getX() * result.get(5));

        ThreeWindingsTransformerShortCircuit extension = t3w.getExtension(ThreeWindingsTransformerShortCircuit.class);
        if (extension != null) {
            // these values already contain the Kt coeff
            extension.setLeg1CoeffRo(result.get(6));
            extension.setLeg1CoeffXo(result.get(7));
            extension.setLeg2CoeffRo(result.get(8));
            extension.setLeg2CoeffXo(result.get(9));
            extension.setLeg3CoeffRo(result.get(10));
            extension.setLeg3CoeffXo(result.get(11));
        }

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

    public void applyNormToNetwork(Network network) {

        // FIXME: the application of the norm modifies the iidm network characteristics. Extensions carried from iidm network to lfNetwork should help to avoid this.

        // Work on two windings transformers
        List<Generator> generatorsWithTfo = new ArrayList<>();
        for (TwoWindingsTransformer t2w : network.getTwoWindingsTransformers()) {
            Generator genTfo = getAssociatedGenerator(network, t2w);
            if (genTfo != null) {
                generatorsWithTfo.add(genTfo);
                double ks = getKs(t2w, genTfo);
                adjustWithKg(genTfo, ks);
                t2w.setX(t2w.getX() * ks);
                t2w.setR(t2w.getR() * ks);
            } else {
                double kt = getKtT2W(t2w);
                t2w.setX(t2w.getX() * kt);
                t2w.setR(t2w.getR() * kt);
            }
            // handling grounding in addition to X and R but without Kt or Ks
            TwoWindingsTransformerShortCircuit extension = t2w.getExtension(TwoWindingsTransformerShortCircuit.class);
            if (extension != null) { // grounding only exist if extension exists
                double x = t2w.getX();
                double r = t2w.getR();
                double ro = extension.getCoeffRo() * r;
                double xo = extension.getCoeffXo() * x;
                double roTotal = ro + extension.getR1Ground() + extension.getR2Ground(); // if not grounded R1 and R2 to ground must be 0.
                double xoTotal = xo + extension.getX1Ground() + extension.getX2Ground(); // TODO : try to implement grounding independently from the norm
                if (Math.abs(r) > 0.00001) {
                    extension.setCoeffRo(roTotal / r);
                }
                if (Math.abs(x) > 0.00001) {
                    extension.setCoeffXo(xoTotal / x);
                }
            }
        }

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
                    adjustWithKg(gen, kg);
                }
            }
        }

        // Work on loads
        for (Load load : network.getLoads()) {
            LoadShortCircuit extension = load.getExtension(LoadShortCircuit.class);
            if (extension == null) {
                continue; // we do not modify loads with no additional short circuit info
            }
            adjustLoadfromInfo(load);
        }

        // Work on three Windings transformers
        for (ThreeWindingsTransformer t3w : network.getThreeWindingsTransformers()) {
            List<Double> resultT3 = getKtT3Wi(t3w); // table contains vector [ kTaR; kTaX; kTbR; kTbX; kTcR; kTcX ]
            adjustWithKt3W(t3w, resultT3);
        }
    }

    public void adjustWithKg(Generator gen, double kg) {
        // This is a temporary function to multiply with kg
        // should disappear when the norm will be properly applied
        GeneratorShortCircuit extension = gen.getExtension(GeneratorShortCircuit.class);
        if (extension != null) {
            extension.setDirectSubtransX(extension.getDirectSubtransX() * kg);
            extension.setDirectTransX(extension.getDirectTransX() * kg);
        }
        GeneratorShortCircuit2 extensions2 = gen.getExtension(GeneratorShortCircuit2.class);
        if (extensions2 != null) {
            extensions2.setSubTransRd(extensions2.getSubTransRd() * kg);
            extensions2.setTransRd(extensions2.getTransRd() * kg);
        }
    }
}
