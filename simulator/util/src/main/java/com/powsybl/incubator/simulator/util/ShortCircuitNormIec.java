/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitNormIec extends ShortCircuitNorm {

    public static final double LOW_VOLTAGE_KV_THRESHOLD = 1.;
    public static final double MEDIUM_VOLTAGE_KV_THRESHOLD = 35.;

    public static final double LOW_VOLTAGE_6PERCENT_CMAX = 1.05;
    public static final double LOW_VOLTAGE_10PERCENT_CMAX = 1.10;
    public static final double MEDIUM_VOLTAGE_CMAX = 1.10;
    public static final double HIGH_VOLTAGE_MAX_CMAX = 1.10;

    public static final double LOW_VOLTAGE_CMIN = 0.95;
    public static final double MEDIUM_VOLTAGE_CMIN = 1.0;
    public static final double HIGH_VOLTAGE_MAX_CMIN = 1.0;

    public ShortCircuitNormIec() {
        super();
    }

    @Override
    public double getCmaxVoltageFactor(double nominalVoltage) {
        double cmax = 1.0;
        if (nominalVoltage <= LOW_VOLTAGE_KV_THRESHOLD) {
            cmax =  LOW_VOLTAGE_6PERCENT_CMAX; //TODO : make the distinction with 10% tolerance parameter
        } else if (nominalVoltage <= MEDIUM_VOLTAGE_KV_THRESHOLD) {
            cmax = MEDIUM_VOLTAGE_CMAX;
        } else {
            cmax = HIGH_VOLTAGE_MAX_CMAX;
        }
        return cmax;
    }

    @Override
    public double getCminVoltageFactor(double nominalVoltage) {
        double cmin = 1.0;
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

        double kt = 0.95 * cmax / (1 + 0.6 * xt2w * ratedSt2w / (ratedU2 * ratedU2));

        return kt;
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

        double kt = 0.95 * cmax / (1 + 0.6 * xt3w * ratedSt3w / (ratedU0 * ratedU0));

        return kt;
    }

    @Override
    public NormType getNormType() {
        return NormType.IEC;
    }

}
