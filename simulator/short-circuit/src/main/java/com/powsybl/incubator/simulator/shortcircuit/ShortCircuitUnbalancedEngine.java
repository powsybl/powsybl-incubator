/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.shortcircuit;

//import com.powsybl.math.matrix.DenseMatrix;

import com.powsybl.iidm.network.Network;
import com.powsybl.incubator.simulator.util.*;
import com.powsybl.math.matrix.DenseMatrix;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import com.powsybl.openloadflow.network.LfBus;
import org.apache.commons.math3.util.Pair;

import java.util.*;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ShortCircuitUnbalancedEngine extends AbstractShortCircuitEngine {

    public ShortCircuitUnbalancedEngine(Network network, ShortCircuitEngineParameters parameters) {
        super(network, parameters);
    }

    @Override
    public void run() {

        if (parameters.getAnalysisType() == ShortCircuitEngineParameters.AnalysisType.SYSTEMATIC) {
            buildSystematicList(ShortCircuitFault.ShortCircuitType.MONOPHASED); // TODO : by default it is monophased, could be changed to choose type of systematic default
            // Biphased common support faults will not be supported yet in systematic
        }

        // We handle a pre-treatement of faults given in input:
        // - filtering of some inconsistencies on the bus identification
        // - addition of info in each fault to ease the identification in LfNetwork of iidm info
        Pair<List<ShortCircuitFault>, List<ShortCircuitFault>> faultLists = buildFaultListsFromInputs();

        solverFaultList = faultLists.getKey();
        solverBiphasedFaultList = faultLists.getValue();

        AdmittanceLinearResolutionParameters admittanceLinearResolutionParametersHomopolar = new AdmittanceLinearResolutionParameters(acLoadFlowParameters,
                parameters.getMatrixFactory(), solverFaultList, parameters.isVoltageUpdate(),
                getAdmittanceVoltageProfileTypeFromParam(), getAdmittancePeriodTypeFromParam(), AdmittanceEquationSystem.AdmittanceType.ADM_THEVENIN_HOMOPOLAR,
                parameters.isIgnoreShunts(), parameters.getAdditionalDataInfo(), parameters.getNorm(), solverBiphasedFaultList);

        AdmittanceLinearResolutionParameters admittanceLinearResolutionParametersDirect = new AdmittanceLinearResolutionParameters(acLoadFlowParameters,
                parameters.getMatrixFactory(), solverFaultList, parameters.isVoltageUpdate(),
                getAdmittanceVoltageProfileTypeFromParam(), getAdmittancePeriodTypeFromParam(), AdmittanceEquationSystem.AdmittanceType.ADM_THEVENIN,
                parameters.isIgnoreShunts(), parameters.getAdditionalDataInfo(), parameters.getNorm(), solverBiphasedFaultList);

        AdmittanceLinearResolution directResolution = new AdmittanceLinearResolution(network, admittanceLinearResolutionParametersDirect);
        AdmittanceLinearResolution homopolarResolution = new AdmittanceLinearResolution(network, admittanceLinearResolutionParametersHomopolar);

        directResolution.run();
        homopolarResolution.run();

        //Build the ShortCircuit results using the linear resolution computation results
        resultsPerFault = new LinkedHashMap<>();
        processAdmittanceLinearResolutionResults(directResolution, homopolarResolution, ShortCircuitFault.ShortCircuitType.MONOPHASED);
        processAdmittanceLinearResolutionResults(directResolution, homopolarResolution, ShortCircuitFault.ShortCircuitType.BIPHASED);
        processAdmittanceLinearResolutionResults(directResolution, homopolarResolution, ShortCircuitFault.ShortCircuitType.BIPHASED_GROUND);
        processAdmittanceLinearResolutionResults(directResolution, homopolarResolution, ShortCircuitFault.ShortCircuitType.BIPHASED_COMMON_SUPPORT);
    }

    public void processAdmittanceLinearResolutionResults(AdmittanceLinearResolution directResolution, AdmittanceLinearResolution homopolarResolution, ShortCircuitFault.ShortCircuitType shortCircuitType) {

        int numResult = 0;
        for (AdmittanceLinearResolution.AdmittanceLinearResolutionResult directResult : directResolution.results) {

            AdmittanceLinearResolution.AdmittanceLinearResolutionResult homopolarResult = homopolarResolution.results.get(numResult);
            numResult++;

            LfBus lfBus1 = directResult.getBus();

            List<ShortCircuitFault> matchingFaults = new ArrayList<>();

            if (shortCircuitType == ShortCircuitFault.ShortCircuitType.MONOPHASED
                    || shortCircuitType == ShortCircuitFault.ShortCircuitType.BIPHASED
                    || shortCircuitType == ShortCircuitFault.ShortCircuitType.BIPHASED_GROUND) {
                for (ShortCircuitFault scfe : solverFaultList) {
                    if (lfBus1.getId().equals(scfe.getLfBusInfo()) && scfe.getType() == shortCircuitType) {
                        matchingFaults.add(scfe);
                    }
                }
            }

            if (shortCircuitType == ShortCircuitFault.ShortCircuitType.BIPHASED_COMMON_SUPPORT) {
                for (ShortCircuitFault scfe : solverBiphasedFaultList) {
                    if (lfBus1.getId().equals(scfe.getLfBusInfo()) && scfe.getType() == shortCircuitType) {
                        matchingFaults.add(scfe);
                    }
                }
            }

            double v1dxInit = directResult.getEthr();
            double v1dyInit = directResult.getEthi();

            double rdf = directResult.getRthz11();
            double xdf = directResult.getXthz12();

            double rof = homopolarResult.getRthz11();
            double xof = homopolarResult.getXthz12();

            for (ShortCircuitFault scf : matchingFaults) {

                double rf = scf.getZfr();
                double xf = scf.getZfi();

                MatrixFactory mf = parameters.getMatrixFactory();

                Matrix mIo = mf.create(2, 1, 2);
                Matrix mId = mf.create(2, 1, 2);
                Matrix mIi = mf.create(2, 1, 2);

                ShortCircuitResult res;

                if (shortCircuitType == ShortCircuitFault.ShortCircuitType.MONOPHASED
                        || shortCircuitType == ShortCircuitFault.ShortCircuitType.BIPHASED
                        || shortCircuitType == ShortCircuitFault.ShortCircuitType.BIPHASED_GROUND) {
                    if (shortCircuitType == ShortCircuitFault.ShortCircuitType.MONOPHASED) {
                        MonophasedShortCircuitCalculator monophasedCalculator = new MonophasedShortCircuitCalculator(rdf, xdf, rof, xof, rf, xf, v1dxInit, v1dyInit, mf);
                        monophasedCalculator.computeCurrents();

                        mIo = monophasedCalculator.getmIo();
                        mId = monophasedCalculator.getmId();
                        mIi = monophasedCalculator.getmIi();

                    } else if (shortCircuitType == ShortCircuitFault.ShortCircuitType.BIPHASED) {
                        BiphasedShortCircuitCalculator biphasedCalculator = new BiphasedShortCircuitCalculator(rdf, xdf, rof, xof, rf, xf, v1dxInit, v1dyInit, mf);
                        biphasedCalculator.computeCurrents();

                        mIo = biphasedCalculator.getmIo();
                        mId = biphasedCalculator.getmId();
                        mIi = biphasedCalculator.getmIi();
                    } else if (shortCircuitType == ShortCircuitFault.ShortCircuitType.BIPHASED_GROUND) {
                        BiphasedGroundShortCircuitCalculator biphasedGrCalculator = new BiphasedGroundShortCircuitCalculator(rdf, xdf, rof, xof, rf, xf, v1dxInit, v1dyInit, mf);
                        biphasedGrCalculator.computeCurrents();

                        mIo = biphasedGrCalculator.getmIo();
                        mId = biphasedGrCalculator.getmId();
                        mIi = biphasedGrCalculator.getmIi();
                    }

                    res =  buildUnbalancedResult(mId, mIo, mIi, rdf, xdf, rof, xof, mf,
                            directResult, homopolarResult,
                            scf, lfBus1, v1dxInit, v1dyInit, directResolution);

                } else if (shortCircuitType == ShortCircuitFault.ShortCircuitType.BIPHASED_COMMON_SUPPORT) {
                    // TODO : We only handle the first biphased of the list for now, check how to handle this in the final version
                    AdmittanceLinearResolution.AdmittanceLinearResolutionResult.AdmittanceLinearResolutionResultBiphased biphasedDirectResult = directResult.getBiphasedResultsAtBus().get(0);
                    AdmittanceLinearResolution.AdmittanceLinearResolutionResult.AdmittanceLinearResolutionResultBiphased biphasedHomopolarResult = homopolarResult.getBiphasedResultsAtBus().get(0);

                    double ro12 = biphasedHomopolarResult.getZ12txx(); // TODO : add some tests to check consistency with Z12tyy and Z12tyx
                    double xo12 = -biphasedHomopolarResult.getZ12txy();
                    double ro22 = biphasedHomopolarResult.getZ22txx();
                    double xo22 = -biphasedHomopolarResult.getZ22txy();
                    double ro21 = biphasedHomopolarResult.getZ21txx();
                    double xo21 = -biphasedHomopolarResult.getZ21txy();

                    double rd12 = biphasedDirectResult.getZ12txx();
                    double xd12 = -biphasedDirectResult.getZ12txy();
                    double rd22 = biphasedDirectResult.getZ22txx();
                    double xd22 = -biphasedDirectResult.getZ22txy();
                    double rd21 = biphasedDirectResult.getZ21txx();
                    double xd21 = -biphasedDirectResult.getZ21txy();

                    BiphasedCommonSupportShortCircuitCalculator biphasedCommonCalculator;
                    if (scf.getBiphasedType() == ShortCircuitFault.ShortCircuitBiphasedType.C1_A2) {
                        biphasedCommonCalculator = new BiphasedC1A2Calculator(rdf, xdf, rof, xof, rf, xf, v1dxInit, v1dyInit, mf,
                                biphasedDirectResult.getV2x(), biphasedDirectResult.getV2y(),
                                ro12, xo12, ro22, xo22, ro21, xo21,
                                rd12, xd12, rd22, xd22, rd21, xd21);
                    } else if (scf.getBiphasedType() == ShortCircuitFault.ShortCircuitBiphasedType.C1_B2) {
                        biphasedCommonCalculator = new BiphasedC1B2Calculator(rdf, xdf, rof, xof, rf, xf, v1dxInit, v1dyInit, mf,
                                biphasedDirectResult.getV2x(), biphasedDirectResult.getV2y(),
                                ro12, xo12, ro22, xo22, ro21, xo21,
                                rd12, xd12, rd22, xd22, rd21, xd21);
                    } else if (scf.getBiphasedType() == ShortCircuitFault.ShortCircuitBiphasedType.C1_C2) {
                        biphasedCommonCalculator = new BiphasedC1C2Calculator(rdf, xdf, rof, xof, rf, xf, v1dxInit, v1dyInit, mf,
                                biphasedDirectResult.getV2x(), biphasedDirectResult.getV2y(),
                                ro12, xo12, ro22, xo22, ro21, xo21,
                                rd12, xd12, rd22, xd22, rd21, xd21);
                    } else {
                        throw new IllegalArgumentException(" short circuit fault of type : " + scf.getBiphasedType() + " not yet handled");
                    }

                    //biphasedCommonCalculator.computeCurrents();
                    mIo = biphasedCommonCalculator.getmIo();
                    mId = biphasedCommonCalculator.getmId();
                    mIi = biphasedCommonCalculator.getmIi();

                    // TODO : check if we need to make a separate function to handle biphased common support
                    Matrix mI2o = biphasedCommonCalculator.getmI2o();
                    Matrix mI2d = biphasedCommonCalculator.getmI2d();
                    Matrix mI2i = biphasedCommonCalculator.getmI2i();

                    //biphasedCommonCalculator.computeVoltages();
                    Matrix mdVo = biphasedCommonCalculator.getmVo(); // Contains variations of voltages, without Vinit
                    Matrix mdVd = biphasedCommonCalculator.getmVd(); // each voltage vector contains [V1x; V1y; V2x; V2y]
                    Matrix mdVi = biphasedCommonCalculator.getmVi();

                    LfBus lfBus2 = biphasedDirectResult.getBus2();

                    double v2dxInit = biphasedDirectResult.getV2x();
                    double v2dyInit = biphasedDirectResult.getV2y();

                    res =  buildUnbalancedCommunSuppportResult(mId, mIo, mIi, mI2d, mI2o, mI2i, mdVd, mdVo, mdVi, rdf, xdf, rof, xof, mf,
                            directResult, homopolarResult, scf,
                            lfBus1, v1dxInit, v1dyInit, directResolution,
                            lfBus2, v2dxInit, v2dyInit, biphasedDirectResult, biphasedHomopolarResult);

                } else {
                    throw new IllegalArgumentException(" Post-processing of short circuit type = " + shortCircuitType + "not yet implemented");
                }

                res.updateFeedersResult(); // feeders are updated only if voltageUpdate is made. TODO : see if update of homopolar feeders are to be updated
                resultsPerFault.put(scf, res);
            }

        }
    }

    public ShortCircuitResult buildUnbalancedResult(Matrix mId, Matrix mIo, Matrix mIi, double rdf, double xdf, double rof, double xof, MatrixFactory mf,
                                                    AdmittanceLinearResolution.AdmittanceLinearResolutionResult directResult,
                                                    AdmittanceLinearResolution.AdmittanceLinearResolutionResult homopolarResult,
                                                    ShortCircuitFault scf, LfBus lfBus1, double v1dxInit, double v1dyInit, AdmittanceLinearResolution directResolution) {
        //get the voltage vectors
        // Vo :
        // [vox]      [ rof  -xof ]   [ iox ]
        // [voy] = -  [ xof   rof ] * [ ioy ]

        Matrix zof = getZ(rof, xof, mf);
        Matrix zdf = getZ(rdf, xdf, mf);

        Matrix minusVo = zof.times(mIo);
        Matrix minusVd = zdf.times(mId);
        Matrix minusVi = zdf.times(mIi);

        //record the results
        ShortCircuitEquationSystemFeeders equationSystemFeedersDirect =  directResult.getEqSysFeeders();
        ShortCircuitEquationSystemFeeders equationSystemFeedersHomopolar =  homopolarResult.getEqSysFeeders();

        ShortCircuitResult res = new ShortCircuitResult(scf, lfBus1,
                mId.toDense().get(0, 0), mId.toDense().get(1, 0),
                mIo.toDense().get(0, 0), mIo.toDense().get(1, 0),
                mIi.toDense().get(0, 0), mIi.toDense().get(1, 0),
                rdf, xdf, rof, xof, rdf, xdf,
                v1dxInit, v1dyInit,
                -minusVd.toDense().get(0, 0), -minusVd.toDense().get(1, 0),
                -minusVo.toDense().get(0, 0), -minusVo.toDense().get(1, 0),
                -minusVi.toDense().get(0, 0), -minusVi.toDense().get(1, 0),
                mf, equationSystemFeedersDirect, equationSystemFeedersHomopolar, parameters.getNorm());

        if (parameters.voltageUpdate) {
            res.setLfNetwork(directResolution.lfNetworkResult);
            res.setTrueVoltageProfileUpdate();
            // The post-fault voltage values for the network busses are computed as follow :
            // [ Vof ] = -inv(Yo) * M * [ Iof ]
            // [ Vdf ] = -inv(Yd) * M * [ Idf ] + [ V(init) ]
            // [ Vif ] = -inv(Yd) * M * [ Iif ]
            // dMo = inv(Yo) * M
            // dMd = inv(Yd) * M

            int nbBusses = directResolution.lfNetworkResult.getBuses().size();
            res.createEmptyFortescueVoltageVector(nbBusses);

            for (Map.Entry<Integer, DenseMatrix> vd : directResult.getDv().entrySet()) {
                int busNum = vd.getKey();

                //direct
                double edVr = vd.getValue().get(0, 0);
                double edVi = vd.getValue().get(1, 0);

                double idr = -mId.toDense().get(0, 0);
                double idi = -mId.toDense().get(1, 0);

                double deltaVdr = -idr * edVr + idi * edVi;
                double deltaVdi = -idr * edVi - idi * edVr;

                //inverse
                double iir = -mIi.toDense().get(0, 0);
                double iii = -mIi.toDense().get(1, 0);

                double deltaVir = -iir * edVr + iii * edVi;
                double deltaVii = -iir * edVi - iii * edVr;

                //homopolar
                double eoVr = homopolarResult.getDv().get(busNum).get(0, 0);
                double eoVi = homopolarResult.getDv().get(busNum).get(1, 0);

                double ior = -mIo.toDense().get(0, 0);
                double ioi = -mIo.toDense().get(1, 0);

                double deltaVor = -ior * eoVr + ioi * eoVi;
                double deltaVoi = -ior * eoVi - ioi * eoVr;

                //System.out.println(" dVth(" + vdr.getKey() + ") = " + edVr + " + j(" + edVi + ")");

                res.fillVoltageInFortescueVector(busNum, deltaVdr, deltaVdi, deltaVor, deltaVoi, deltaVir, deltaVii);
            }
        }

        return res;
    }

    public ShortCircuitResult buildUnbalancedCommunSuppportResult(Matrix mId, Matrix mIo, Matrix mIi, Matrix mI2d, Matrix mI2o, Matrix mI2i, Matrix mVd, Matrix mVo, Matrix mVi, double rdf, double xdf, double rof, double xof, MatrixFactory mf,
                                                    AdmittanceLinearResolution.AdmittanceLinearResolutionResult directResult,
                                                    AdmittanceLinearResolution.AdmittanceLinearResolutionResult homopolarResult, ShortCircuitFault scf,
                                                                  LfBus lfBus1, double v1dxInit, double v1dyInit, AdmittanceLinearResolution directResolution,
                                                                  LfBus lfBus2, double v2dxInit, double v2dyInit,
                                                                  AdmittanceLinearResolution.AdmittanceLinearResolutionResult.AdmittanceLinearResolutionResultBiphased biphasedDirectResult,
                                                                  AdmittanceLinearResolution.AdmittanceLinearResolutionResult.AdmittanceLinearResolutionResultBiphased biphasedHomopolarResult) {

        //record the results
        ShortCircuitEquationSystemFeeders equationSystemFeedersDirect =  directResult.getEqSysFeeders();
        ShortCircuitEquationSystemFeeders equationSystemFeedersHomopolar =  homopolarResult.getEqSysFeeders();
        // TODO : adapt in case of a biphased common support

        ShortCircuitResult res = new ShortCircuitResult(scf, lfBus1,
                mId.toDense().get(0, 0), mId.toDense().get(1, 0),
                mIo.toDense().get(0, 0), mIo.toDense().get(1, 0),
                mIi.toDense().get(0, 0), mIi.toDense().get(1, 0),
                rdf, xdf, rof, xof, rdf, xdf,
                v1dxInit, v1dyInit,
                mVd.toDense().get(0, 0), mVd.toDense().get(1, 0),
                mVo.toDense().get(0, 0), mVo.toDense().get(1, 0),
                mVi.toDense().get(0, 0), mVi.toDense().get(1, 0),
                mf, equationSystemFeedersDirect, equationSystemFeedersHomopolar, parameters.getNorm(),
                mI2d.toDense().get(0, 0), mI2d.toDense().get(1, 0),
                mI2o.toDense().get(0, 0), mI2o.toDense().get(1, 0),
                mI2i.toDense().get(0, 0), mI2i.toDense().get(1, 0),
                v2dxInit, v2dyInit,
                mVd.toDense().get(2, 0), mVd.toDense().get(3, 0),
                mVo.toDense().get(2, 0), mVo.toDense().get(3, 0),
                mVi.toDense().get(2, 0), mVi.toDense().get(3, 0),
                lfBus2);

        if (parameters.voltageUpdate) {
            res.setLfNetwork(directResolution.lfNetworkResult);
            res.setTrueVoltageProfileUpdate();
            // The post-fault voltage values for the network busses are computed as follow :
            // [ Vof ] = -inv(Yo) * M * [ Iof ]
            // [ Vdf ] = -inv(Yd) * M * [ Idf ] + [ V(init) ]
            // [ Vif ] = -inv(Yd) * M * [ Iif ]
            // dMo = inv(Yo) * M
            // dMd = inv(Yd) * M

            int nbBusses = directResolution.lfNetworkResult.getBuses().size();
            res.createEmptyFortescueVoltageVector(nbBusses);

            for (Map.Entry<Integer, DenseMatrix> vd : directResult.getDv().entrySet()) {
                int busNum = vd.getKey();

                //int numBus2 = biphasedDirectResult.getNumBus2Fault();

                //direct
                double edVr = vd.getValue().get(0, 0);
                double edVi = vd.getValue().get(1, 0);
                double edV2r = biphasedDirectResult.getDv2().get(busNum).get(0, 0); // TODO : check : probably wrong, should be busNum
                double edV2i = biphasedDirectResult.getDv2().get(busNum).get(1, 0); // TODO : check order to access DV2 is OK

                double idr = -mId.toDense().get(0, 0);
                double idi = -mId.toDense().get(1, 0);
                double i2dr = -mI2d.toDense().get(0, 0);
                double i2di = -mI2d.toDense().get(1, 0);

                double deltaVdr = -idr * edVr + idi * edVi - i2dr * edV2r + i2di * edV2i;
                double deltaVdi = -idr * edVi - idi * edVr - i2dr * edV2i - i2di * edV2r;

                //inverse
                double iir = -mIi.toDense().get(0, 0);
                double iii = -mIi.toDense().get(1, 0);
                double i2ir = -mI2i.toDense().get(0, 0);
                double i2ii = -mI2i.toDense().get(1, 0);

                double deltaVir = -iir * edVr + iii * edVi - i2ir * edV2r - i2ii * edV2i;
                double deltaVii = -iir * edVi - iii * edVr - i2ir * edV2i - i2ii * edV2r;

                //homopolar
                double eoVr = homopolarResult.getDv().get(busNum).get(0, 0);
                double eoVi = homopolarResult.getDv().get(busNum).get(1, 0);
                double eoV2r = biphasedHomopolarResult.getDv2().get(busNum).get(0, 0);
                double eoV2i = biphasedHomopolarResult.getDv2().get(busNum).get(1, 0);

                double ior = -mIo.toDense().get(0, 0);
                double ioi = -mIo.toDense().get(1, 0);
                double i2or = -mI2o.toDense().get(0, 0);
                double i2oi = -mI2o.toDense().get(1, 0);

                double deltaVor = -ior * eoVr + ioi * eoVi - i2or * eoV2r + i2oi * eoV2i;
                double deltaVoi = -ior * eoVi - ioi * eoVr - i2or * eoV2i - i2oi * eoV2r;

                //System.out.println(" dVth(" + vdr.getKey() + ") = " + edVr + " + j(" + edVi + ")");

                res.fillVoltageInFortescueVector(busNum, deltaVdr, deltaVdi, deltaVor, deltaVoi, deltaVir, deltaVii);
            }
        }

        return res;
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
