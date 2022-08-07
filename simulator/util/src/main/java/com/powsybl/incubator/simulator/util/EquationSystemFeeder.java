/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class EquationSystemFeeder {

    //Feeder class is used to post process the results of a short circuit computation to get the feeder contribution in short-circuit current
    public EquationSystemFeeder(double b, double g, String id, EquationSystemFeeder.FeederType feederType) {

        this.b = b;
        this.g = g;
        this.id = id;
        this.feederType = feederType;

    }

    public enum FeederType {
        GENERATOR,
        SHUNT,
        CONTROLLED_SHUNT,
        LOAD;
    }

    private double b;

    private double g;

    private String id; // id in LfNetwork

    private EquationSystemFeeder.FeederType feederType;

    public double getB() {
        return b;
    }

    public double getG() {
        return g;
    }

    public String getId() {
        return id;
    }

    public EquationSystemFeeder.FeederType getFeederType() {
        return feederType;
    }

}
