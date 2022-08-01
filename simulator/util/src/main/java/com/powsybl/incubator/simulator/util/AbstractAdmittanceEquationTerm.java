/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.incubator.simulator.util.extensions.*;
import com.powsybl.math.matrix.DenseMatrix;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.equations.AbstractNamedEquationTerm;
import com.powsybl.openloadflow.equations.Variable;
import com.powsybl.openloadflow.equations.VariableSet;
import com.powsybl.openloadflow.network.ElementType;
import com.powsybl.openloadflow.network.LfBranch;
import com.powsybl.openloadflow.network.LfBus;
import com.powsybl.openloadflow.network.PiModel;

import java.util.List;
import java.util.Objects;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public abstract class AbstractAdmittanceEquationTerm extends AbstractNamedEquationTerm<VariableType, EquationType> implements LinearEquationTerm {

    private final LfBranch branch;

    protected final Variable<VariableType> v1rVar;

    protected final Variable<VariableType> v1iVar;

    protected final Variable<VariableType> v2rVar;

    protected final Variable<VariableType> v2iVar;

    protected final List<Variable<VariableType>> variables;

    protected double rho;

    protected double zInvSquare;

    protected double r;

    protected double x;

    protected double cosA;

    protected double sinA;

    protected double cos2A;

    protected double sin2A;

    protected double gPi1;

    protected double bPi1;

    protected double gPi2;

    protected double bPi2;

    protected HomopolarExtension homopolarExtension;

    protected MatrixFactory mf;

    // zero sequence additional attributes
    //
    // Proposed Transformer model :
    //      Ia       Yg    A'  rho                 B'     Yg        Ib        Zga : grounding impedance on A side (in ohms expressed on A side)
    //   A-->--3*Zga--+    +--(())--+--Zoa--+--Zob--+     +--3*ZGb--<--B      Zoa : leakage impedance of A-winding (in ohms expressed on B side)
    //                Y +                   |           + Y                   Zob : leakage impdedance of B-winding (in ohms expressed on B side)
    //                    + D              Zom        + D                     Zom : magnetizing impedance of the two windings (in ohms expressed on B side)
    //                    |                 |         |                       Zgb : grounding impedance on B side (in ohms expressed on B side)
    //                    |                 |         |                       rho might be a complex value
    //                    |    free fluxes \          |
    //                    |                 |         |
    //                  /////             /////     /////                     A' and B' are connected to Yg, Y or D depending on the winding connection type (Y to ground, Y or Delta)
    //

    public class HomopolarExtension {
        //values here are expressed in pu (Vnom_B, Sbase = 100.)
        protected double ro; // ro = roa + rob
        protected double xo; // xo = xoa + xob

        protected double gom; // Zom = 1 / Yom with Yom = gom + j*bom
        protected double bom;

        protected double rga;
        protected double xga;

        protected double rgb;
        protected double xgb;

        protected double zoInvSquare;

        ShortCircuitTransformerLeg.LegConnectionType leg1ConnectionType;
        ShortCircuitTransformerLeg.LegConnectionType leg2ConnectionType;

        boolean isFreeFluxes;

        public HomopolarExtension() {
            ro = 0.;
            xo = 0.;

            gom = 0.;
            bom = 0.;

            rga = 0.;
            xga = 0.;

            rgb = 0.;
            xgb = 0.;

            zoInvSquare = 0.;

            leg1ConnectionType = ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED;
            leg2ConnectionType = ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED;

            isFreeFluxes = false;

            DenseMatrix homopolarAdmittanceMatrix;

        }
    }

    public AbstractAdmittanceEquationTerm(LfBranch branch, LfBus bus1, LfBus bus2, VariableSet<VariableType> variableSet, MatrixFactory mf) {
        this.branch = Objects.requireNonNull(branch);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);
        Objects.requireNonNull(variableSet);

        this.mf = mf;

        v1rVar = variableSet.getVariable(bus1.getNum(), VariableType.BUS_VR);
        v2rVar = variableSet.getVariable(bus2.getNum(), VariableType.BUS_VR);
        v1iVar = variableSet.getVariable(bus1.getNum(), VariableType.BUS_VI);
        v2iVar = variableSet.getVariable(bus2.getNum(), VariableType.BUS_VI);

        variables = List.of(v1rVar, v2rVar, v1iVar, v2iVar);

        PiModel piModel = branch.getPiModel();
        if (piModel.getX() == 0) {
            throw new IllegalArgumentException("Branch '" + branch.getId() + "' has reactance equal to zero");
        }
        rho = piModel.getR1();
        if (piModel.getZ() == 0) {
            throw new IllegalArgumentException("Branch '" + branch.getId() + "' has Z equal to zero");
        }
        zInvSquare = 1 / (piModel.getZ() * piModel.getZ());
        r = piModel.getR();
        x = piModel.getX();
        double alpha = piModel.getA1();
        cosA = Math.cos(Math.toRadians(alpha));
        sinA = Math.sin(Math.toRadians(alpha));
        cos2A = Math.cos(Math.toRadians(2 * alpha));
        sin2A = Math.sin(Math.toRadians(2 * alpha));

        gPi1 = piModel.getG1();
        bPi1 = piModel.getB1();
        gPi2 = piModel.getG2();
        bPi2 = piModel.getB2();
    }

    public void setHomopolarAttributes() {

        homopolarExtension = new HomopolarExtension();

        //TODO : update with values in input, we need to access extra data and iidm element of the branch
        //Default initialization if no homopolar values available
        homopolarExtension.ro = r /  AdmittanceConstants.COEF_XO_XD;
        homopolarExtension.xo = x /  AdmittanceConstants.COEF_XO_XD;

        homopolarExtension.gom = gPi1 * AdmittanceConstants.COEF_XO_XD; //TODO : adapt
        homopolarExtension.bom = bPi1 * AdmittanceConstants.COEF_XO_XD;  //TODO : adapt

        homopolarExtension.rga = 0.;
        homopolarExtension.xga = 0.;

        homopolarExtension.rgb = 0.;
        homopolarExtension.xgb = 0.;

        homopolarExtension.leg1ConnectionType = ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED; // if the branch is not a transfo, then it is the correct default behaviour
        homopolarExtension.leg2ConnectionType = ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED;

        homopolarExtension.isFreeFluxes = false;

        if (branch.getBranchType() == LfBranch.BranchType.LINE) {
            // branch is a line and homopolar data available
            ShortCircuitLine shortCircuitLine = (ShortCircuitLine) branch.getProperty(ShortCircuitExtensions.PROPERTY_NAME);
            double rCoeff = shortCircuitLine.getCoeffRo();
            double xCoeff = shortCircuitLine.getCoeffXo();
            homopolarExtension.ro = r * rCoeff;
            homopolarExtension.xo = x * xCoeff;
            homopolarExtension.gom = gPi1 / rCoeff; //TODO : adapt
            homopolarExtension.bom = bPi1 / xCoeff;  //TODO : adapt

        } else if (branch.getBranchType() == LfBranch.BranchType.TRANSFO_2) {
            // branch is a 2 windings transformer and homopolar data available
            ShortCircuitT2W shortCircuitT2W = (ShortCircuitT2W) branch.getProperty(ShortCircuitExtensions.PROPERTY_NAME);
            double rCoeff = shortCircuitT2W.getCoeffRo();
            double xCoeff = shortCircuitT2W.getCoeffXo();
            homopolarExtension.ro = r * rCoeff;
            homopolarExtension.xo = x * xCoeff;
            homopolarExtension.gom = gPi1 / rCoeff; //TODO : adapt
            homopolarExtension.bom = bPi1 / xCoeff;  //TODO : adapt

            homopolarExtension.leg1ConnectionType =  shortCircuitT2W.getLeg1().getLegConnectionType();
            homopolarExtension.leg2ConnectionType =  shortCircuitT2W.getLeg2().getLegConnectionType();
        } else if (branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_1
                || branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_2
                || branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_3) {
            // branch is leg1 of a 3 windings transformer and homopolar data available
            //throw new IllegalArgumentException("Branch " + branch.getId() + " has a not yet supported type");
            ShortCircuitT3W shortCircuitT3W = (ShortCircuitT3W) branch.getProperty(ShortCircuitExtensions.PROPERTY_NAME);
            double rCoeff = 1.0;
            double xCoeff = 1.0;
            if (branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_1) {
                rCoeff = shortCircuitT3W.getLeg1().getCoeffRo();
                xCoeff = shortCircuitT3W.getLeg1().getCoeffXo();
                homopolarExtension.leg1ConnectionType = shortCircuitT3W.getLeg1().getLegConnectionType();
                homopolarExtension.isFreeFluxes = shortCircuitT3W.getLeg1().isFreeFluxes();
            } else if (branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_2) {
                rCoeff = shortCircuitT3W.getLeg2().getCoeffRo();
                xCoeff = shortCircuitT3W.getLeg2().getCoeffXo();
                homopolarExtension.leg1ConnectionType = shortCircuitT3W.getLeg2().getLegConnectionType();
                homopolarExtension.isFreeFluxes = shortCircuitT3W.getLeg2().isFreeFluxes();
            } else if (branch.getBranchType() == LfBranch.BranchType.TRANSFO_3_LEG_3) {
                rCoeff = shortCircuitT3W.getLeg3().getCoeffRo();
                xCoeff = shortCircuitT3W.getLeg3().getCoeffXo();
                homopolarExtension.leg1ConnectionType = shortCircuitT3W.getLeg3().getLegConnectionType();
                homopolarExtension.isFreeFluxes = shortCircuitT3W.getLeg3().isFreeFluxes();
            } else {
                throw new IllegalArgumentException("Branch " + branch.getId() + " has unknown 3-winding leg number");
            }

            homopolarExtension.ro = r * rCoeff;
            homopolarExtension.xo = x * xCoeff;
            homopolarExtension.gom = gPi1 / rCoeff; //TODO : adapt
            homopolarExtension.bom = bPi1 / xCoeff;  //TODO : adapt

        } else {
            throw new IllegalArgumentException("Branch " + branch.getId() + " has a not yet supported type");
        }

        homopolarExtension.zoInvSquare = 1 / (homopolarExtension.ro * homopolarExtension.ro + homopolarExtension.xo * homopolarExtension.xo);
    }

    public DenseMatrix computeHomopolarAdmittanceMatrix() {

        DenseMatrix mo = mf.create(4, 4, 16).toDense();

        double infiniteImpedanceAdmittance = AdmittanceConstants.INFINITE_IMPEDANCE_ADMITTANCE_VALUE;

        // if the free fluxes option is false, we suppose that if Yom given in input is zero, then Zom = is zero  : TODO : see if there is a more robust way to handle this
        // if the free fluxes option is true, Zom is infinite and Yom is then considered as zero
        double rm = 0.;
        double xm = 0.;
        if (homopolarExtension.bom != 0. || homopolarExtension.gom != 0.) {
            rm = homopolarExtension.gom / (homopolarExtension.bom * homopolarExtension.bom + homopolarExtension.gom * homopolarExtension.gom);
            xm = -homopolarExtension.bom / (homopolarExtension.bom * homopolarExtension.bom + homopolarExtension.gom * homopolarExtension.gom);
        }

        // we suppose that zob = zoa = Zo / 2  : TODO : check this is an acceptable approximation
        double roa = homopolarExtension.ro / 2.;
        double xoa = homopolarExtension.xo / 2.;
        double rob = homopolarExtension.ro / 2.;
        double xob = homopolarExtension.xo / 2.;

        // we suppose that all impedance and admittance terms of the homopolar extension are per-unitized on Sbase = 100 MVA and Vnom = Vnom on B side
        if ((homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y)
                || (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.DELTA)
                || (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.DELTA && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y)
                || (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.DELTA && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.DELTA)
                || (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y && homopolarExtension.isFreeFluxes)
                || (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED && homopolarExtension.isFreeFluxes)) {
            // homopolar admittance matrix is zero-Matrix
            mo.set(0, 0, infiniteImpedanceAdmittance);
            mo.set(1, 1, infiniteImpedanceAdmittance);
            mo.set(2, 2, infiniteImpedanceAdmittance);
            mo.set(3, 3, infiniteImpedanceAdmittance);

        } else if (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y) {
            // we suppose that Zoa = Zo given in input for the transformer
            // we suppose that if Yom given in input is zero, then Zom = is zero : TODO : see if there is a more robust way to handle this

            // we have yo11 = 1 / ( 3Zga(pu) + (Zoa(pu)+ Zom(pu))/(rho*e(jAlpha))² )
            // and yo12 = yo22 = yo21 = 0.
            // 3Zga(pu) + Zoa(pu)/(rho*e(jAlpha))² + Zom/(rho*e(jAlpha))² = 3*rg + 1/rho²*((roa+rom)cos2A-(xoa+xom)sin2A) + j(3*xg + 1/rho²*((xoa+xom)cos2A+(roa+rom)sin2A) )
            double req = 3 * homopolarExtension.rga + 1 / (rho * rho) * ((homopolarExtension.ro + rm) * cos2A - (homopolarExtension.xo + xm) * sin2A);
            double xeq = 3 * homopolarExtension.xga + 1 / (rho * rho) * ((homopolarExtension.xo + xm) * cos2A + (homopolarExtension.ro + rm) * sin2A);
            double bo11 = -xeq / (xeq * xeq + req * req);
            double go11 = req / (xeq * xeq + req * req);
            mo.set(0, 0, go11);
            mo.set(0, 1, -bo11);
            mo.set(1, 0, bo11);
            mo.set(1, 1, go11);
            mo.set(2, 2, infiniteImpedanceAdmittance);
            mo.set(3, 3, infiniteImpedanceAdmittance);
        } else if (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED) {
            // we suppose that zob = Zo given in input for the transformer
            // we suppose that if Yom given in input is zero, then Zom = is zero : TODO : see if there is a more robust way to handle this

            // we have yo22 = 1 / ( 3Zga(pu) + Zob(pu) + Zom(pu) )
            // and yo12 = yo11 = yo21 = 0.
            // 3Zgb(pu) + Zob(pu) + Zom = 3*rg + rob + rom + j(3*xg + xob + xom )
            double req = 3 * homopolarExtension.rgb +  homopolarExtension.ro + rm;
            double xeq = 3 * homopolarExtension.xgb +  homopolarExtension.xo + xm;
            double bo22 = -xeq / (xeq * xeq + req * req);
            double go22 = req / (xeq * xeq + req * req);
            mo.set(0, 0, infiniteImpedanceAdmittance);
            mo.set(1, 1, infiniteImpedanceAdmittance);
            mo.set(2, 2, go22);
            mo.set(2, 3, -bo22);
            mo.set(3, 2, bo22);
            mo.set(3, 3, go22);
        } else if (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.DELTA) {

            // we suppose that if Yom given in input is zero, then Zom = is zero : TODO : see if there is a more robust way to handle this

            // we have yo11 = 1 / ( 3Zga(pu) + (Zoa(pu) + 1 / (1/Zom + 1/Zob))/(rho*e(jAlpha))² )
            // and yo12 = yo22 = yo21 = 0.
            // using Ztmp = Zoa(pu) + 1 / (Yom + 1/Zob)
            // 3Zga(pu) + (Zoa(pu) + 1 / (Yom + 1/Zob))/(rho*e(jAlpha))² = 3*rg + 1/rho²*((rtmp)cos2A-(xtmp)sin2A) + j(3*xg + 1/rho²*((xtmp)cos2A+(rtmp)sin2A) )
            double bob = -xob / (rob * rob + xob * xob);
            double gob = rob / (rob * rob + xob * xob);
            double bombob = homopolarExtension.bom + bob;
            double gomgob = homopolarExtension.gom + gob;
            double rtmp = roa + gomgob / (gomgob * gomgob + bombob * bombob);
            double xtmp = xoa - bombob / (gomgob * gomgob + bombob * bombob);
            double req = 3 * homopolarExtension.rga + 1 / (rho * rho) * (rtmp * cos2A - xtmp * sin2A);
            double xeq = 3 * homopolarExtension.xga + 1 / (rho * rho) * (xtmp * cos2A + rtmp * sin2A);
            double bo11 = -xeq / (xeq * xeq + req * req);
            double go11 = req / (xeq * xeq + req * req);

            if (homopolarExtension.isFreeFluxes) {
                // we have Zm = infinity : yo11 = 1 / ( 3Zga(pu) + (Zoa(pu) + 1/Zob(pu))/(rho*e(jAlpha))² )
                rtmp = roa + rob;
                xtmp = xoa + xob;
                req = 3 * homopolarExtension.rga + 1 / (rho * rho) * (rtmp * cos2A - xtmp * sin2A);
                xeq = 3 * homopolarExtension.xga + 1 / (rho * rho) * (xtmp * cos2A + rtmp * sin2A);
                bo11 = -xeq / (xeq * xeq + req * req);
                go11 = req / (xeq * xeq + req * req);
            }

            mo.set(0, 0, go11);
            mo.set(0, 1, -bo11);
            mo.set(1, 0, bo11);
            mo.set(1, 1, go11);
            mo.set(2, 2, infiniteImpedanceAdmittance);
            mo.set(3, 3, infiniteImpedanceAdmittance);

        } else if (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.DELTA && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED) {

            // we have yo22 = 1 / ( 3Zga(pu) + Zob(pu) + 1/(1/Zom(pu)+1/Zoa(pu)) )
            // and yo12 = yo11 = yo21 = 0.
            //  3Zgb(pu) + Zob(pu) + Zom = 3*rg + rob + rom + j(3*xg + xob + xom )
            double boa = -xoa / (roa * roa + xoa * xoa);
            double goa = roa / (roa * roa + xoa * xoa);
            double bomboa = homopolarExtension.bom + boa;
            double gomgoa = homopolarExtension.gom + goa;

            double req = 3 * homopolarExtension.rgb + rob + gomgoa / (gomgoa * gomgoa + bomboa * bomboa);
            double xeq = 3 * homopolarExtension.xgb + xob - bomboa / (gomgoa * gomgoa + bomboa * bomboa);

            double bo22 = -xeq / (xeq * xeq + req * req);
            double go22 = req / (xeq * xeq + req * req);

            if (homopolarExtension.isFreeFluxes) {
                // we have Zm = infinity : yo22 = 1 / ( 3Zga(pu) + Zob(pu) + Zoa(pu) )
                // and yo12 = yo11 = yo21 = 0.

                req = 3 * homopolarExtension.rgb + rob + roa;
                xeq = 3 * homopolarExtension.xgb + xob + xoa;

                bo22 = -xeq / (xeq * xeq + req * req);
                go22 = req / (xeq * xeq + req * req);
            }

            mo.set(0, 0, infiniteImpedanceAdmittance);
            mo.set(1, 1, infiniteImpedanceAdmittance);
            mo.set(2, 2, go22);
            mo.set(2, 3, -bo22);
            mo.set(3, 2, bo22);
            mo.set(3, 3, go22);

        } else if (homopolarExtension.leg1ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED && homopolarExtension.leg2ConnectionType == ShortCircuitTransformerLeg.LegConnectionType.Y_GROUNDED) {

            double go11 = 0.;
            double bo11 = 0.;
            double go12 = 0.;
            double bo12 = 0.;
            double go22 = 0.;
            double bo22 = 0.;

            if (!homopolarExtension.isFreeFluxes) {
                // Case where fluxes are forced, meaning that Zm is not ignored (and could be zero with a direct connection to ground)
                //
                //  k = rho*e(jAlpha))
                //
                // [Ia]                   1                             [ Zom+Zob+3Zgs    -Zom/k        ] [Va]
                // [  ] = ------------------------------------------  * [                               ] [  ]
                // [Ib]   (Zom/k²+3Zga+Zoa/k²)(Zom+Zob+3Zgb)-(Zom/k)²   [   -Zom/k   Zom/k²+3Zga+Zoa/k² ] [Vb]
                // [  ]                                                 [                               ] [  ]
                //
                // Zc = Zom/k²+3Zga+Zoa/k²
                // Zd = Zom+Zob+3Zgb
                // Ze = Zom/k
                //
                // we suppose that if Yom given in input is zero, then Zom = is zero : TODO : see if there is a more robust way to handle this

                double rc = 1 / (rho * rho) * (cos2A * (rm + roa) + sin2A * (xm + xoa)) + 3 * homopolarExtension.rga;
                double xc = 1 / (rho * rho) * (cos2A * (xm + xoa) - sin2A * (rm + roa)) + 3 * homopolarExtension.xga;
                double rd = rm + rob + 3 * homopolarExtension.rgb;
                double xd = xm + xob + 3 * homopolarExtension.xgb;
                double re = (rm * cosA + xm * sinA) / rho;
                double xe = (xm * cosA - rm * sinA) / rho;

                // this gives :
                // [Ia]         1         [ Zd -Ze ] [Va]
                // [  ] = ------------- * [        ] [  ]
                // [Ib]   Zc * Zd - Ze²   [-Ze  Zc ] [Vb]
                // [  ]                   [        ] [  ]
                //
                // We set Z2denom = Zc * Zd - Ze²
                double r2denom = rc * rd - xc * xd - re * re + xe * xe;
                double x2demon = rc * xd + xc * rd - 2 * re * xe;
                double g2demon = r2denom / (r2denom * r2denom + x2demon * x2demon);
                double b2demon = -x2demon  / (r2denom * r2denom + x2demon * x2demon);
                go11 = g2demon * rd - b2demon * xd;
                bo11 = b2demon * rd + g2demon * xd;
                go12 = -g2demon * re + b2demon * xe;
                bo12 = -b2demon * re - g2demon * xe;
                go22 = g2demon * rc - b2demon * xc;
                bo22 = b2demon * rc + g2demon * xc;

            } else {
                //
                // Zm = infinity
                //  k = rho*e(jAlpha))
                //
                // [Ia]                   1               [    1    -1/k  ] [Va]
                // [  ] = ---------------------------   * [               ] [  ]
                // [Ib]   3Zga+Zoa/k²+Zob/k²+3Zgb/k²)     [   -1/k   1/k² ] [Vb]
                // [  ]                                   [               ] [  ]
                //
                // Zc = 3Zga+Zoa/k²+Zob/k²+3Zgb/k²)
                // Zd = Zom+Zob+3Zgb
                // Ze = Zom/k

                double rc = 1 / (rho * rho) * (cos2A * (rob + roa + 3 * homopolarExtension.rgb) + sin2A * (xoa + xob + 3 * homopolarExtension.xgb)) + 3 * homopolarExtension.rga;
                double xc = 1 / (rho * rho) * (cos2A * (xob + xoa + 3 * homopolarExtension.xgb) - sin2A * (rob + roa + 3 * homopolarExtension.rgb)) + 3 * homopolarExtension.xga;

                //double re = (rm * cosA + xm * sinA) / rho;
                //double xe = (xm * cosA - rm * sinA) / rho;

                // this gives :
                // [Ia]         1         [ 1   -1/k ] [Va]
                // [  ] = ------------- * [          ] [  ]
                // [Ib]         Zc        [-1/k  1/k²] [Vb]
                // [  ]                   [          ] [  ]
                //
                // We set Z2denom = Zc * Zd - Ze²
                double gcdemon = rc / (rc * rc + xc * xc);
                double bcdemon = -xc  / (rc * rc + xc * xc);

                go11 = gcdemon;
                bo11 = bcdemon;
                go12 = -(gcdemon * cosA + bcdemon * sinA) / rho;
                bo12 = -(bcdemon * cosA - gcdemon * sinA) / rho;
                go22 = (gcdemon * cos2A + bcdemon * sin2A) / (rho * rho);
                bo22 = (bcdemon * cos2A - gcdemon * sin2A) / (rho * rho);
            }
            mo.set(0, 0, go11);
            mo.set(0, 1, -bo11);
            mo.set(1, 0, bo11);
            mo.set(1, 1, go11);

            mo.set(2, 2, go22);
            mo.set(2, 3, -bo22);
            mo.set(3, 2, bo22);
            mo.set(3, 3, go22);

            mo.set(0, 2, go12);
            mo.set(0, 3, -bo12);
            mo.set(1, 2, bo12);
            mo.set(1, 3, go12);

            mo.set(2, 0, go12);
            mo.set(2, 1, -bo12);
            mo.set(3, 0, bo12);
            mo.set(3, 1, go12);
        } else {
            throw new IllegalArgumentException("Branch " + branch.getId() + " configuration is not supported yet : " + homopolarExtension.leg1ConnectionType + " --- " + homopolarExtension.leg2ConnectionType);
        }

        return mo;
    }

    @Override
    public List<Variable<VariableType>> getVariables() {
        return variables;
    }

    @Override
    public ElementType getElementType() {
        return ElementType.BRANCH;
    }

    @Override
    public int getElementNum() {
        return branch.getNum();
    }

    @Override
    public double eval() {
        throw new UnsupportedOperationException("Not needed");
    }

    @Override
    public double der(Variable<VariableType> variable) {
        throw new UnsupportedOperationException("Not needed");
    }

    @Override
    public boolean hasRhs() {
        return false;
    }

    @Override
    public double rhs() {
        return 0;
    }
}
