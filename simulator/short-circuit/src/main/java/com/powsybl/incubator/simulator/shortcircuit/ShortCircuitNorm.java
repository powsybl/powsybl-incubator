/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public interface ShortCircuitNorm {

    double getCmaxVoltageFactor(double nominalVoltage);

    double getCminVoltageFactor(double nominalVoltage);

    double getKtT2W(TwoWindingsTransformer t2w);

    double getKg(Generator gen);

    String getNormType();

    void setKtT3Wi(ThreeWindingsTransformer t3w);

    void setGenKg(Generator gen, double kg);

    void applyNormToNetwork(Network network);

    Network getNetwork();

    public class T3wCoefs {
        public final double ktr1;
        public final double ktx1;
        public final double ktr2;
        public final double ktx2;
        public final double ktr3;
        public final double ktx3;
        public final double ktro1;
        public final double ktxo1;
        public final double ktro2;
        public final double ktxo2;
        public final double ktro3;
        public final double ktxo3;
        public final double coefro1;
        public final double coefxo1;
        public final double coefro2;
        public final double coefxo2;
        public final double coefro3;
        public final double coefxo3;
        public final boolean updateCoefo;

        T3wCoefs(double ktr1, double ktx1, double ktr2, double ktx2, double ktr3, double ktx3,
                 double ktro1, double ktxo1, double ktro2, double ktxo2, double ktro3, double ktxo3,
                 double coefro1, double coefxo1, double coefro2, double coefxo2, double coefro3, double coefxo3) {
            this.ktr1 = ktr1;
            this.ktr2 = ktr2;
            this.ktr3 = ktr3;
            this.ktx1 = ktx1;
            this.ktx2 = ktx2;
            this.ktx3 = ktx3;
            this.ktro1 = ktro1;
            this.ktro2 = ktro2;
            this.ktro3 = ktro3;
            this.ktxo1 = ktxo1;
            this.ktxo2 = ktxo2;
            this.ktxo3 = ktxo3;
            this.coefro1 = coefro1;
            this.coefro2 = coefro2;
            this.coefro3 = coefro3;
            this.coefxo1 = coefxo1;
            this.coefxo2 = coefxo2;
            this.coefxo3 = coefxo3;
            this.updateCoefo = true;
        }

        T3wCoefs(double ktr1, double ktx1, double ktr2, double ktx2, double ktr3, double ktx3,
                 double ktro1, double ktxo1, double ktro2, double ktxo2, double ktro3, double ktxo3) {
            this.ktr1 = ktr1;
            this.ktr2 = ktr2;
            this.ktr3 = ktr3;
            this.ktx1 = ktx1;
            this.ktx2 = ktx2;
            this.ktx3 = ktx3;
            this.ktro1 = ktro1;
            this.ktro2 = ktro2;
            this.ktro3 = ktro3;
            this.ktxo1 = ktxo1;
            this.ktxo2 = ktxo2;
            this.ktxo3 = ktxo3;
            this.coefro1 = 0.;
            this.coefro2 = 0.;
            this.coefro3 = 0.;
            this.coefxo1 = 0.;
            this.coefxo2 = 0.;
            this.coefxo3 = 0.;
            this.updateCoefo = false;
        }

    }

}
