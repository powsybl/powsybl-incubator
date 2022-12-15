/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions;

/**
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ScGenerator {

    public enum MachineType {
        SYNCHRONOUS_GEN,
        ASYNCHRONOUS_GEN,
        SYNCHRONOUS_MOTOR,
        ASYNCHRONOUS_MOTOR;
    }

    private final double transXd;
    private final double transRd;

    private final double subTransXd;
    private final double subTransRd;

    private final double stepUpTfoX;
    private final double stepUpTfoR;

    private final boolean grounded;
    private final double groundR;
    private final double groundX;

    private final double ro;
    private final double xo;

    private final MachineType machineType;

    public ScGenerator(double transXd, double stepUpTfoX, MachineType machineType, double transRd, double stepUpTfoR, double subTransRd, double subTransXd,
                       boolean grounded, double groundR, double groundX, double ro, double xo) {
        this.transXd = transXd;
        this.stepUpTfoX = stepUpTfoX;
        this.machineType = machineType;
        this.transRd = transRd;
        this.stepUpTfoR = stepUpTfoR;
        this.subTransRd = subTransRd;
        this.subTransXd = subTransXd;
        this.grounded = grounded;
        this.groundR = groundR;
        this.groundX = groundX;
        this.ro = ro;
        this.xo = xo;
    }

    public double getTransXd() {
        return transXd;
    }

    public double getSubTransXd() {
        return subTransXd;
    }

    public double getStepUpTfoX() {
        return  stepUpTfoX;
    }

    public double getTransRd() {
        return transRd;
    }

    public double getSubTransRd() {
        return subTransRd;
    }

    public double getStepUpTfoR() {
        return  stepUpTfoR;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public double getGroundR() {
        return groundR;
    }

    public double getGroundX() {
        return groundX;
    }

    public double getRo() {
        return  ro;
    }

    public double getXo() {
        return  xo;
    }

}
