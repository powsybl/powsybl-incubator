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
public class BiphasedShortCircuitCalculator extends AbstractShortCircuitCalculator  {

    public BiphasedShortCircuitCalculator(double rdf, double xdf, double rof, double xof, double rg, double xg,
                                            double initVx, double initVy) {
        super(rdf, xdf, rof, xof, rg, xg, initVx, initVy);

    }

    public void computeCurrents() {
        //Description of the fault:
        // a ---------------x------------------  by definition : Ia = 0
        // b ---------------+------------------                  Ib = -Ic
        //                  |
        //                 Zf
        //                  |
        // c ---------------+------------------                  Vb = Zf * Ib + Vc
        //
        //Problem to solve:
        // Given the equalities above, we need to solve for V and I the following problem:
        // [ Vof ] = -tM * inv(Yo) * M * [ Iof ]
        // [ Vdf ] = -tM * inv(Yd) * M * [ Idf ] + tM * [ V(init) ]
        // [ Vif ] = -tM * inv(Yd) * M * [ Iif ]
        // Where:
        // - Yo and Yd are the full network admittance matrices (zero and direct fortescue components)
        // - M is the extraction matrix to get the subvectors V1, I1 of the full network vectors [V] and [I]
        //
        // Step 1: express the currents in fortescue basis :
        //
        // [ Io ]         [ 1  1  1 ]   [ 0  ]              [  0   ]
        // [ Id ] = 1/3 * [ 1  a  a²] * [ Ib ] = 1/3 * Ib * [a - a²]
        // [ Ii ]         [ 1  a² a ]   [-Ib ]              [a²- a ]
        //
        // Step 2: get Ib :
        // Given:  Vb = Vo + a² * Vd + a * Vi  and Vc = Vo + a * Vd + a² * Vi
        // replacing them in Vb = Zf * Ib + Vc we get :
        //
        //               tM * [Vinit]                  j * sqrt(3) * tM * [Vinit]
        // Ib = ----------------------------------- = -----------------------------
        //       (a-a²)/3 * (Zif + Zdf) + Zf/(a²-a)         Zdf + Zif +Zf
        //
        // Where Zof and Zdf are complex impedance matrix elements :
        // Zof = tM * inv(Yo) * M   and Zdf = tM * inv(Yd) * M
        //
        // Step 3: compute the short circuit voltages:
        // From computed Ic1 we get complex values : I1o, I1d, I1i, I2o, I2d, I2i using step 1 formulas expressed with Ic1
        // Then compute the voltages from current values
        double rt = 2 * rdf + rg;
        double xt = 2 * xdf + xg;

        // [Zt] = [ rt  -xt ]
        //        [ xt   rt ]
        //
        // Cartesian expression of Ic using matrices :
        //                                               -sqrt(3)
        // [ibx] = - inv([Zt]) * [j] *sqrt(3) * [vdx] = ------------ * [ rt xt ] * [ 0 -1 ] * [vdx]
        // [iby]                                [vdy]   (rt² + xt²)    [-xt rt ]   [ 1  0 ]   [vdy]

        DenseMatrix vdInit = new DenseMatrix(2, 1);
        vdInit.add(0, 0, initVx);
        vdInit.add(1, 0, initVy);

        DenseMatrix jmSqrt3 = getMatrixByType(BlocType.J, -Math.sqrt(3));
        DenseMatrix invZt = getInvZt(rt, xt);

        DenseMatrix tmpjVd = jmSqrt3.times(vdInit).toDense();
        DenseMatrix mIb = invZt.times(tmpjVd).toDense();

        // Compute the currents :
        // [ Io ]         [ 1  1  1 ]   [ 0  ]              [  0   ]
        // [ Id ] = 1/3 * [ 1  a  a²] * [ Ib ] = 1/3 * Ib * [a - a²]
        // [ Ii ]         [ 1  a² a ]   [-Ib ]              [a²- a ]

        // [Io] = 0
        DenseMatrix adiv3 = getMatrixByType(BlocType.A, 1. / 3.);
        DenseMatrix ma2div3 = getMatrixByType(BlocType.A2, -1. / 3);
        DenseMatrix minusId = getMatrixByType(BlocType.Id, -1.);
        DenseMatrix aa2div3 = addMatrices22(adiv3.toDense(), ma2div3.toDense());

        mId = aa2div3.times(mIb).toDense();
        mIi = minusId.times(mId).toDense();
        mIo = new DenseMatrix(2, 1);
    }
}
