package com.powsybl.incubator.simulator.util.extensions;

import java.util.Objects;

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
