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
public class BiphasedCommonSupportShortCircuitCalculator extends AbstractShortCircuitCalculator {

    protected double ro12;
    protected double xo12;
    protected double ro22;
    protected double xo22;
    protected double ro21;
    protected double xo21;

    protected double rd12;
    protected double xd12;
    protected double rd22;
    protected double xd22;
    protected double rd21;
    protected double xd21;

    protected double  v2dxInit;
    protected double  v2dyInit;

    protected DenseMatrix zdf11;
    protected DenseMatrix zdf12;
    protected DenseMatrix zdf21;
    protected DenseMatrix zdf22;
    protected DenseMatrix zof11;
    protected DenseMatrix zof12;
    protected DenseMatrix zof21;
    protected DenseMatrix zof22;
    protected DenseMatrix zif11;
    protected DenseMatrix zif12;
    protected DenseMatrix zif21;
    protected DenseMatrix zif22;

    protected DenseMatrix zdf;
    protected DenseMatrix zof;

    protected DenseMatrix mI2o; // current from bus 2
    protected DenseMatrix mI2d;
    protected DenseMatrix mI2i;

    protected DenseMatrix mVo;
    protected DenseMatrix mVd;
    protected DenseMatrix mVi;

    protected double rt; // values of total impedance used to get Ic
    protected double xt;

    protected DenseMatrix mIc; // short circuit phase C1 current circulating from common support 1 to 2

    public BiphasedCommonSupportShortCircuitCalculator(double rdf, double xdf, double rof, double xof, double rg, double xg,
                                                       double initVx, double initVy,
                                                       double v2dxInit, double v2dyInit,
                                                       double ro12, double xo12, double ro22, double xo22, double ro21, double xo21,
                                                       double rd12, double xd12, double rd22, double xd22, double rd21, double xd21) {
        super(rdf, xdf, rof, xof, rg, xg, initVx, initVy);
        this.ro12 = ro12;
        this.ro21 = ro21;
        this.ro22 = ro22;
        this.xo12 = xo12;
        this.xo21 = xo21;
        this.xo22 = xo22;
        this.rd12 = rd12;
        this.rd21 = rd21;
        this.rd22 = rd22;
        this.xd12 = xd12;
        this.xd21 = xd21;
        this.xd22 = xd22;
        this.v2dxInit = v2dxInit;
        this.v2dyInit = v2dyInit;

        buildZxf(); // build all remaining matrix elements from inputs

    }

    public void computeZt() { }

    public void computeIc() { }

    public void computeCurrents() { }

    public void computeVoltages() {
        // Function using no input args
        // [ Vof ] = -tM * inv(Yo) * M * [ Iof ]
        // [ Vdf ] = -tM * inv(Yd) * M * [ Idf ] + tM * [ V(init) ]
        // [ Vif ] = -tM * inv(Yd) * M * [ Iif ]

        //get the voltage vectors
        // Vo :
        // [v1ox]          [ rof_11  -xof_11  rof_12  -xof_12 ]   [ i1ox ]
        // [v1oy] = -1  *  [ xof_11   rof_11  xof_12   rof_12 ] * [ i1oy ]
        // [v2ox]          [ rof_21  -xof_21  rof_22  -xof_22 ]   [ i2ox ]
        // [v2oy]          [ xof_21   rof_21  xof_22   rof_22 ]   [ i2oy ]
        DenseMatrix minusIo = new DenseMatrix(4, 1);
        minusIo.add(0, 0, -getmIo().toDense().get(0, 0));
        minusIo.add(1, 0, -getmIo().toDense().get(1, 0));
        minusIo.add(2, 0, -getmI2o().toDense().get(0, 0));
        minusIo.add(3, 0, -getmI2o().toDense().get(1, 0));

        mVo = zof.times(minusIo).toDense();

        // Vd :
        // [v1dx]          [ rdf_11  -xdf_11  rdf_12  -xdf_12 ]   [ i1ox ]     [v1dx(init)]
        // [v1dy] = -1  *  [ xdf_11   rdf_11  xdf_12   rdf_12 ] * [ i1oy ]  +  [v1dy(init)]
        // [v2dx]          [ rdf_21  -xdf_21  rdf_22  -xdf_22 ]   [ i2ox ]     [v2dx(init)]
        // [v2dy]          [ xdf_21   rdf_21  xdf_22   rdf_22 ]   [ i2oy ]     [v2dy(init)]
        DenseMatrix minusId = new DenseMatrix(4, 1);
        minusId.add(0, 0, -getmId().toDense().get(0, 0));
        minusId.add(1, 0, -getmId().toDense().get(1, 0));
        minusId.add(2, 0, -getmI2d().toDense().get(0, 0));
        minusId.add(3, 0, -getmI2d().toDense().get(1, 0));

        mVd = zdf.times(minusId).toDense();

        // Vi :
        // [v1ix]          [ rdf_11  -xdf_11  rdf_12  -xdf_12 ]   [ i1dx ]
        // [v1iy] = -1  *  [ xdf_11   rdf_11  xdf_12   rdf_12 ] * [ i1dy ]
        // [v2ix]          [ rdf_21  -xdf_21  rdf_22  -xdf_22 ]   [ i2dx ]
        // [v2iy]          [ xdf_21   rdf_21  xdf_22   rdf_22 ]   [ i2dy ]

        DenseMatrix minusIi = new DenseMatrix(4, 1);
        minusIi.add(0, 0, -getmIi().toDense().get(0, 0));
        minusIi.add(1, 0, -getmIi().toDense().get(1, 0));
        minusIi.add(2, 0, -getmI2i().toDense().get(0, 0));
        minusIi.add(3, 0, -getmI2i().toDense().get(1, 0));

        mVi = zdf.times(minusIi).toDense();

    }

