/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.ThreeWindingsTransformer;

import java.util.Objects;

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class ThreeWindingsTransformerShortCircuitAdder extends AbstractExtensionAdder<ThreeWindingsTransformer, ThreeWindingsTransformerShortCircuit> {

    private double leg1CoeffRo = DEFAULT_COEFF_RO;
    private double leg2CoeffRo = DEFAULT_COEFF_RO;
    private double leg3CoeffRo = DEFAULT_COEFF_RO;
    private double leg1CoeffXo = DEFAULT_COEFF_XO;
    private double leg2CoeffXo = DEFAULT_COEFF_XO;
    private double leg3CoeffXo = DEFAULT_COEFF_XO;
    private boolean leg1FreeFluxes = DEFAULT_FREE_FLUXES;
    private boolean leg2FreeFluxes = DEFAULT_FREE_FLUXES;
    private boolean leg3FreeFluxes = DEFAULT_FREE_FLUXES;
    private LegConnectionType leg1ConnectionType = DEFAULT_LEG1_CONNECTION_TYPE;
    private LegConnectionType leg2ConnectionType = DEFAULT_LEG2_CONNECTION_TYPE;
    private LegConnectionType leg3ConnectionType = DEFAULT_LEG3_CONNECTION_TYPE;
    private double leg1Ro = DEFAULT_COEFF_RO;
    private double leg2Ro = DEFAULT_COEFF_RO;
    private double leg3Ro = DEFAULT_COEFF_RO;
    private double leg1Xo = DEFAULT_COEFF_XO;
    private double leg2Xo = DEFAULT_COEFF_XO;
    private double leg3Xo = DEFAULT_COEFF_XO;
    private double kt1R = DEFAULT_COEFF_K;
    private double kt1X = DEFAULT_COEFF_K;
    private double kt2R = DEFAULT_COEFF_K;
    private double kt2X = DEFAULT_COEFF_K;
    private double kt3R = DEFAULT_COEFF_K;
    private double kt3X = DEFAULT_COEFF_K;
    private double kt1Ro = DEFAULT_COEFF_K;
    private double kt1Xo = DEFAULT_COEFF_K;
    private double kt2Ro = DEFAULT_COEFF_K;
    private double kt2Xo = DEFAULT_COEFF_K;
    private double kt3Ro = DEFAULT_COEFF_K;
    private double kt3Xo = DEFAULT_COEFF_K;

    public ThreeWindingsTransformerShortCircuitAdder(ThreeWindingsTransformer twt) {
        super(twt);
    }

    @Override
    public Class<? super ThreeWindingsTransformerShortCircuit> getExtensionClass() {
        return ThreeWindingsTransformerShortCircuit.class;
    }

    @Override
    protected ThreeWindingsTransformerShortCircuit createExtension(ThreeWindingsTransformer twt) {
        return new ThreeWindingsTransformerShortCircuit(twt,
                leg1CoeffRo, leg2CoeffRo, leg3CoeffRo,
                leg1CoeffXo, leg2CoeffXo, leg3CoeffXo,
                leg1FreeFluxes, leg2FreeFluxes, leg3FreeFluxes,
                leg1ConnectionType, leg2ConnectionType, leg3ConnectionType,
                leg1Ro, leg2Ro, leg3Ro, leg1Xo, leg2Xo, leg3Xo,
                kt1R, kt1X, kt2R, kt2X, kt3R, kt3X,
                kt1Ro, kt1Xo, kt2Ro, kt2Xo, kt3Ro, kt3Xo);
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg1CoeffRo(double leg1CoeffRo) {
        this.leg1CoeffRo = leg1CoeffRo;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg2CoeffRo(double leg2CoeffRo) {
        this.leg2CoeffRo = leg2CoeffRo;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg3CoeffRo(double leg3CoeffRo) {
        this.leg3CoeffRo = leg3CoeffRo;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg1CoeffXo(double leg1CoeffXo) {
        this.leg1CoeffXo = leg1CoeffXo;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg2CoeffXo(double leg2CoeffXo) {
        this.leg2CoeffXo = leg2CoeffXo;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg3CoeffXo(double leg3CoeffXo) {
        this.leg3CoeffXo = leg3CoeffXo;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg1FreeFluxes(boolean leg1FreeFluxes) {
        this.leg1FreeFluxes = leg1FreeFluxes;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg2FreeFluxes(boolean leg2FreeFluxes) {
        this.leg2FreeFluxes = leg2FreeFluxes;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg3FreeFluxes(boolean leg3FreeFluxes) {
        this.leg3FreeFluxes = leg3FreeFluxes;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg1ConnectionType(LegConnectionType leg1ConnectionType) {
        this.leg1ConnectionType = Objects.requireNonNull(leg1ConnectionType);
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg2ConnectionType(LegConnectionType leg2ConnectionType) {
        this.leg2ConnectionType = Objects.requireNonNull(leg2ConnectionType);
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg3ConnectionType(LegConnectionType leg3ConnectionType) {
        this.leg3ConnectionType = Objects.requireNonNull(leg3ConnectionType);
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg1Ro(double leg1Ro) {
        this.leg1Ro = leg1Ro;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg2Ro(double leg2Ro) {
        this.leg2Ro = leg2Ro;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg3Ro(double leg3Ro) {
        this.leg3Ro = leg3Ro;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg1Xo(double leg1Xo) {
        this.leg1Xo = leg1Xo;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg2Xo(double leg2Xo) {
        this.leg2Xo = leg2Xo;
        return this;
    }

    public ThreeWindingsTransformerShortCircuitAdder withLeg3Xo(double leg3Xo) {
        this.leg3Xo = leg3Xo;
        return this;
    }

}
