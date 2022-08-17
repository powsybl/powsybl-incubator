/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.extensions.GeneratorShortCircuit;
import com.powsybl.incubator.simulator.util.extensions.iidm.GeneratorShortCircuit2;
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

        return result;
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

        /*System.out.println(" ==========>>>kG IEC : nominalU = " + nominalU + " ratedU = " + ratedU + " cmax = "
                + cmax + " subTransXdpu = " + subTransXdpu + " cosPhi = " + cosPhi);*/

        double kg = nominalU / ratedU * cmax / (1. + subTransXdpu * Math.sqrt(1. - cosPhi * cosPhi));

        return kg;
    }
}
