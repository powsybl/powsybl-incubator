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
public class ShortCircuitNetworkMachineInfo {

    public static final String NAME = "ShortCircuit";

    public enum MachineType {
        SYNCHRONOUS_GEN,
        ASYNCHRONOUS_GEN,
        SYNCHRONOUS_MOTOR,
        ASYNCHRONOUS_MOTOR;
    }

    private double transXd;
    private double transRd;

    private double subTransXd;
    private double subTransRd;

    private double stepUpTfoX;
    private double stepUpTfoR;

    private boolean isGrounded;
    private double groundR;
    private double groundX;

    private double coeffRo; // coeff used to get Ro from Rd
    private double coeffXo; // coeff used to get Xo from Xd

    private MachineType machineType;

    public ShortCircuitNetworkMachineInfo(double transXd, MachineType machineType) {
        this.transXd = transXd;
        this.stepUpTfoX = 0.;
        this.machineType = machineType;
        this.transRd = 0.;
        this.subTransXd = 0.;
        this.subTransRd = 0.;
        this.isGrounded = false;
        this.groundR = 0.;
        this.groundX = 0.;
        this.stepUpTfoR = 0.;
        this.coeffRo = 1.;
        this.coeffXo = 1.;
    }

    public ShortCircuitNetworkMachineInfo(double transXd, double stepUpTfoX, MachineType machineType) {
        this(transXd, machineType);
        this.stepUpTfoX = stepUpTfoX;
    }

    public ShortCircuitNetworkMachineInfo(double transXd, double stepUpTfoX, MachineType machineType, boolean isGrounded) {
        this(transXd, stepUpTfoX, machineType);
        this.isGrounded = isGrounded;
    }

    public ShortCircuitNetworkMachineInfo(double transXd, double stepUpTfoX, MachineType machineType, boolean isGrounded, double transRd) {
        this(transXd, stepUpTfoX, machineType, isGrounded);
        this.transRd = transRd;
    }

    public ShortCircuitNetworkMachineInfo(double transXd, double stepUpTfoX, MachineType machineType, double transRd, double stepUpTfoR, double subTransRd, double subTransXd,
                               boolean isGrounded, double groundR, double groundX, double coeffRo, double coeffXo) {
        this(transXd, stepUpTfoX, machineType, isGrounded, transRd);
        this.subTransRd = subTransRd;
        this.groundR = groundR;
        this.groundX = groundX;
        this.stepUpTfoR = stepUpTfoR;
        this.subTransXd = subTransXd;
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
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
        return  isGrounded;
    }

    public double getCoeffRo() {
        return  coeffRo;
    }

    public double getCoeffXo() {
        return  coeffXo;
    }

    public void printInfos() {
        System.out.println(" transXd =" + transXd + " transRd="  + transRd);
        System.out.println(" subTransXd =" + subTransXd + " subtransRd="  + subTransRd);
        System.out.println(" Machine Type =" + machineType + " isGrounded="  + isGrounded);
    }
}
