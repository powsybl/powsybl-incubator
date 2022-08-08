/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

import com.powsybl.incubator.simulator.util.*;
import com.powsybl.math.matrix.DenseMatrix;
import com.powsybl.openloadflow.network.LfBranch;
import com.powsybl.openloadflow.network.LfBus;
import com.powsybl.openloadflow.network.LfNetwork;
import com.powsybl.openloadflow.network.PiModel;

import java.util.*;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitResult {

    public class CommonSupportResult {

        private LfBus lfBus2; // FIXME : might be wrongly overwritten in the "resultsPerFault" presentation

        private double eth2x;

        private double eth2y;

        private DenseMatrix i2Fortescue; //fortescue vector of currents

        private DenseMatrix v2Fortescue; //fortescue vector of voltages

        CommonSupportResult(LfBus lfBus2, double eth2x, double eth2y,
                            double i2dx, double i2dy, double i2ox, double i2oy, double i2ix, double i2iy,
                            double dv2dx, double dv2dy, double dv2ox, double dv2oy, double dv2ix, double dv2iy) {
            this.lfBus2 = lfBus2;
            this.eth2x = eth2x;
            this.eth2y = eth2y;

            DenseMatrix mI2 = new DenseMatrix(6, 1);
            mI2.add(0, 0, i2ox);
            mI2.add(1, 0, i2oy);
            mI2.add(2, 0, i2dx);
            mI2.add(3, 0, i2dy);
            mI2.add(4, 0, i2ix);
            mI2.add(5, 0, i2iy);
            this.i2Fortescue = mI2.toDense();

            //construction of the fortescue vector vFortescue = t[Vh, Vd, Vi]
            double vdx = eth2x + dv2dx;
            double vdy = eth2y + dv2dy;
            double vhx = dv2ox;
            double vhy = dv2oy;
            double vix = dv2ix;
            double viy = dv2iy;

            DenseMatrix mV2 = new DenseMatrix(6, 1);
            mV2.add(0, 0, vhx);
            mV2.add(1, 0, vhy);
            mV2.add(2, 0, vdx);
            mV2.add(3, 0, vdy);
            mV2.add(4, 0, vix);
            mV2.add(5, 0, viy);
            this.v2Fortescue = mV2.toDense();
        }
    }

    private LfBus lfBus; // FIXME : might be wrongly overwritten in the "resultsPerFault" presentation

    private LfNetwork lfNetwork;

    private ShortCircuitNorm norm;

    private double rd; // equivalent direct impedance

    private double xd;

    private double ri; // equivalent inverse impedance

    private double xi;

    private double rh; // equivalent homopolar impedance

    private double xh;

    private double ethx;

    private double ethy;

    private DenseMatrix iFortescue; //fortescue vector of currents

    private DenseMatrix vFortescue; //fortescue vector of voltages

    private boolean isVoltageProfileUpdated;
    private List<DenseMatrix>  busNum2Dv;

    private EquationSystemFeeders eqSysFeedersDirect;

    private EquationSystemFeeders eqSysFeedersHomopolar;

    private Map<LfBus, EquationSystemBusFeedersResult> feedersAtBusResultsDirect;

    private Map<LfBus, EquationSystemBusFeedersResult> feedersAtBusResultsHomopolar;

    private ShortCircuitFault shortCircuitFault;

    private CommonSupportResult commonSupportResult; // used only for biphased with common support faults

    public ShortCircuitResult(ShortCircuitFault shortCircuitFault, LfBus lfBus,
                              double ifr, double ifi,
                              double rth, double xth, double ethr, double ethi, double dvr, double dvi,
                              EquationSystemFeeders eqSysFeeders, ShortCircuitNorm norm) {
        this.lfBus = lfBus;
        this.eqSysFeedersDirect = eqSysFeeders;
        this.shortCircuitFault = shortCircuitFault;
        this.norm = norm;

        this.rd = rth;
        this.xd = xth;
        this.ri = 0.;
        this.xi = 0.;
        this.rh = 0.;
        this.xh = 0.;

        this.ethx = ethr;
        this.ethy = ethi;

        //construction of the fortescue vector iFortescue = t[Ih, Id, Ii]
        double idx = ifr;
        double idy = ifi;
        double iix = 0.;
        double iiy = 0.;
        double ihx = 0.;
        double ihy = 0.;

        DenseMatrix mI = new DenseMatrix(6, 1);
        mI.add(0, 0, ihx);
        mI.add(1, 0, ihy);
        mI.add(2, 0, idx);
        mI.add(3, 0, idy);
        mI.add(4, 0, iix);
        mI.add(5, 0, iiy);
        this.iFortescue = mI.toDense();

        //construction of the fortescue vector vFortescue = t[Vh, Vd, Vi]
        double vdx = ethr + dvr;
        double vdy = ethi + dvi;
        double vhx = 0.;
        double vhy = 0.;
        double vix = 0.;
        double viy = 0.;

        DenseMatrix mV = new DenseMatrix(6, 1);
        mV.add(0, 0, vhx);
        mV.add(1, 0, vhy);
        mV.add(2, 0, vdx);
        mV.add(3, 0, vdy);
        mV.add(4, 0, vix);
        mV.add(5, 0, viy);
        this.vFortescue = mV.toDense();

        isVoltageProfileUpdated = false;

    }

    public ShortCircuitResult(ShortCircuitFault shortCircuitFault, LfBus lfBus,
                              double idx, double idy, double iox, double ioy, double iix, double iiy,
                              double rd, double xd, double ro, double xo, double ri, double xi,
                              double vdxinit, double vdyinit, double dvdx, double dvdy, double dvox, double dvoy, double dvix, double dviy,
                              EquationSystemFeeders eqSysFeedersDirect, EquationSystemFeeders eqSysFeedersHomopolar, ShortCircuitNorm norm) {
        this.lfBus = lfBus;
        this.eqSysFeedersDirect = eqSysFeedersDirect;
        this.eqSysFeedersHomopolar = eqSysFeedersHomopolar;
        this.shortCircuitFault = shortCircuitFault;
        this.norm = norm;

        this.rd = rd;
        this.xd = xd;
        this.ri = ri;
        this.xi = xi;
        this.rh = ro;
        this.xh = xo;

        this.ethx = vdxinit;
        this.ethy = vdyinit;

        //construction of the fortescue vector iFortescue = t[Ih, Id, Ii]
        DenseMatrix mI = new DenseMatrix(6, 1);
        mI.add(0, 0, iox);
        mI.add(1, 0, ioy);
        mI.add(2, 0, idx);
        mI.add(3, 0, idy);
        mI.add(4, 0, iix);
        mI.add(5, 0, iiy);
        this.iFortescue = mI.toDense();

        //construction of the fortescue vector vFortescue = t[Vh, Vd, Vi]
        double vdx = ethx + dvdx;
        double vdy = ethy + dvdy;
        double vhx = dvox;
        double vhy = dvoy;
        double vix = dvix;
        double viy = dviy;

        DenseMatrix mV = new DenseMatrix(6, 1);
        mV.add(0, 0, vhx);
        mV.add(1, 0, vhy);
        mV.add(2, 0, vdx);
        mV.add(3, 0, vdy);
        mV.add(4, 0, vix);
        mV.add(5, 0, viy);
        this.vFortescue = mV.toDense();

        isVoltageProfileUpdated = false;

    }

    public ShortCircuitResult(ShortCircuitFault shortCircuitFault, LfBus lfBus,
                              double idx, double idy, double iox, double ioy, double iix, double iiy,
                              double rd, double xd, double ro, double xo, double ri, double xi,
                              double vdxinit, double vdyinit, double dvdx, double dvdy, double dvox, double dvoy, double dvix, double dviy,
                              EquationSystemFeeders eqSysFeedersDirect, EquationSystemFeeders eqSysFeedersHomopolar, ShortCircuitNorm norm,
                              double i2dx, double i2dy, double i2ox, double i2oy, double i2ix, double i2iy,
                              double v2dxinit, double v2dyinit, double dv2dx, double dv2dy, double dv2ox, double dv2oy, double dv2ix, double dv2iy,
                              LfBus lfBus2) {
        this(shortCircuitFault, lfBus,
                idx, idy, iox, ioy, iix, iiy,
                rd, xd, ro, xo, ri, xi,
                vdxinit, vdyinit, dvdx, dvdy, dvox, dvoy, dvix, dviy,
                eqSysFeedersDirect, eqSysFeedersHomopolar, norm);

        this.commonSupportResult = new CommonSupportResult(lfBus2, v2dxinit, v2dyinit,
                i2dx, i2dy, i2ox, i2oy, i2ix,  i2iy,
                dv2dx, dv2dy, dv2ox, dv2oy, dv2ix, dv2iy);

    }

    public void updateFeedersResult() {
        //System.out.println(" VL name = " + shortCircuitVoltageLevelLocation);
        //System.out.println(" bus name = " + shortCircuitLfbusLocation);
        //System.out.println(" Icc = " + getIcc());
        //System.out.println(" Ih = " + iFortescue.get(0, 0) + " + j(" + iFortescue.get(1, 0) + ")");
        //System.out.println(" Id = " + iFortescue.get(2, 0) + " + j(" + iFortescue.get(3, 0) + ")");
        //System.out.println(" Ii = " + iFortescue.get(4, 0) + " + j(" + iFortescue.get(5, 0) + ")");
        //System.out.println(" Vh = " + vFortescue.get(0, 0) + " + j(" + vFortescue.get(1, 0) + ")");
        //System.out.println(" Vd = " + vFortescue.get(2, 0) + " + j(" + vFortescue.get(3, 0) + ")");
        //System.out.println(" Vi = " + vFortescue.get(4, 0) + " + j(" + vFortescue.get(5, 0) + ")");
        //System.out.println(" Eth = " + ethx + " + j(" + ethy + ")");

        if (isVoltageProfileUpdated) {

            /*for (Map.Entry<Integer, DenseMatrix> vd : bus2dv.entrySet()) {
                System.out.println(" dVd(" + vd.getKey() + ") = " + vd.getValue().get(2, 0) + " + j(" + vd.getValue().get(3, 0) + ")");
                System.out.println(" dVo(" + vd.getKey() + ") = " + vd.getValue().get(0, 0) + " + j(" + vd.getValue().get(1, 0) + ")");
                System.out.println(" dVi(" + vd.getKey() + ") = " + vd.getValue().get(4, 0) + " + j(" + vd.getValue().get(5, 0) + ")");
            }*/

            feedersAtBusResultsDirect = new HashMap<>(); // TODO : homopolar

            for (LfBus bus : lfNetwork.getBuses()) {
                //int busNum = bus.getNum();
                //double dvx = busNum2Dv.get(busNum).get(2, 0);
                //double dvy = busNum2Dv.get(busNum).get(3, 0);
                //double vx = dvx + ethx;
                //double vy = dvy + ethy;

                //System.out.println(" dVd(" + bus.getId() + ") = " + dvx + " + j(" + dvy + ")  Module = " + bus.getNominalV() * Math.sqrt(vx * vx + vy * vy));
                //System.out.println(" dVo(" + bus.getId() + ") = " + bus2dv.get(busNum).get(0, 0) + " + j(" + bus2dv.get(busNum).get(1, 0) + ")");
                //System.out.println(" dVi(" + bus.getId() + ") = " + bus2dv.get(busNum).get(4, 0) + " + j(" + bus2dv.get(busNum).get(5, 0) + ")");

                // Init of feeder results
                EquationSystemBusFeeders busFeeders = eqSysFeedersDirect.busToFeeders.get(bus);
                EquationSystemBusFeedersResult resultBusFeeders = new EquationSystemBusFeedersResult(busFeeders.getFeeders(), bus);
                feedersAtBusResultsDirect.put(bus, resultBusFeeders);  // TODO : homopolar

            }

            for (LfBranch branch : lfNetwork.getBranches()) {
                LfBus bus1 = branch.getBus1();
                LfBus bus2 = branch.getBus2();
                if (bus1 != null && bus2 != null) {
                    DenseMatrix yd12 = getAdmittanceMatrixBranch(branch, AdmittanceEquationSystem.AdmittanceType.ADM_THEVENIN);
                    int busNum1 = bus1.getNum();
                    double dvx1 = busNum2Dv.get(busNum1).get(2, 0);
                    double dvy1 = busNum2Dv.get(busNum1).get(3, 0);
                    int busNum2 = bus2.getNum();
                    double dvx2 = busNum2Dv.get(busNum2).get(2, 0);
                    double dvy2 = busNum2Dv.get(busNum2).get(3, 0);
                    DenseMatrix v12 = new DenseMatrix(4, 1);
                    v12.add(0, 0, dvx1 + 0.); //TODO : replace 1. by initial value
                    v12.add(1, 0, dvy1 + 0.); //TODO : replace 0. by initial value
                    v12.add(2, 0, dvx2 + 0.); //TODO : replace 1. by initial value
                    v12.add(3, 0, dvy2 + 0.); //TODO : replace 0. by initial value
                    DenseMatrix i12 = yd12.times(v12).toDense();
                    //System.out.println(" dI1d(" + branch.getId() + ") = " + i12.get(0, 0) + " + j(" + i12.get(1, 0) + ")  Module I1d = " + 1000. * 100. / bus1.getNominalV() * Math.sqrt((i12.get(0, 0) * i12.get(0, 0) + i12.get(1, 0) * i12.get(1, 0)) / 3));
                    //System.out.println(" dI2d(" + branch.getId() + ") = " + i12.get(2, 0) + " + j(" + i12.get(3, 0) + ")  Module I2d = " + 1000. * 100. / bus2.getNominalV() * Math.sqrt((i12.get(2, 0) * i12.get(2, 0) + i12.get(3, 0) * i12.get(3, 0)) / 3));

                    // Feeders :
                    // compute the sum of currents from branches at each bus
                    EquationSystemBusFeedersResult resultBus1Feeders = feedersAtBusResultsDirect.get(bus1); // TODO : homopolar
                    EquationSystemBusFeedersResult resultBus2Feeders = feedersAtBusResultsDirect.get(bus2); // TODO : homopolar

                    resultBus1Feeders.addIfeeders(i12.get(0, 0), i12.get(1, 0));
                    resultBus2Feeders.addIfeeders(i12.get(2, 0), i12.get(3, 0));

                }
            }

            // computing feeders contribution
            for (LfBus bus : lfNetwork.getBuses()) {
                EquationSystemBusFeedersResult busFeeders = feedersAtBusResultsDirect.get(bus); // TODO : homopolar
                busFeeders.updateContributions();
            }
        }
    }

    public double getIdx() {
        return iFortescue.get(2, 0);
    }

    public double getIdy() {
        return iFortescue.get(3, 0);
    }

    public double getIox() {
        return iFortescue.get(0, 0);
    }

    public double getIoy() {
        return iFortescue.get(1, 0);
    }

    public double getIcc() {
        // Icc = 1/sqrt(3) * Eth(pu) / Zth(pu) * SB(MVA) * 10e6 / (VB(kV) * 10e3)
        //return Math.sqrt((getIdx() * getIdx() + getIdy() * getIdy()) / 3) * 1000. * 100. / lfBus.getNominalV();
        return Math.sqrt((getIdx() * getIdx() + getIdy() * getIdy()) / 3) * 1000. * 100.;
    }

    public double getIk() {
        // Ik = c * Un / (sqrt(3) * Zk) = c / sqrt(3) * Eth(pu) / Zth(pu) * Sb / Vb
        return Math.sqrt((getIdx() * getIdx() + getIdy() * getIdy()) / 3) * 100.  / lfBus.getNominalV() * norm.getCmaxVoltageFactor(lfBus.getNominalV());
    }

    public double getPcc() {
        //Pcc = |Eth|*Icc*sqrt(3)
        return Math.sqrt(3) * getIcc() * lfBus.getV() * lfBus.getNominalV(); //TODO: check formula
    }

    public void setTrueVoltageProfileUpdate() {
        isVoltageProfileUpdated = true;
    }

    public void createEmptyFortescueVoltageVector(int nbBusses) {
        List<DenseMatrix> busNum2Dv = new ArrayList<>();
        for (int i = 0;  i < nbBusses; i++) {
            DenseMatrix mdV = new DenseMatrix(6, 1);
            busNum2Dv.add(mdV);
        }
        this.busNum2Dv = busNum2Dv;
    }

    public void fillVoltageInFortescueVector(int busNum, double dVdx, double dVdy) {
        this.busNum2Dv.get(busNum).add(2, 0, dVdx);
        this.busNum2Dv.get(busNum).add(3, 0, dVdy);
    }

    public void fillVoltageInFortescueVector(int busNum, double dVdx, double dVdy, double dVox, double dVoy, double dVix, double dViy) {
        this.busNum2Dv.get(busNum).add(0, 0, dVox);
        this.busNum2Dv.get(busNum).add(1, 0, dVoy);
        this.busNum2Dv.get(busNum).add(2, 0, dVdx);
        this.busNum2Dv.get(busNum).add(3, 0, dVdy);
        this.busNum2Dv.get(busNum).add(4, 0, dVix);
        this.busNum2Dv.get(busNum).add(5, 0, dViy);
    }

    public void setLfNetwork(LfNetwork lfNetwork) {
        this.lfNetwork = lfNetwork;
    }

    static DenseMatrix getAdmittanceMatrixBranch(LfBranch branch,
                                                 AdmittanceEquationSystem.AdmittanceType admittanceType) {

        // TODO : code duplicated with the admittance equation system, should be un-duplicated
        PiModel piModel = branch.getPiModel();
        if (piModel.getX() == 0) {
            throw new IllegalArgumentException("Branch '" + branch.getId() + "' has reactance equal to zero");
        }
        double rho = piModel.getR1();
        if (piModel.getZ() == 0) {
            throw new IllegalArgumentException("Branch '" + branch.getId() + "' has Z equal to zero");
        }
        double zInvSquare = 1 / (piModel.getZ() * piModel.getZ());
        double r = piModel.getR();
        double x = piModel.getX();
        double alpha = piModel.getA1();
        double cosA = Math.cos(Math.toRadians(alpha));
        double sinA = Math.sin(Math.toRadians(alpha));
        double gPi1 = piModel.getG1();
        double bPi1 = piModel.getB1();
        double gPi2 = piModel.getG2();
        double bPi2 = piModel.getB2();

        double g12 = rho * zInvSquare * (r * cosA + x * sinA);
        double b12 = -rho * zInvSquare * (x * cosA + r * sinA);
        double g1g12sum = rho * rho * (gPi1 + r * zInvSquare);
        double b1b12sum = rho * rho * (bPi1 - x * zInvSquare);
        if (admittanceType == AdmittanceEquationSystem.AdmittanceType.ADM_THEVENIN_HOMOPOLAR) {
            g12 = g12 * AdmittanceConstants.COEF_XO_XD; // Xo = 3 * Xd as a first approximation : TODO : improve when more data available
            b12 = b12 * AdmittanceConstants.COEF_XO_XD;
            g1g12sum = g1g12sum * AdmittanceConstants.COEF_XO_XD;
            b1b12sum = b1b12sum * AdmittanceConstants.COEF_XO_XD;
        }

        double g21 = rho * zInvSquare * (r * cosA + x * sinA);
        double b21 = rho * zInvSquare * (r * sinA - x * cosA);
        double g2g21sum = r * zInvSquare + gPi2;
        double b2b21sum = -x * zInvSquare + bPi2;
        if (admittanceType == AdmittanceEquationSystem.AdmittanceType.ADM_THEVENIN_HOMOPOLAR) {
            g21 = g21 * AdmittanceConstants.COEF_XO_XD; // Xo = 3 * Xd as a first approximation : TODO : improve when more data available
            b21 = b21 * AdmittanceConstants.COEF_XO_XD;
            g2g21sum = g2g21sum * AdmittanceConstants.COEF_XO_XD;
            b2b21sum = b2b21sum * AdmittanceConstants.COEF_XO_XD;
        }

        DenseMatrix mAdmittance = new DenseMatrix(4, 4);
        mAdmittance.add(0, 0, g1g12sum);
        mAdmittance.add(0, 1, -b1b12sum);
        mAdmittance.add(0, 2, -g12);
        mAdmittance.add(0, 3, b12);
        mAdmittance.add(1, 0, b1b12sum);
        mAdmittance.add(1, 1, g1g12sum);
        mAdmittance.add(1, 2, -b12);
        mAdmittance.add(1, 3, -g12);
        mAdmittance.add(2, 0, -g21);
        mAdmittance.add(2, 1, b21);
        mAdmittance.add(2, 2, g2g21sum);
        mAdmittance.add(2, 3, -b2b21sum);
        mAdmittance.add(3, 0, -b21);
        mAdmittance.add(3, 1, -g21);
        mAdmittance.add(3, 2, b2b21sum);
        mAdmittance.add(3, 3, g2g21sum);

        return mAdmittance.toDense();

    }

    // used for tests
    public double getIxFeeder(String busId, String feederId) {
        double ix = 0.;
        for (LfBus bus : lfNetwork.getBuses()) {
            if (bus.getId().equals(busId)) {
                EquationSystemBusFeedersResult resultFeeder = feedersAtBusResultsDirect.get(bus); // TODO : homopolar
                List<EquationSystemResultFeeder> busFeedersResults = resultFeeder.getBusFeedersResults();
                for (EquationSystemResultFeeder feeder : busFeedersResults) {
                    if (feeder.getId().equals(feederId)) {
                        ix = feeder.getIxContribution();
                    }
                }
            }
        }
        return ix;
    }

}
