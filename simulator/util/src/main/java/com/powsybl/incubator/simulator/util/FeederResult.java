/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

import com.powsybl.openloadflow.network.LfBus;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class FeederResult {

    private Feeder feeder;

    private double ixContribution;
    private double iyContribution;

    public FeederResult(Feeder feeder, double ix, double iy) {
        this.feeder = feeder;
        this.ixContribution = ix;
        this.iyContribution = iy;
    }

    public void updateIcontribution(double ix, double iy) {
        ixContribution = ixContribution + ix;
        iyContribution = iyContribution + iy;
    }

    public void printContributions(LfBus bus) {
        System.out.println(" ix(" + feeder.getId() + ", " + feeder.getFeederType() + ") = " + ixContribution + " + j(" + iyContribution + ")  Module I = "
                + 1000. * 100. / bus.getNominalV() * Math.sqrt((ixContribution * ixContribution + iyContribution * iyContribution) / 3.)); //TODO : issue with a 3x factor
    }

    public double getIxContribution() {
        return ixContribution;
    }

    public Feeder getFeeder() {
        return feeder;
    }
}
