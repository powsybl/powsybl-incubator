/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LfBranchImpl implements LfBranch {

    private final Branch branch;

    private final LfBus bus1;

    private final LfBus bus2;

    private double r;
    private double x;
    private double z;
    private double y;
    private double ksi;
    private double g1;
    private double g2;
    private double b1;
    private double b2;
    private double r1;
    private double r2;
    private double a1;
    private double a2;

    public LfBranchImpl(Branch branch, LfBus bus1, LfBus bus2) {
        this.branch = Objects.requireNonNull(branch);
        this.bus1 = bus1;
        this.bus2 = bus2;
        init();
    }

    private void init() {

        r2 = 1d;
        a2 = 0d;

        if (branch instanceof Line) {
            initLine((Line) branch);
        } else if (branch instanceof TwoWindingsTransformer) {
            initTransformer((TwoWindingsTransformer) branch);
        } else {
            throw new PowsyblException("Unsupported type of branch for flow equations for branch: " + branch.getId());
        }

        z = Math.hypot(r, x);
        y = 1 / z;
        ksi = Math.atan2(r, x);
    }

    private void initLine(Line line) {
        r = line.getR();
        x = line.getX();
        g1 = line.getG1();
        g2 = line.getG2();
        b1 = line.getB1();
        b2 = line.getB2();
        r1 = 1d;
        a1 = 0d;
    }

    private void initTransformer(TwoWindingsTransformer tf) {
        r = Transformers.getR(tf);
        x = Transformers.getX(tf);
        g1 = Transformers.getG1(tf);
        g2 = 0d;
        b1 = Transformers.getB1(tf);
        b2 = 0d;
        r1 = Transformers.getRatio(tf);
        a1 = Transformers.getAngle(tf);
    }

    @Override
    public String getId() {
        return branch.getId();
    }

    @Override
    public LfBus getBus1() {
        return bus1;
    }

    @Override
    public LfBus getBus2() {
        return bus2;
    }

    @Override
    public void setP1(double p1) {
        branch.getTerminal1().setP(p1);
    }

    @Override
    public void setP2(double p2) {
        branch.getTerminal2().setP(p2);
    }

    @Override
    public void setQ1(double q1) {
        branch.getTerminal1().setQ(q1);
    }

    @Override
    public void setQ2(double q2) {
        branch.getTerminal2().setQ(q2);
    }

    @Override
    public double r() {
        return r;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double z() {
        return z;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double ksi() {
        return ksi;
    }

    @Override
    public double g1() {
        return g1;
    }

    @Override
    public double g2() {
        return g2;
    }

    @Override
    public double b1() {
        return b1;
    }

    @Override
    public double b2() {
        return b2;
    }

    @Override
    public double r1() {
        return r1;
    }

    @Override
    public double r2() {
        return r2;
    }

    @Override
    public double a1() {
        return a1;
    }

    @Override
    public double a2() {
        return a2;
    }
}
