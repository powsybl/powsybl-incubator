/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.openloadflow.network.LfBus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class EquationSystemBusFeedersResult {

    private static final double EPSILON = 0.000001;

    private double ixFeedersSum; //sum of currents coming from branches, which is also the sum of feeders'currents
    private double iyFeedersSum;

    private List<EquationSystemFeeder> feedersInputData; // input data of feeders at bus

    private List<EquationSystemResultFeeder> busFeedersResults; // output data of feeders at bus

    private LfBus feedersBus;

    public EquationSystemBusFeedersResult(List<EquationSystemFeeder> feeders, LfBus bus) {
        this.ixFeedersSum = 0.;
        this.iyFeedersSum = 0.;
        this.feedersInputData = feeders;
        this.feedersBus = bus;
        this.busFeedersResults = new ArrayList<>();

        // init on feeder results based on equation system feeders
        for (EquationSystemFeeder feeder : feedersInputData) {
            EquationSystemResultFeeder feederResult = new EquationSystemResultFeeder(feeder.getId(), feeder.getFeederType(), 0., 0., feeder.getG(), feeder.getB());
            busFeedersResults.add(feederResult);
        }
    }

    public void addIfeeders(double ix, double iy) {
        this.ixFeedersSum = ix + this.ixFeedersSum;
        this.iyFeedersSum = iy + this.iyFeedersSum;
    }

    public void updateContributions() {
        double bSum = 0.;
        double gSum = 0.;

        for (EquationSystemResultFeeder feeder : busFeedersResults) {
            bSum = bSum + feeder.getB();
            gSum = gSum + feeder.getG();
        }

        if (Math.abs(gSum) > EPSILON || Math.abs(bSum) > EPSILON) {
            double det = 1 / (gSum * gSum + bSum * bSum);
            for (EquationSystemResultFeeder feederResult : busFeedersResults) {
                double gk = feederResult.getG();
                double bk = feederResult.getB();
                double ixk = det * ((gk * gSum + bk * bSum) * ixFeedersSum + (gk * bSum - gSum * bk) * iyFeedersSum);
                double iyk = det * ((-gk * bSum + gSum * bk) * ixFeedersSum + (gk * gSum + bk * bSum) * iyFeedersSum);

                feederResult.updateIcontribution(ixk, iyk);
                feederResult.printContributions(feedersBus);
            }
        }
    }

    public List<EquationSystemResultFeeder> getBusFeedersResults() {
        return busFeedersResults;
    }
}
