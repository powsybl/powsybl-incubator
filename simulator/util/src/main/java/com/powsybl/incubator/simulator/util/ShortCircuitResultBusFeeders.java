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
public class ShortCircuitResultBusFeeders {

    private double ixFeedersSum; //sum of currents coming from branches, which is also the sum of feeders'currents
    private double iyFeedersSum;

    private List<ShortCircuitEquationSystemFeeder> feedersInputData; // input data of feeders at bus

    private List<ShortCircuitResultFeeder> busFeedersResults; // output data of feeders at bus

    private LfBus feedersBus;

    public ShortCircuitResultBusFeeders(List<ShortCircuitEquationSystemFeeder> feeders, LfBus bus) {
        this.ixFeedersSum = 0.;
        this.iyFeedersSum = 0.;
        this.feedersInputData = feeders;
        this.feedersBus = bus;
        this.busFeedersResults = new ArrayList<>();

        // init on feeder results based on equation system feeders
        for (ShortCircuitEquationSystemFeeder feeder : feedersInputData) {
            ShortCircuitResultFeeder feederResult = new ShortCircuitResultFeeder(feeder.getId(), feeder.getFeederType(), 0., 0., feeder.getG(), feeder.getB());
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

        for (ShortCircuitResultFeeder feeder : busFeedersResults) {
            bSum = bSum + feeder.getB();
            gSum = gSum + feeder.getG();
        }

        for (ShortCircuitResultFeeder feederResult : busFeedersResults) {
            double epsilon = 0.000001;
            if (Math.abs(gSum) > epsilon || Math.abs(bSum) > epsilon) {
                double det = 1 / (gSum * gSum + bSum * bSum);
                double gk = feederResult.getG();
                double bk = feederResult.getB();
                double ixk = det * ((gk * gSum + bk * bSum) * ixFeedersSum + (gk * bSum - gSum * bk) * iyFeedersSum);
                double iyk = det * ((-gk * bSum + gSum * bk) * ixFeedersSum + (gk * gSum + bk * bSum) * iyFeedersSum);

                feederResult.updateIcontribution(ixk, iyk);
                //ShortCircuitResultFeeder feederResult = new ShortCircuitResultFeeder(feeder.getId(), feeder.getFeederType(), ixk, iyk);
                feederResult.printContributions(feedersBus);
                //busFeedersResults.add(feederResult);
            }
        }
    }

    List<ShortCircuitResultFeeder> getBusFeedersResults() {
        return busFeedersResults;
    }
}
