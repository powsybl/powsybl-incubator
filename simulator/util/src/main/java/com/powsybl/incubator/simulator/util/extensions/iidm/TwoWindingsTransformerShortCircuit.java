/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class TwoWindingsTransformerShortCircuit extends AbstractExtension<TwoWindingsTransformer> {

    public static final String NAME = "twoWindingsTransformerShortCircuit";

    private double coeffRo; // Ro = Rd * CoeffRo
    private double coeffXo;
    private double kNorm; // coef used by the chosen norm to modify R, X, Ro and Xo
    private final boolean freeFluxes; // free fluxes mean that magnetizing impedance Zm is infinite, by default, fluxes are forced and Zm exists
    private LegConnectionType leg1ConnectionType;
    private LegConnectionType leg2ConnectionType;
    private boolean isPartOfGeneratingUnit;
    private double r1Ground;
    private double x1Ground;
    private double r2Ground;
    private double x2Ground;

    @Override
    public String getName() {
        return NAME;
    }

    public TwoWindingsTransformerShortCircuit(TwoWindingsTransformer twt, boolean isPartOfGeneratingUnit, double coeffRo, double coeffXo, double kNorm, boolean freeFluxes,
                                              LegConnectionType leg1ConnectionType, LegConnectionType leg2ConnectionType, double r1Ground, double x1Ground, double r2Ground, double x2Ground) {
        super(twt);
        this.isPartOfGeneratingUnit = isPartOfGeneratingUnit;
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
        this.kNorm = kNorm;
        this.freeFluxes = freeFluxes;
        this.leg1ConnectionType = Objects.requireNonNull(leg1ConnectionType);
        this.leg2ConnectionType = Objects.requireNonNull(leg2ConnectionType);
        this.r1Ground = r1Ground;
        this.x1Ground = x1Ground;
        this.r2Ground = r2Ground;
        this.x2Ground = x2Ground;
    }

    public double getCoeffRo() {
        return coeffRo;
    }

    public double getCoeffXo() {
        return coeffXo;
    }

    public boolean isFreeFluxes() {
        return freeFluxes;
    }

    public LegConnectionType getLeg1ConnectionType() {
        return leg1ConnectionType;
    }

    public LegConnectionType getLeg2ConnectionType() {
        return leg2ConnectionType;
    }

    public void setPartOfGeneratingUnit(boolean partOfGeneratingUnit) {
        isPartOfGeneratingUnit = partOfGeneratingUnit;
    }

    public boolean isPartOfGeneratingUnit() {
        return isPartOfGeneratingUnit;
    }

    public void setCoeffXo(double coeffXo) {
        this.coeffXo = coeffXo;
    }

    public void setLeg2ConnectionType(LegConnectionType leg2ConnectionType) {
        this.leg2ConnectionType = leg2ConnectionType;
    }

    public void setLeg1ConnectionType(LegConnectionType leg1ConnectionType) {
        this.leg1ConnectionType = leg1ConnectionType;
    }

    public void setCoeffRo(double coeffRo) {
        this.coeffRo = coeffRo;
    }

    public double getR1Ground() {
        return r1Ground;
    }

    public double getR2Ground() {
        return r2Ground;
    }

    public double getX1Ground() {
        return x1Ground;
    }

    public double getX2Ground() {
        return x2Ground;
    }

    public void setR1Ground(double r1Ground) {
        this.r1Ground = r1Ground;
    }

    public void setR2Ground(double r2Ground) {
        this.r2Ground = r2Ground;
    }

    public void setX1Ground(double x1Ground) {
        this.x1Ground = x1Ground;
    }

    public void setX2Ground(double x2Ground) {
        this.x2Ground = x2Ground;
    }

    public double getkNorm() {
        return kNorm;
    }

    public void setkNorm(double kNorm) {
        this.kNorm = kNorm;
    }
}
