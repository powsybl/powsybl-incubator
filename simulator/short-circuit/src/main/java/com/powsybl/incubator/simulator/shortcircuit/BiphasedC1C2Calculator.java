/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.math.matrix.DenseMatrix;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class BiphasedC1C2Calculator extends BiphasedCommonSupportShortCircuitCalculator {

    public BiphasedC1C2Calculator(double rdf, double xdf, double rof, double xof, double rg, double xg,
                                  double initVx, double initVy, MatrixFactory mf,
                                  double v2dxInit, double v2dyInit,
                                  double ro12, double xo12, double ro22, double xo22, double ro21, double xo21,
                                  double rd12, double xd12, double rd22, double xd22, double rd21, double xd21) {
        super(rdf, xdf, rof, xof, rg, xg, initVx, initVy, mf, v2dxInit, v2dyInit, ro12, xo12, ro22, xo22, ro21, xo21, rd12, xd12, rd22, xd22, rd21, xd21);

//Description of the fault (short circuit between c1 and a2) :
        // a1 ---------------x------------------  by definition : Ia1 = Ib1 = Ia2 = Ib2 = 0
        // b1 ---------------x------------------                  Ic1 = -Ic2
        // c1 ---------------+------------------                  Vc1 = Zf * Ic1 + Vc2
        //                   |
        //                  Zf
        //                   |
        // c2 ---------------+------------------
        // a2 ---------------x------------------
        // b2 ---------------x------------------

        //Problem to solve:
        // Given the equalities above, we need to solve for V and I the following problem:
        // [ Vof ] = -tM * inv(Yo) * M * [ Iof ]
        // [ Vdf ] = tM * [ V(init) ] - tM * inv(Yd) * M * [ Idf ]
        // [ Vif ] = -tM * inv(Yd) * M * [ Iif ]
        // Where:
        // - Yo and Yd are the full network admittance matrices (zero and direct fortescue components)
        // - M is the extraction matrix to get the subvectors V1, V2, I1 and I2 of the full network vectors [V] and [I]
        //
        // Step 1: express the currents in fortescue basis :
        //
        // [ I1o ]         [ 1  1  1 ]   [ 0  ]               [ 1 ]
        // [ I1d ] = 1/3 * [ 1  a  a²] * [ 0  ] = 1/3 * Ic1 * [ a²]
        // [ I1i ]         [ 1  a² a ]   [ Ic1]               [ a ]
        //
        // [ I2o ]               [ 1 ]
        // [ I2d ] = -1/3 *Ic1 * [ a²]
        // [ I2i ]               [ a ]
        //
        // Step 2: get Ic1 :
        // Given:  Vc1 = V1o + a * V1d + a² * V1i        and       Vc2 = V2o + a * V2d + a² * V2i
        // and replacing them in:  Vc1 = Zf * Ic1 + Vb2
        // we get
        //                                          a * (V1d(init) - V2d(init))
        // Ic1 = ---------------------------------------------------------------------------------------------------------
        //        Zf + 1/3*(Zd_11 - Zd_12 + Zd_22 - Zd_21 + Zo_11 - Zo_21 + Zo_22 - Zo_12 + Zi_22 - Zi_12 + Zi_11 - Zi_21)
        //
        // Where Zof_ij and Zdf_ij are complex impedance matrix elements at nodes 1 and 2:
        // Zof = tM * inv(Yo) * M   and Zdf = tM * inv(Yd) * M
        //
        // Then, for example, we have with complex variables :
        // [ V1of ]          [ I1of ]     [ Zof_11 Zof_12 ]   [ I1of ]
        // [ V2of ] = -Zof * [ I2of ] = - [ Zof_21 Zof_22 ] * [ I2of ]
        //
        // Step 3: compute the short circuit voltages:
        // From computed Ic1 we get complex values : I1o, I1d, I1i, I2o, I2d, I2i using step 1 formulas expressed with Ic1
        // Then compute the voltages from current values

        computeZt(); // computes rt and xt
        computeIc(); // computes Ic of common support from rt and xt
        computeCurrents(); // computes Io, Id, Ii for both supports from computed Ic
        computeVoltages(); // computes Vo, Vd, Vi from computed currents and computed terms of the impedance matrix

    }

    @Override
    public void computeIc() {

        // Compute Ic1 :
        // Complex expression :
        //                                             a * (V1d(init) - V2d(init))
        // Ic1 = ---------------------------------------------------------------------------------------------------------
        //        Zf + 1/3*(Zd_11 - Zd_12 + Zd_22 - Zd_21 + Zo_11 - Zo_21 + Zo_22 - Zo_12 + Zi_22 - Zi_12 + Zi_11 - Zi_21)
        //
        //
        // The equivalent cartesian matrix expression of Ic :
        //                    1
        // [ic1x] =  ------------------------ * [ rt xt ] *  [ -1/2  -sqrt(3)/2 ] * ( [vd1x] - [vd2x] )
        // [ic1y]         (rt² + xt²)           [-xt rt ]    [ sqrt(3)/2  -1/2  ]   ( [vd1y]   [vd2y] )
        //

        //compute the numerator matrix =  a * (V1d(init) - V2d(init))
        Matrix ma = getMatrixByType(AbstractShortCircuitCalculator.BlocType.A, 1.0, mf);

        Matrix mVdInit = mf.create(2, 1, 2);
        mVdInit.add(0, 0, initVx - v2dxInit);
        mVdInit.add(1, 0, initVy - v2dyInit);

        Matrix numerator = ma.times(mVdInit);

        // get Ic by multiplying the numerator to inv(Zt)
        Matrix invZt = getInvZt(rt, xt, mf);
        mIc = invZt.times(numerator);
    }

    @Override
    public void computeZt() {
        //  Zf + 1/3*(Zd_11 - Zd_12 + Zd_22 - Zd_21 + Zo_11 - Zo_21 + Zo_22 - Zo_12 + Zi_22 - Zi_12 + Zi_11 - Zi_21)
        Matrix mId = getMatrixByType(AbstractShortCircuitCalculator.BlocType.Id, -1.0, mf);
        Matrix idDiv3 = getMatrixByType(AbstractShortCircuitCalculator.BlocType.Id, 1. / 3., mf);

        Matrix td12 = mId.times(zdf12);
        Matrix td21 = mId.times(zdf21);

        Matrix to21 = mId.times(zof21);
        Matrix to12 = mId.times(zof12);

        Matrix ti12 = mId.times(zif12);
        Matrix ti21 = mId.times(zif21);

        DenseMatrix zt = addMatrices22(zdf11.toDense(), td12.toDense(), mf);
        zt = addMatrices22(zt, zdf22.toDense(), mf);
        zt = addMatrices22(zt, td21.toDense(), mf);
        zt = addMatrices22(zt, zof11.toDense(), mf);
        zt = addMatrices22(zt, to21.toDense(), mf);
        zt = addMatrices22(zt, zof22.toDense(), mf);
        zt = addMatrices22(zt, to12.toDense(), mf);
        zt = addMatrices22(zt, zif22.toDense(), mf);
        zt = addMatrices22(zt, ti12.toDense(), mf);
        zt = addMatrices22(zt, zif11.toDense(), mf);
        zt = addMatrices22(zt, ti21.toDense(), mf);

        Matrix tmpzt = idDiv3.times(zt);

        Matrix zf = getZ(rg, xg, mf);

        zt = addMatrices22(tmpzt.toDense(), zf.toDense(), mf);

        rt = zt.get(0, 0);
        xt = zt.get(1, 0);
    }

    @Override
    public void computeCurrents() {
        // step 3:
        // get the currents vectors
        // [ I1o ]               [ 1 ]
        // [ I1d ] = 1/3 * Ic1 * [ a²]
        // [ I1i ]               [ a ]
        //
        // [ I2o ]               [ 1 ]
        // [ I2d ] = -1/3 *Ic1 * [ a²]
        // [ I2i ]               [ a ]

        Matrix mI3 = getMatrixByType(AbstractShortCircuitCalculator.BlocType.Id, 1. / 3, mf);
        Matrix ma2Div3 = getMatrixByType(AbstractShortCircuitCalculator.BlocType.A2, 1. / 3, mf);
        Matrix maDiv3 = getMatrixByType(AbstractShortCircuitCalculator.BlocType.A, 1. / 3, mf);
        Matrix mMinusI = getMatrixByType(AbstractShortCircuitCalculator.BlocType.Id, -1., mf);

        mIo = mI3.times(mIc);
        mId = ma2Div3.times(mIc);
        mIi = maDiv3.times(mIc);

        mI2o = mMinusI.times(mIo);
        mI2d = mMinusI.times(mId);
        mI2i = mMinusI.times(mIi);
    }
}
