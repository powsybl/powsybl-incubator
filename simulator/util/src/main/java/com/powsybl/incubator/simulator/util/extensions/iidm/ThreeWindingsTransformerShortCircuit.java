/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.ThreeWindingsTransformer;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class ThreeWindingsTransformerShortCircuit extends AbstractExtension<ThreeWindingsTransformer> {

    public static final String NAME = "threeWindingsTransformerShortCircuit";

    private double leg1CoeffRo;
    private double leg2CoeffRo;
    private double leg3CoeffRo;
    private double leg1CoeffXo;
    private double leg2CoeffXo;
    private double leg3CoeffXo;
    private double leg1Ro;
    private double leg2Ro;
    private double leg3Ro;
    private double leg1Xo;
    private double leg2Xo;
    private double leg3Xo;
    private boolean leg1FreeFluxes;
    private boolean leg2FreeFluxes;
    private boolean leg3FreeFluxes;
    private LegConnectionType leg1ConnectionType;
    private LegConnectionType leg2ConnectionType;
    private LegConnectionType leg3ConnectionType;

    @Override
    public String getName() {
        return NAME;
    }

    public ThreeWindingsTransformerShortCircuit(ThreeWindingsTransformer extendable,
                                                double leg1CoeffRo, double leg2CoeffRo, double leg3CoeffRo,
                                                double leg1CoeffXo, double leg2CoeffXo, double leg3CoeffXo,
                                                boolean leg1FreeFluxes, boolean leg2FreeFluxes, boolean leg3FreeFluxes,
                                                LegConnectionType leg1ConnectionType, LegConnectionType leg2ConnectionType, LegConnectionType leg3ConnectionType,
                                                double leg1Ro, double leg2Ro, double leg3Ro,
                                                double leg1Xo, double leg2Xo, double leg3Xo) {
        super(extendable);
        this.leg1CoeffRo = leg1CoeffRo;
        this.leg2CoeffRo = leg2CoeffRo;
        this.leg3CoeffRo = leg3CoeffRo;
        this.leg1CoeffXo = leg1CoeffXo;
        this.leg2CoeffXo = leg2CoeffXo;
        this.leg3CoeffXo = leg3CoeffXo;
        this.leg1Ro = leg1Ro;
        this.leg2Ro = leg2Ro;
        this.leg3Ro = leg3Ro;
        this.leg1Xo = leg1Xo;
        this.leg2Xo = leg2Xo;
        this.leg3Xo = leg3Xo;
        this.leg1FreeFluxes = leg1FreeFluxes;
        this.leg2FreeFluxes = leg2FreeFluxes;
        this.leg3FreeFluxes = leg3FreeFluxes;
        this.leg1ConnectionType = Objects.requireNonNull(leg1ConnectionType);
        this.leg2ConnectionType = Objects.requireNonNull(leg2ConnectionType);
        this.leg3ConnectionType = Objects.requireNonNull(leg3ConnectionType);
    }

    public double getLeg1CoeffRo() {
        return leg1CoeffRo;
    }

    public double getLeg2CoeffRo() {
        return leg2CoeffRo;
    }

    public double getLeg3CoeffRo() {
        return leg3CoeffRo;
    }

    public double getLeg1CoeffXo() {
        return leg1CoeffXo;
    }

    public double getLeg2CoeffXo() {
        return leg2CoeffXo;
    }

    public double getLeg3CoeffXo() {
        return leg3CoeffXo;
    }

    public boolean isLeg1FreeFluxes() {
        return leg1FreeFluxes;
    }

    public boolean isLeg2FreeFluxes() {
        return leg2FreeFluxes;
    }

    public boolean isLeg3FreeFluxes() {
        return leg3FreeFluxes;
    }

    public LegConnectionType getLeg1ConnectionType() {
        return leg1ConnectionType;
    }

    public LegConnectionType getLeg2ConnectionType() {
        return leg2ConnectionType;
    }

    public LegConnectionType getLeg3ConnectionType() {
        return leg3ConnectionType;
    }

    public void setLeg1CoeffRo(double leg1CoeffRo) {
        this.leg1CoeffRo = leg1CoeffRo;
    }

    public void setLeg1CoeffXo(double leg1CoeffXo) {
        this.leg1CoeffXo = leg1CoeffXo;
    }

    public void setLeg2CoeffRo(double leg2CoeffRo) {
        this.leg2CoeffRo = leg2CoeffRo;
    }

    public void setLeg2CoeffXo(double leg2CoeffXo) {
        this.leg2CoeffXo = leg2CoeffXo;
    }

    public void setLeg3CoeffRo(double leg3CoeffRo) {
        this.leg3CoeffRo = leg3CoeffRo;
    }

    public void setLeg3CoeffXo(double leg3CoeffXo) {
        this.leg3CoeffXo = leg3CoeffXo;
    }

    public void setLeg1ConnectionType(LegConnectionType leg1ConnectionType) {
        this.leg1ConnectionType = leg1ConnectionType;
    }

    public void setLeg2ConnectionType(LegConnectionType leg2ConnectionType) {
        this.leg2ConnectionType = leg2ConnectionType;
    }

    public void setLeg3ConnectionType(LegConnectionType leg3ConnectionType) {
        this.leg3ConnectionType = leg3ConnectionType;
    }

    public void setLeg1FreeFluxes(boolean leg1FreeFluxes) {
        this.leg1FreeFluxes = leg1FreeFluxes;
    }

    public void setLeg2FreeFluxes(boolean leg2FreeFluxes) {
        this.leg2FreeFluxes = leg2FreeFluxes;
    }

    public void setLeg3FreeFluxes(boolean leg3FreeFluxes) {
        this.leg3FreeFluxes = leg3FreeFluxes;
    }

    public double getLeg1Ro() {
        return leg1Ro;
    }

    public double getLeg1Xo() {
        return leg1Xo;
    }

    public double getLeg2Ro() {
        return leg2Ro;
    }

    public double getLeg2Xo() {
        return leg2Xo;
    }

    public double getLeg3Ro() {
        return leg3Ro;
    }

    public double getLeg3Xo() {
        return leg3Xo;
    }

    public void setLeg1Ro(double leg1Ro) {
        this.leg1Ro = leg1Ro;
    }

    public void setLeg1Xo(double leg1Xo) {
        this.leg1Xo = leg1Xo;
    }

    public void setLeg2Ro(double leg2Ro) {
        this.leg2Ro = leg2Ro;
    }

    public void setLeg2Xo(double leg2Xo) {
        this.leg2Xo = leg2Xo;
    }

    public void setLeg3Ro(double leg3Ro) {
        this.leg3Ro = leg3Ro;
    }

    public void setLeg3Xo(double leg3Xo) {
        this.leg3Xo = leg3Xo;
    }
}