    public void buildZxf() {

        zdf11 = new DenseMatrix(2, 2);
        zdf11.add(0, 0, rdf);
        zdf11.add(0, 1, -xdf);
        zdf11.add(1, 0, xdf);
        zdf11.add(1, 1, rdf);

        zdf12 = new DenseMatrix(2, 2);
        zdf12.add(0, 0, rd12);
        zdf12.add(0, 1, -xd12);
        zdf12.add(1, 0, xd12);
        zdf12.add(1, 1, rd12);

        zdf21 = new DenseMatrix(2, 2);
        zdf21.add(0, 0, rd21);
        zdf21.add(0, 1, -xd21);
        zdf21.add(1, 0, xd21);
        zdf21.add(1, 1, rd21);

        zdf22 = new DenseMatrix(2, 2);
        zdf22.add(0, 0, rd22);
        zdf22.add(0, 1, -xd22);
        zdf22.add(1, 0, xd22);
        zdf22.add(1, 1, rd22);

        zof11 = new DenseMatrix(2, 2);
        zof11.add(0, 0, rof);
        zof11.add(0, 1, -xof);
        zof11.add(1, 0, xof);
        zof11.add(1, 1, rof);

        zof12 = new DenseMatrix(2, 2);
        zof12.add(0, 0, ro12);
        zof12.add(0, 1, -xo12);
        zof12.add(1, 0, xo12);
        zof12.add(1, 1, ro12);

        zof21 = new DenseMatrix(2, 2);
        zof21.add(0, 0, ro21);
        zof21.add(0, 1, -xo21);
        zof21.add(1, 0, xo21);
        zof21.add(1, 1, ro21);

        zof22 = new DenseMatrix(2, 2);
        zof22.add(0, 0, ro22);
        zof22.add(0, 1, -xo22);
        zof22.add(1, 0, xo22);
        zof22.add(1, 1, ro22);

        zif11 = new DenseMatrix(2, 2);
        zif11.add(0, 0, rdf);
        zif11.add(0, 1, -xdf);
        zif11.add(1, 0, xdf);
        zif11.add(1, 1, rdf);

        zif12 = new DenseMatrix(2, 2);
        zif12.add(0, 0, rd12);
        zif12.add(0, 1, -xd12);
        zif12.add(1, 0, xd12);
        zif12.add(1, 1, rd12);

        zif21 = new DenseMatrix(2, 2);
        zif21.add(0, 0, rd21);
        zif21.add(0, 1, -xd21);
        zif21.add(1, 0, xd21);
        zif21.add(1, 1, rd21);

        zif22 = new DenseMatrix(2, 2);
        zif22.add(0, 0, rd22);
        zif22.add(0, 1, -xd22);
        zif22.add(1, 0, xd22);
        zif22.add(1, 1, rd22);

        //Matrix zof
        // [ rof_11  -xof_11  rof_12  -xof_12 ]
        // [ xof_11   rof_11  xof_12   rof_12 ]
        // [ rof_21  -xof_21  rof_22  -xof_22 ]
        // [ xof_21   rof_21  xof_22   rof_22 ]
        zof = new DenseMatrix(4, 4);
        zof.add(0, 0, rof);
        zof.add(0, 1, -xof);
        zof.add(1, 0, xof);
        zof.add(1, 1, rof);

        zof.add(0, 2, ro12);
        zof.add(0, 3, -xo12);
        zof.add(1, 2, xo12);
        zof.add(1, 3, ro12);

        zof.add(2, 0, ro21);
        zof.add(2, 1, -xo21);
        zof.add(3, 0, xo21);
        zof.add(3, 1, ro21);

        zof.add(2, 2, ro22);
        zof.add(2, 3, -xo22);
        zof.add(3, 2, xo22);
        zof.add(3, 3, ro22);

        //Matrix zdf
        zdf = new DenseMatrix(4, 4);
        zdf.add(0, 0, rdf);
        zdf.add(0, 1, -xdf);
        zdf.add(1, 0, xdf);
        zdf.add(1, 1, rdf);

        zdf.add(0, 2, rd12);
        zdf.add(0, 3, -xd12);
        zdf.add(1, 2, xd12);
        zdf.add(1, 3, rd12);

        zdf.add(2, 0, rd21);
        zdf.add(2, 1, -xd21);
        zdf.add(3, 0, xd21);
        zdf.add(3, 1, rd21);

        zdf.add(2, 2, rd22);
        zdf.add(2, 3, -xd22);
        zdf.add(3, 2, xd22);
        zdf.add(3, 3, rd22);

    }

    public DenseMatrix getmI2d() {
        return mI2d;
    }

    public DenseMatrix getmI2i() {
        return mI2i;
    }

    public DenseMatrix getmI2o() {
        return mI2o;
    }

    public DenseMatrix getmVd() {
        return mVd;
    }

    public DenseMatrix getmVi() {
        return mVi;
    }

    public DenseMatrix getmVo() {
        return mVo;
    }
}
