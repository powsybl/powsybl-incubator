/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ScTransfo3wKt {

    public static class Leg {

        private final double kTr;
        private final double kTx;

        private final double kTro;
        private final double kTxo;

        public Leg(double kTr, double kTx, double kTro, double kTxo) {
            this.kTr = kTr;
            this.kTx = kTx;
            this.kTro = kTro;
            this.kTxo = kTxo;
        }

        public double getkTr() {
            return kTr;
        }

        public double getkTx() {
            return kTx;
        }

        public double getkTro() {
            return kTro;
        }

        public double getkTxo() {
            return kTxo;
        }
    }

    private final ScTransfo3wKt.Leg leg1;
    private final ScTransfo3wKt.Leg leg2;
    private final ScTransfo3wKt.Leg leg3;

    ScTransfo3wKt(ScTransfo3wKt.Leg leg1, ScTransfo3wKt.Leg leg2, ScTransfo3wKt.Leg leg3) {
        this.leg1 = Objects.requireNonNull(leg1);
        this.leg2 = Objects.requireNonNull(leg2);
        this.leg3 = Objects.requireNonNull(leg3);
    }

    public ScTransfo3wKt.Leg getLeg1() {
        return leg1;
    }

    public ScTransfo3wKt.Leg getLeg2() {
        return leg2;
    }

    public ScTransfo3wKt.Leg getLeg3() {
        return leg3;
    }
}
