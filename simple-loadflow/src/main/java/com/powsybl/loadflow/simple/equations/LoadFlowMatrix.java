/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.equations;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PrimitiveDenseStore;
import org.ojalgo.matrix.store.SparseStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

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

    public static SparseStore<Double> build(IndexedNetwork network) {
        SparseStore<Double> a = SparseStore.PRIMITIVE.make(2L * network.getBusCount(), 2L * network.getBusCount());

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

            a.add(rowP1, colPh1, eq.dp1dph1());
            a.add(rowP1, colPh2, eq.dp1dph2());
            a.add(rowP1, colV1, eq.dp1dv1());
            a.add(rowP1, colV2, eq.dp1dv2());

            if (!busGenerators.contains(bus1)) {
                a.add(rowQ1, colPh1, eq.dq1dph1());
                a.add(rowQ1, colPh2, eq.dq1dph2());
                a.add(rowQ1, colV1, eq.dq1dv1());
                a.add(rowQ1, colV2, eq.dq1dv2());
            }

            a.add(rowP2, colPh1, eq.dp2dph1());
            a.add(rowP2, colPh2, eq.dp2dph2());
            a.add(rowP2, colV1, eq.dp2dv1());
            a.add(rowP2, colV2, eq.dp2dv2());

            if (!busGenerators.contains(bus2)) {
                a.add(rowQ2, colPh1, eq.dq2dph1());
                a.add(rowQ2, colPh2, eq.dq2dph2());
                a.add(rowQ2, colV1, eq.dq2dv1());
                a.add(rowQ2, colV2, eq.dq2dv2());
            }
        }

        return a;
    }

    public static SparseStore<Double> buildDc(IndexedNetwork network) {
        SparseStore<Double> a = SparseStore.PRIMITIVE.make(network.getBusCount(), network.getBusCount());

        for (Branch branch : network.get().getBranches()) {

            Bus bus1 = getBus1(branch);
            Bus bus2 = getBus2(branch);

            if (bus1 == null || bus2 == null) {
                continue;
            }

            DcFlowEquationsImpl eq = new DcFlowEquationsImpl(branch);

            int num1 = network.getIndex(bus1);
            int num2 = network.getIndex(bus2);

            int rowP1 = num1;
            int rowP2 = num2;
            int colPh1 = num1;
            int colPh2 = num2;

            if (rowP1 != 0) {
                a.add(rowP1, colPh1, eq.dp1dph1());
                a.add(rowP1, colPh2, eq.dp1dph2());
            }
            if (rowP2 != 0) {
                a.add(rowP2, colPh1, eq.dp2dph1());
                a.add(rowP2, colPh2, eq.dp2dph2());
            }
        }

        a.set(0, 0, 1);

        return a;
    }

    public static PrimitiveDenseStore buildDcRhs(IndexedNetwork network) {
        PrimitiveDenseStore rhs = PrimitiveDenseStore.FACTORY.makeZero(network.getBusCount(), 1);

        for (Generator gen : network.get().getGenerators()) {
            Bus bus = gen.getTerminal().getBusView().getBus();
            int num = network.getIndex(bus);
            if (num == 0) {
                continue;
            }
            rhs.set(num, 0, gen.getTargetP());
        }

        for (Load load : network.get().getLoads()) {
            Bus bus = load.getTerminal().getBusView().getBus();
            int num = network.getIndex(bus);
            if (num == 0) {
                continue;
            }
            rhs.set(num, 0, load.getP0());
        }

        return rhs;
    }

    public static void updateNetwork(IndexedNetwork network, MatrixStore<Double> lhs) {
        for (Bus bus : network.getBuses()) {
            int num = network.getIndex(bus);
            bus.setAngle(lhs.get(num, 0));
        }

        for (Branch branch : network.get().getBranches()) {
            DcFlowEquations eq = DcFlowEquations.of(branch);
            branch.getTerminal1().setP(eq.p1());
            branch.getTerminal2().setP(eq.p2());
        }
    }
}
