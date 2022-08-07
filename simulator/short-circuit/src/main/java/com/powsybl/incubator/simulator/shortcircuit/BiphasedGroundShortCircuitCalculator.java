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
public class BiphasedGroundShortCircuitCalculator extends AbstractShortCircuitCalculator {

    public BiphasedGroundShortCircuitCalculator(double rdf, double xdf, double rof, double xof, double rg, double xg,
                                            double initVx, double initVy) {
        super(rdf, xdf, rof, xof, rg, xg, initVx, initVy);

    }

    public void computeCurrents() {
        //Description of the fault:
        // a ---------------x------------------  by definition : Ia = 0
        // b ---------------+------------------                  Vc = Vb = 0
        //                  |
        //                  |
        // c ---------------+------------------
        //                  |
        //                  |
        //                /////
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
        // [ Io ]         [ 1  1  1 ]   [ 0  ]
        // [ Id ] = 1/3 * [ 1  a  a²] * [ Ib ]   => Io + Id + Ii = 0
        // [ Ii ]         [ 1  a² a ]   [ Ic ]
        //
        // Vo = Vd = Vi = 1/3 * Va
        //
        // Step 2:
        // Using the previous expressions we get : Id, Ii, Io and Va

        // Zof and Zdf are complex impedance matrix elements :
        // Zof = tM * inv(Yo) * M   and Zdf = tM * inv(Yd) * M
        //
        // Step 3: compute the short circuit voltages:
        // From computed Ic1 we get complex values : I1o, I1d, I1i, I2o, I2d, I2i using step 1 formulas expressed with Ic1
        // Then compute the voltages from current values

        DenseMatrix zof = getZ(rof, xof);
        DenseMatrix zdf = getZ(rdf, xdf);

        //         (zof + zdf) * [Vinit]
        // Id = ---------------------------
        //         Zdf * (Zdf + 2 * Zof)
        //
        //          - zof * [Vinit]
        // Ii = ---------------------------
        //         Zdf * (Zdf + 2 * Zof)
        //
        //          - zdf * [Vinit]
        // Io = ---------------------------
        //         Zdf * (Zdf + 2 * Zof)
        //
        //       - 3 * zof * [Vinit]
        // Va = ---------------------
        //          Zdf + 2 * Zof

        DenseMatrix vdInit = new DenseMatrix(2, 1);
        vdInit.add(0, 0, initVx);
        vdInit.add(1, 0, initVy);

        DenseMatrix twoId = getMatrixByType(BlocType.Id, 2.);
        DenseMatrix minusId = getMatrixByType(BlocType.Id, -1.);
        DenseMatrix threeId = getMatrixByType(BlocType.Id, 3.);

        DenseMatrix twoZof = twoId.times(zof).toDense();
        DenseMatrix zdf2Zof = addMatrices22(zdf.toDense(), twoZof.toDense());
        DenseMatrix zdfZof = addMatrices22(zdf.toDense(), zof.toDense());
        DenseMatrix minusZof = zof.times(minusId).toDense();
        DenseMatrix minusZdf = zdf.times(minusId).toDense();

        DenseMatrix numId = zdfZof.times(vdInit).toDense();
        DenseMatrix numIo = minusZdf.times(vdInit).toDense();
        DenseMatrix numIi = minusZof.times(vdInit).toDense();

        DenseMatrix demonI = zdf.times(zdf2Zof).toDense();
        DenseMatrix invDemonI = getInvZt(demonI.get(0, 0), demonI.get(1, 0));

        mId = invDemonI.times(numId).toDense();
        mIo = invDemonI.times(numIo).toDense();
        mIi = invDemonI.times(numIi).toDense();
    }
}
