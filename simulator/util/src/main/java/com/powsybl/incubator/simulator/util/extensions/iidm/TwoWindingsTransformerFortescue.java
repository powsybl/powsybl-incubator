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
public class TwoWindingsTransformerFortescue extends AbstractExtension<TwoWindingsTransformer> {

    public static final String NAME = "twoWindingsTransformerShortCircuit";

    private double ro; // Ro = Rd * CoeffRo
    private double xo;
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

    public TwoWindingsTransformerFortescue(TwoWindingsTransformer twt, boolean isPartOfGeneratingUnit, double ro, double xo, boolean freeFluxes,
                                           LegConnectionType leg1ConnectionType, LegConnectionType leg2ConnectionType, double r1Ground, double x1Ground, double r2Ground, double x2Ground) {
        super(twt);
        this.isPartOfGeneratingUnit = isPartOfGeneratingUnit;
        this.ro = ro;
        this.xo = xo;
        this.freeFluxes = freeFluxes;
        this.leg1ConnectionType = Objects.requireNonNull(leg1ConnectionType);
        this.leg2ConnectionType = Objects.requireNonNull(leg2ConnectionType);
        this.r1Ground = r1Ground;
        this.x1Ground = x1Ground;
        this.r2Ground = r2Ground;
        this.x2Ground = x2Ground;
    }

    public double getRo() {
        return ro;
    }

    public double getXo() {
        return xo;
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

    public void setXo(double xo) {
        this.xo = xo;
    }

    public void setRo(double ro) {
        this.ro = ro;
    }

    public void setLeg2ConnectionType(LegConnectionType leg2ConnectionType) {
        this.leg2ConnectionType = leg2ConnectionType;
    }

    public void setLeg1ConnectionType(LegConnectionType leg1ConnectionType) {
        this.leg1ConnectionType = leg1ConnectionType;
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

}
