/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.math.matrix.DenseMatrix;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public abstract class AbstractShortCircuitCalculator {

    DenseMatrix mIo;
    DenseMatrix mId;
    DenseMatrix mIi;

    DenseMatrix mIk; //contains the shortcircuit values

    double rdf;
    double xdf;

    double xof;
    double rof;

    double rg;
    double xg;

    double initVx;
    double initVy;

    protected AbstractShortCircuitCalculator(double rdf, double xdf, double rof, double xof, double rg, double xg,
                                             double initVx, double initVy) {
        this.rdf = rdf;
        this.xdf = xdf;
        this.rof = rof;
        this.xof = xof;
        this.rg = rg;
        this.xg = xg;
        this.initVx = initVx;
        this.initVy = initVy;

    }

    DenseMatrix getmIo() {
        return mIo;
    }

    DenseMatrix getmId() {
        return mId;
    }

    DenseMatrix getmIi() {
        return mIi;
    }

    public abstract void computeCurrents();

    public enum BlocType {
        A,
        A2,
        I_D,
        J
    }

    public static DenseMatrix getMatrixByType(BlocType bt, double scalar) {
        DenseMatrix m = new DenseMatrix(2, 2);
        addMatrixBlocByType(m, bt, scalar);
        return m;
    }

    public static void addMatrixBlocByType(DenseMatrix m, BlocType bt, double scalar) {
        if (bt == BlocType.A) {
            addMatrixBloc(m, -1. / 2. * scalar, -Math.sqrt(3.) / 2. * scalar, Math.sqrt(3.) / 2. * scalar, -1. / 2. * scalar);
        } else if (bt == BlocType.A2) {
            addMatrixBloc(m, 1. / 2. * scalar, -Math.sqrt(3.) / 2. * scalar, Math.sqrt(3.) / 2. * scalar, 1. / 2. * scalar);
        } else if (bt == BlocType.I_D) {
            addMatrixBloc(m, scalar, 0., 0., scalar);
        } else if (bt == BlocType.J) {
            addMatrixBloc(m, 0., -scalar, scalar, 0.);
        } else {
            throw new IllegalArgumentException("Bloc type unknown ");
        }
    }

    public static void addMatrixBloc(DenseMatrix m, double m11, double m12, double m21, double m22) {
        m.add(0, 0, m11);
        m.add(0, 1, m12);
        m.add(1, 0, m21);
        m.add(1, 1, m22);
    }

    public static DenseMatrix addMatrices22(DenseMatrix m1, DenseMatrix m2) {
        DenseMatrix m = new DenseMatrix(2, 2);

        m.add(0, 0, m1.get(0, 0) + m2.get(0, 0));
        m.add(0, 1, m1.get(0, 1) + m2.get(0, 1));
        m.add(1, 0, m1.get(0, 1) + m2.get(1, 0));
        m.add(1, 1, m1.get(0, 1) + m2.get(1, 1));

        return m.toDense();
    }

    public static DenseMatrix getInvZt(double r, double x) {
        double detZ = r * r + x * x;
        DenseMatrix invZ = new DenseMatrix(2, 2);
        invZ.add(0, 0, r / detZ);
        invZ.add(0, 1, x / detZ);
        invZ.add(1, 0, -x / detZ);
        invZ.add(1, 1, r / detZ);

        return invZ;
    }

    public static DenseMatrix getZ(double r, double x) {
        DenseMatrix z =  new DenseMatrix(2, 2);
        z.add(0, 0, r);
        z.add(0, 1, -x);
        z.add(1, 0, x);
        z.add(1, 1, r);

        return z;
    }

}
