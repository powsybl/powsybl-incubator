/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.incubator.simulator.util.extensions.LegConnectionType;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 */
public class TwoWindingsTransformerShortCircuitAdder extends AbstractExtensionAdder<TwoWindingsTransformer, TwoWindingsTransformerShortCircuit> {

    public static final double DEFAULT_COEFF_RO = 1;
    public static final double DEFAULT_COEFF_XO = 1;
    public static final boolean DEFAULT_FREE_FLUXES = false;
    public static final LegConnectionType DEFAULT_LEG1_CONNECTION_TYPE = LegConnectionType.DELTA; // TODO : check if default connection acceptable
    public static final LegConnectionType DEFAULT_LEG2_CONNECTION_TYPE = LegConnectionType.Y_GROUNDED; // TODO : check if default connection acceptable

    private double coeffRo = DEFAULT_COEFF_RO;
    private double coeffXo = DEFAULT_COEFF_XO;
    private boolean freeFluxes = DEFAULT_FREE_FLUXES;
    private LegConnectionType leg1ConnectionType = DEFAULT_LEG1_CONNECTION_TYPE;
    private LegConnectionType leg2ConnectionType = DEFAULT_LEG2_CONNECTION_TYPE;

    public TwoWindingsTransformerShortCircuitAdder(TwoWindingsTransformer twt) {
        super(twt);
    }

    @Override
    public Class<? super TwoWindingsTransformerShortCircuit> getExtensionClass() {
        return TwoWindingsTransformerShortCircuit.class;
    }

    @Override
    protected TwoWindingsTransformerShortCircuit createExtension(TwoWindingsTransformer twt) {
        return new TwoWindingsTransformerShortCircuit(twt, coeffRo, coeffXo, freeFluxes, leg1ConnectionType, leg2ConnectionType);
    }

    public TwoWindingsTransformerShortCircuitAdder withCoeffRo(double coeffRo) {
        this.coeffRo = coeffRo;
        return this;
    }

    public TwoWindingsTransformerShortCircuitAdder withCoeffXo(double coeffXo) {
        this.coeffXo = coeffXo;
        return this;
    }

    public TwoWindingsTransformerShortCircuitAdder withFreeFluxes(boolean freeFluxes) {
        this.freeFluxes = freeFluxes;
        return this;
    }

    public TwoWindingsTransformerShortCircuitAdder withLeg1ConnectionType(LegConnectionType leg1ConnectionType) {
        this.leg1ConnectionType = Objects.requireNonNull(leg1ConnectionType);
        return this;
    }

    public TwoWindingsTransformerShortCircuitAdder withLeg2ConnectionType(LegConnectionType leg2ConnectionType) {
        this.leg2ConnectionType = Objects.requireNonNull(leg2ConnectionType);
        return this;
    }
}
