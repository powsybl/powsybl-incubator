/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.incubator.simulator.util.AdmittanceMatrix;
import com.powsybl.math.matrix.DenseMatrix;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public abstract class AbstractShortCircuitCalculator {

    Matrix mIo;
    Matrix mId;
    Matrix mIi;

    Matrix mIk; //contains the shortcircuit values

    double rdf;
    double xdf;

    double xof;
    double rof;

    double rg;
    double xg;

    double initVx;
    double initVy;

    MatrixFactory mf;

    public AbstractShortCircuitCalculator(double rdf, double xdf, double rof, double xof, double rg, double xg,
                                          double initVx, double initVy, MatrixFactory mf) {
        this.rdf = rdf;
        this.xdf = xdf;
        this.rof = rof;
        this.xof = xof;
        this.rg = rg;
        this.xg = xg;
        this.initVx = initVx;
        this.initVy = initVy;
        this.mf = mf;

    }

    Matrix getmIo() {
        return mIo;
    }

    Matrix getmId() {
        return mId;
    }

    Matrix getmIi() {
        return mIi;
    }

    public abstract void computeCurrents();

    public DenseMatrix getImpedanceMatrix(Matrix tM, AdmittanceMatrix y, Matrix m) {
        DenseMatrix dM = m.toDense();
        y.solveTransposed(dM);
        Matrix z = tM.times(dM);
        DenseMatrix dZ = z.toDense();

        return dZ;
    }

    public enum BlocType {
        A,
        A2,
        Id,
        J
    }

    public static Matrix getMatrixByType(BlocType bt, double scalar, MatrixFactory mf) {
        Matrix m = mf.create(2, 2, 2);
        addMatrixBlocByType(m, bt, scalar);
        return m;
    }

    public static Matrix getMatrixByType(BlocType bt1, double scalar1, BlocType bt2, double scalar2, MatrixFactory mf) {
        Matrix m = mf.create(4, 2, 2);
        addMatrixBlocByType(m, bt1, scalar1);
        addMatrixBlocByType(m, bt2, scalar2);
        return m;
    }

    public static void addMatrixBlocByType(Matrix m, BlocType bt, double scalar) {
        if (bt == BlocType.A) {
            addMatrixBloc(m, 1, -1. / 2. * scalar, -Math.sqrt(3.) / 2. * scalar, Math.sqrt(3.) / 2. * scalar, -1. / 2. * scalar);
        } else if (bt == BlocType.A2) {
            addMatrixBloc(m, 1, 1. / 2. * scalar, -Math.sqrt(3.) / 2. * scalar, Math.sqrt(3.) / 2. * scalar, 1. / 2. * scalar);
        } else if (bt == BlocType.Id) {
            addMatrixBloc(m, 1, scalar, 0., 0., scalar);
        } else if (bt == BlocType.J) {
            addMatrixBloc(m, 1, 0., -scalar, scalar, 0.);
        } else {
            throw new IllegalArgumentException("Bloc type unknown ");
        }
    }

    public static void addMatrixBloc(Matrix m, int numBloc, double m11, double m12, double m21, double m22) {

        if (numBloc != 1 && numBloc != 2) {
            throw new IllegalArgumentException("Bloc number must be either 1 or 2 ");
        }

        int iIndex = 0;
        if (numBloc == 2) {
            iIndex = 2;
        }

        m.add(iIndex, 0, m11);
        m.add(iIndex, 1, m12);
        m.add(iIndex + 1, 0, m21);
        m.add(iIndex + 1, 1, m22);
    }

    public static DenseMatrix addMatrices22(DenseMatrix m1, DenseMatrix m2, MatrixFactory mf) {
        Matrix m = mf.create(2, 2, 2);

        m.add(0, 0, m1.get(0, 0) + m2.get(0, 0));
        m.add(0, 1, m1.get(0, 1) + m2.get(0, 1));
        m.add(1, 0, m1.get(0, 1) + m2.get(1, 0));
        m.add(1, 1, m1.get(0, 1) + m2.get(1, 1));

        return m.toDense();
    }

    public static Matrix getInvZt(double r, double x, MatrixFactory mf) {
        double detZ = r * r + x * x;
        Matrix invZ = mf.create(2, 2, 2);
        invZ.add(0, 0, r / detZ);
        invZ.add(0, 1, x / detZ);
        invZ.add(1, 0, -x / detZ);
        invZ.add(1, 1, r / detZ);

        return invZ;
    }

    public static Matrix getInvZt(Matrix m, MatrixFactory mf) {
        double r = m.toDense().get(0, 0);
        double x = m.toDense().get(1, 0);
        double detZ = r * r + x * x;
        Matrix invZ = mf.create(2, 2, 2);
        invZ.add(0, 0, r / detZ);
        invZ.add(0, 1, x / detZ);
        invZ.add(1, 0, -x / detZ);
        invZ.add(1, 1, r / detZ);

        return invZ;
    }

    public static Matrix getZ(double r, double x, MatrixFactory mf) {
        Matrix z =  mf.create(2, 2, 2);
        z.add(0, 0, r);
        z.add(0, 1, -x);
        z.add(1, 0, x);
        z.add(1, 1, r);

        return z;
    }

}
