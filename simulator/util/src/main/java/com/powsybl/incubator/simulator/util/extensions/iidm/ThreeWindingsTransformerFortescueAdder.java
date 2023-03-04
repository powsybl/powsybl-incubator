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

import static com.powsybl.incubator.simulator.util.extensions.iidm.FortescueConstants.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class ThreeWindingsTransformerFortescueAdder extends AbstractExtensionAdder<ThreeWindingsTransformer, ThreeWindingsTransformerFortescue> {

    private double leg1Ro = 0;
    private double leg2Ro = 0;
    private double leg3Ro = 0;
    private double leg1Xo = 0;
    private double leg2Xo = 0;
    private double leg3Xo = 0;
    private boolean leg1FreeFluxes = DEFAULT_FREE_FLUXES;
    private boolean leg2FreeFluxes = DEFAULT_FREE_FLUXES;
    private boolean leg3FreeFluxes = DEFAULT_FREE_FLUXES;
    private LegConnectionType leg1ConnectionType = DEFAULT_LEG1_CONNECTION_TYPE;
    private LegConnectionType leg2ConnectionType = DEFAULT_LEG2_CONNECTION_TYPE;
    private LegConnectionType leg3ConnectionType = DEFAULT_LEG3_CONNECTION_TYPE;

    public ThreeWindingsTransformerFortescueAdder(ThreeWindingsTransformer twt) {
        super(twt);
    }

    @Override
    public Class<? super ThreeWindingsTransformerFortescue> getExtensionClass() {
        return ThreeWindingsTransformerFortescue.class;
    }

    @Override
    protected ThreeWindingsTransformerFortescue createExtension(ThreeWindingsTransformer twt) {
        return new ThreeWindingsTransformerFortescue(twt,
                leg1Ro, leg2Ro, leg3Ro,
                leg1Xo, leg2Xo, leg3Xo,
                leg1FreeFluxes, leg2FreeFluxes, leg3FreeFluxes,
                leg1ConnectionType, leg2ConnectionType, leg3ConnectionType);
    }

    public ThreeWindingsTransformerFortescueAdder withLeg1Ro(double leg1Ro) {
        this.leg1Ro = leg1Ro;
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg2Ro(double leg2Ro) {
        this.leg2Ro = leg2Ro;
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg3Ro(double leg3Ro) {
        this.leg3Ro = leg3Ro;
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg1Xo(double leg1Xo) {
        this.leg1Xo = leg1Xo;
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg2Xo(double leg2Xo) {
        this.leg2Xo = leg2Xo;
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg3Xo(double leg3Xo) {
        this.leg3Xo = leg3Xo;
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg1FreeFluxes(boolean leg1FreeFluxes) {
        this.leg1FreeFluxes = leg1FreeFluxes;
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg2FreeFluxes(boolean leg2FreeFluxes) {
        this.leg2FreeFluxes = leg2FreeFluxes;
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg3FreeFluxes(boolean leg3FreeFluxes) {
        this.leg3FreeFluxes = leg3FreeFluxes;
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg1ConnectionType(LegConnectionType leg1ConnectionType) {
        this.leg1ConnectionType = Objects.requireNonNull(leg1ConnectionType);
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg2ConnectionType(LegConnectionType leg2ConnectionType) {
        this.leg2ConnectionType = Objects.requireNonNull(leg2ConnectionType);
        return this;
    }

    public ThreeWindingsTransformerFortescueAdder withLeg3ConnectionType(LegConnectionType leg3ConnectionType) {
        this.leg3ConnectionType = Objects.requireNonNull(leg3ConnectionType);
        return this;
    }

}
