/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.google.common.base.Stopwatch;
import com.powsybl.iidm.network.*;
import com.powsybl.math.matrix.DenseMatrix;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.math.matrix.Matrix;
import com.powsybl.math.matrix.MatrixFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public final class LoadFlowMatrix {

    private LoadFlowMatrix() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowMatrix.class);

    private static Bus getBus(Branch b, Branch.Side s) {
        return b.getTerminal(s).getBusView().getBus();
    }

    private static Bus getBus1(Branch b) {
        return getBus(b, Branch.Side.ONE);
    }

    private static Bus getBus2(Branch b) {
        return getBus(b, Branch.Side.TWO);
    }

    public static Matrix build(IndexedNetwork network) {
        DenseMatrix a = new DenseMatrixFactory().create(2 * network.getBusCount(), 2 * network.getBusCount(), 1);

        Set<Bus> busGenerators = new HashSet<>();
        for (Generator generator : network.get().getGenerators()) {
            Bus bus = generator.getTerminal().getBusView().getBus();
            busGenerators.add(bus);
            int num = network.getIndex(bus);
            int rowV = 2 * num + 1; //index of reactive power on that node
        }

        for (Branch branch : network.get().getBranches()) {
            FlowEquations eq = new FlowEquations(branch);

            LOGGER.info("{}", eq);

            Bus bus1 = getBus1(branch);
            Bus bus2 = getBus2(branch);
            int num1 = network.getIndex(bus1);
            int num2 = network.getIndex(bus2);

            int rowP1 = 2 * num1;
            int rowQ1 = 2 * num1 + 1;
            int rowP2 = 2 * num2;
            int rowQ2 = 2 * num2 + 1;
            int colPh1 = 2 * num1;
            int colV1 = 2 * num1 + 1;
            int colPh2 = 2 * num2;
            int colV2 = 2 * num2 + 1;

            a.addValue(rowP1, colPh1, eq.dp1dph1());
            a.addValue(rowP1, colPh2, eq.dp1dph2());
            a.addValue(rowP1, colV1, eq.dp1dv1());
            a.addValue(rowP1, colV2, eq.dp1dv2());

            if (!busGenerators.contains(bus1)) {
                a.addValue(rowQ1, colPh1, eq.dq1dph1());
                a.addValue(rowQ1, colPh2, eq.dq1dph2());
                a.addValue(rowQ1, colV1, eq.dq1dv1());
                a.addValue(rowQ1, colV2, eq.dq1dv2());
            }

            a.addValue(rowP2, colPh1, eq.dp2dph1());
            a.addValue(rowP2, colPh2, eq.dp2dph2());
            a.addValue(rowP2, colV1, eq.dp2dv1());
            a.addValue(rowP2, colV2, eq.dp2dv2());

            if (!busGenerators.contains(bus2)) {
                a.addValue(rowQ2, colPh1, eq.dq2dph1());
                a.addValue(rowQ2, colPh2, eq.dq2dph2());
                a.addValue(rowQ2, colV1, eq.dq2dv1());
                a.addValue(rowQ2, colV2, eq.dq2dv2());
            }
        }

        return a;
    }

    public static Matrix buildDc(IndexedNetwork network, int slackBusNum, MatrixFactory matrixFactory, double[] rhs) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(matrixFactory);
        Objects.requireNonNull(rhs);

        Stopwatch stopwatch = Stopwatch.createStarted();

        Matrix a = matrixFactory.create(network.getBusCount(), network.getBusCount(), network.getBusCount() * 3);

        network.forEachBranchInCscOrder(new IndexedNetwork.BranchHandler() {

            private boolean slackBusAdded = false;

            @Override
            public void onBranch(int row, int column, Branch branch, Branch.Side side) {
                if (!slackBusAdded) {
                    if (column >= slackBusNum) {
                        a.setValue(slackBusNum, slackBusNum, 1);
                        slackBusAdded = true;
                    }
                }
                if (row != slackBusNum && column != slackBusNum) {
                    ClosedBranchDcFlowEquations eq = new ClosedBranchDcFlowEquations(branch);
                    if (row == column) {
                        if (side == Branch.Side.ONE) {
                            a.addValue(row, column, eq.dp1dph1());
                            rhs[row] -= eq.rhs1();
                        } else {
                            a.addValue(row, column, eq.dp2dph2());
                            rhs[row] -= eq.rhs2();
                        }
                    } else {
                        if (side == Branch.Side.ONE) {
                            a.addValue(row, column, eq.dp1dph2());
                        } else {
                            a.addValue(row, column, eq.dp2dph1());
                        }
                    }
                }
            }
        });

        stopwatch.stop();
        LOGGER.info("DC matrix built in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return a;
    }

    public static double[] buildDcRhs(IndexedNetwork network, int slackBusNum) {
        double[] rhs = new double[network.getBusCount()];

        for (Generator gen : network.get().getGenerators()) {
            Bus bus = gen.getTerminal().getBusView().getBus();
            if (bus == null || !bus.isInMainConnectedComponent()) {
                continue;
            }
            int num = network.getIndex(bus);
            if (num == slackBusNum) {
                continue;
            }
            rhs[num] -= gen.getTargetP();
        }

        for (Load load : network.get().getLoads()) {
            Bus bus = load.getTerminal().getBusView().getBus();
            if (bus == null || !bus.isInMainConnectedComponent()) {
                continue;
            }
            int num = network.getIndex(bus);
            if (num == slackBusNum) {
                continue;
            }
            rhs[num] += load.getP0();
        }

        for (HvdcLine line : network.get().getHvdcLines()) {
            Bus bus1 = line.getConverterStation1().getTerminal().getBusView().getBus();
            Bus bus2 = line.getConverterStation2().getTerminal().getBusView().getBus();
            double p = line.getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER
                    ? line.getActivePowerSetpoint()
                    : -line.getActivePowerSetpoint();
            if (bus1 != null && bus1.isInMainConnectedComponent()) {
                int num = network.getIndex(bus1);
                if (num != slackBusNum) {
                    rhs[num] += p;
                }
            }
            if (bus2 != null && bus2.isInMainConnectedComponent()) {
                int num = network.getIndex(bus2);
                if (num != slackBusNum) {
                    rhs[num] -= p;
                }
            }
        }

        return rhs;
    }

    public static void updateDcNetwork(IndexedNetwork network, double[] lhs) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        for (Bus bus : network.get().getBusView().getBuses()) {
            bus.setV(Double.NaN);
            bus.setAngle(Double.NaN);
        }

        for (Bus bus : network.getBuses()) {
            int num = network.getIndex(bus);
            bus.setV(bus.getVoltageLevel().getNominalV());
            bus.setAngle(Math.toDegrees(lhs[num]));
        }

        for (Branch branch : network.get().getBranches()) {
            DcFlowEquations eq = DcFlowEquations.of(branch);
            branch.getTerminal1().setP(eq.p1());
            branch.getTerminal1().setQ(Double.NaN);
            branch.getTerminal2().setP(eq.p2());
            branch.getTerminal2().setQ(Double.NaN);
        }

        stopwatch.stop();
        LOGGER.info("Network updated with DC result in {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
