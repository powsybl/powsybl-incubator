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
public class FeedersAtBusResult {

    private static final double EPSILON = 0.000001;

    private double ixFeedersSum; //sum of currents coming from branches, which is also the sum of feeders'currents at the same LfBus
    private double iyFeedersSum;

    private List<Feeder> feedersInputData; // input data of feeders at bus

    private List<FeederResult> busFeedersResult; // output data of feeders at bus

    private LfBus feedersBus;

    private FeedersAtBus feederAtBus;

    public FeedersAtBusResult(FeedersAtBus feederAtBus) {
        this.ixFeedersSum = 0.;
        this.iyFeedersSum = 0.;
        this.feederAtBus = feederAtBus;

        // init on feeder results based on equation system feeders
        for (Feeder feeder : feedersInputData) {
            FeederResult feederResult = new FeederResult(feeder, 0., 0.);
            busFeedersResult.add(feederResult);
        }
    }

    public FeedersAtBusResult(List<Feeder> feeders, LfBus bus) {
        this.ixFeedersSum = 0.;
        this.iyFeedersSum = 0.;
        this.feedersInputData = feeders;
        this.feedersBus = bus;
        this.busFeedersResult = new ArrayList<>();

        // init on feeder results based on equation system feeders
        for (Feeder feeder : feedersInputData) {
            FeederResult feederResult = new FeederResult(feeder, 0., 0.);
            busFeedersResult.add(feederResult);
        }
    }

    public void addIfeeders(double ix, double iy) {
        this.ixFeedersSum = ix + this.ixFeedersSum;
        this.iyFeedersSum = iy + this.iyFeedersSum;
    }

    public void updateContributions() {
        double bSum = 0.;
        double gSum = 0.;

        for (FeederResult feederResult : busFeedersResult) {
            bSum = bSum + feederResult.getFeeder().getB();
            gSum = gSum + feederResult.getFeeder().getG();
        }

        if (Math.abs(gSum) > EPSILON || Math.abs(bSum) > EPSILON) {
            double det = 1 / (gSum * gSum + bSum * bSum);
            for (FeederResult feederResult : busFeedersResult) {
                double gk = feederResult.getFeeder().getG();
                double bk = feederResult.getFeeder().getB();
                double ixk = det * ((gk * gSum + bk * bSum) * ixFeedersSum + (gk * bSum - gSum * bk) * iyFeedersSum);
                double iyk = det * ((-gk * bSum + gSum * bk) * ixFeedersSum + (gk * gSum + bk * bSum) * iyFeedersSum);

                feederResult.updateIcontribution(ixk, iyk);
                feederResult.printContributions(feedersBus);
            }
        }
    }

    public List<FeederResult> getBusFeedersResult() {
        return busFeedersResult;
    }
}
