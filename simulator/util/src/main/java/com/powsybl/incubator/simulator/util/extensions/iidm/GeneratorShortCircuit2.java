/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Generator;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class GeneratorShortCircuit2 extends AbstractExtension<Generator> {

    public static final String NAME = "generatorShortCircuit2";

    public enum RotorType {
        UNDEFINED,
        TURBOSERIES_1;
    }

    public enum GeneratorType {
        UNKNOWN,
        ROTATING_MACHINE,
        FEEDER;
    }

    private double transRd; // transient resistance // TODO : will be final when kG of the norm will be handled
    private double subTransRd; // sub-transient resistance // TODO : will be final when kG of the norm will be handled
    private boolean toGround;
    private double coeffRo;
    private double coeffXo;
    private final double coeffRi;
    private final double coeffXi;
    private final double groundingR;
    private final double groundingX;
    private final RotorType rotorType;
    private final double ratedU;
    private final double cosPhi;
    private final GeneratorType generatorType;
    private final double cq;
    private final double maxR1ToX1Ratio;
    private final double ikQmax;
    private final double voltageRegulationRange;

    @Override
    public String getName() {
        return NAME;
    }

    public GeneratorShortCircuit2(Generator generator, double transRd, double subTransRd, boolean toGround, double coeffRo, double coeffXo, double coeffRi, double coeffXi, double groundingR, double groundingX, RotorType rotorType, double ratedU, double cosPhi,
                                  GeneratorType generatorType, double cq, double ikQmax, double maxR1ToX1Ratio, double voltageRegulationRange) {
        super(generator);
        this.transRd = transRd;
        this.subTransRd = subTransRd;
        this.toGround = toGround;
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
        this.coeffRi = coeffRi;
        this.coeffXi = coeffXi;
        this.groundingR = groundingR;
        this.groundingX = groundingX;
        this.rotorType = rotorType;
        this.ratedU = ratedU;
        this.cosPhi = cosPhi;
        this.generatorType = generatorType;
        this.cq = cq;
        this.ikQmax = ikQmax;
        this.maxR1ToX1Ratio = maxR1ToX1Ratio;
        this.voltageRegulationRange = voltageRegulationRange;
    }

    public double getTransRd() {
        return transRd;
    }

    public double getSubTransRd() {
        return subTransRd;
    }

    public boolean isToGround() {
        return toGround;
    }

    public double getCoeffRo() {
        return coeffRo;
    }

    public double getCoeffXo() {
        return coeffXo;
    }

    public double getGroundingR() {
        return groundingR;
    }

    public double getGroundingX() {
        return groundingX;
    }

    public double getCoeffRi() {
        return coeffRi;
    }

    public double getCoeffXi() {
        return coeffXi;
    }

    public RotorType getRotorType() {
        return rotorType;
    }

    public double getCosPhi() {
        return cosPhi;
    }

    public double getRatedU() {
        return ratedU;
    }

    public void setSubTransRd(double subTransRd) {
        this.subTransRd = subTransRd;
    }

    public void setTransRd(double transRd) {
        this.transRd = transRd;
    }

    public GeneratorType getGeneratorType() {
        return generatorType;
    }

    public double getCq() {
        return cq;
    }

    public double getIkQmax() {
        return ikQmax;
    }

    public double getMaxR1ToX1Ratio() {
        return maxR1ToX1Ratio;
    }

    public double getVoltageRegulationRange() {
        return voltageRegulationRange;
    }

    public void setCoeffRo(double coeffRo) {
        this.coeffRo = coeffRo;
    }

    public void setCoeffXo(double coeffXo) {
        this.coeffXo = coeffXo;
    }

    public void setToGround(boolean toGround) {
        this.toGround = toGround;
    }
}
