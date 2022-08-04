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
public class EquationSystemResultFeeder {

    private double b;

    private double g;

    private String id; // id in LfNetwork

    private EquationSystemFeeder.FeederType feederType;

    private double ixContribution;

    private double iyContribution;

    public EquationSystemResultFeeder(String id, EquationSystemFeeder.FeederType feederType, double ix, double iy, double g, double b) {
        this.id = id;
        this.feederType = feederType;
        this.ixContribution = ix;
        this.iyContribution = iy;
        this.g = g;
        this.b = b;
    }

    public EquationSystemFeeder.FeederType getFeederType() {
        return feederType;
    }

    public void updateIcontribution(double ix, double iy) {
        ixContribution = ixContribution + ix;
        iyContribution = iyContribution + iy;
    }

    public void printContributions(LfBus bus) {
        System.out.println(" ix(" + id + ", " + feederType + ") = " + ixContribution + " + j(" + iyContribution + ")  Module I = "
                + 1000. * 100. / bus.getNominalV() * Math.sqrt((ixContribution * ixContribution + iyContribution * iyContribution) / 3.)); //TODO : issue with a 3x factor
    }

    double getB() {
        return b;
    }

    double getG() {
        return g;
    }

    String getId() {
        return id;
    }

    double getIxContribution() {
        return ixContribution;
    }
}
