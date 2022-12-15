/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

import com.powsybl.commons.extensions.AbstractExtensionAdder;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.Objects;

import static com.powsybl.incubator.simulator.util.extensions.iidm.ShortCircuitConstants.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public class TwoWindingsTransformerShortCircuitAdder extends AbstractExtensionAdder<TwoWindingsTransformer, TwoWindingsTransformerShortCircuit> {

    private boolean isPartOfGeneratingUnit = false;
    private double coeffRo = DEFAULT_COEFF_RO;
    private double coeffXo = DEFAULT_COEFF_XO;
    private double ro = 0.;
    private double xo = 0.;
    private boolean freeFluxes = DEFAULT_FREE_FLUXES;
    private LegConnectionType leg1ConnectionType = DEFAULT_LEG1_CONNECTION_TYPE;
    private LegConnectionType leg2ConnectionType = DEFAULT_LEG2_CONNECTION_TYPE;
    private double r1Ground = 0.;
    private double x1Ground = 0.;
    private double r2Ground = 0.;
    private double x2Ground = 0.;
    private double kNorm = 1.;

    public TwoWindingsTransformerShortCircuitAdder(TwoWindingsTransformer twt) {
        super(twt);
    }

    @Override
    public Class<? super TwoWindingsTransformerShortCircuit> getExtensionClass() {
        return TwoWindingsTransformerShortCircuit.class;
    }

    @Override
    protected TwoWindingsTransformerShortCircuit createExtension(TwoWindingsTransformer twt) {
        return new TwoWindingsTransformerShortCircuit(twt, isPartOfGeneratingUnit, ro, xo, kNorm, freeFluxes, leg1ConnectionType, leg2ConnectionType, r1Ground, x1Ground, r2Ground, x2Ground);
    }

    public TwoWindingsTransformerShortCircuitAdder withIsPartOfGeneratingUnit(boolean isPartOfGeneratingUnit) {
        this.isPartOfGeneratingUnit = isPartOfGeneratingUnit;
        return this;
    }

    public TwoWindingsTransformerShortCircuitAdder withCoeffRo(double coeffRo) {
        this.coeffRo = coeffRo;
        return this;
    }

    public TwoWindingsTransformerShortCircuitAdder withCoeffXo(double coeffXo) {
        this.coeffXo = coeffXo;
        return this;
    }

    public TwoWindingsTransformerShortCircuitAdder withRo(double ro) {
        this.ro = ro;
        return this;
    }

    public TwoWindingsTransformerShortCircuitAdder withXo(double xo) {
        this.xo = xo;
        return this;
    }

    public TwoWindingsTransformerShortCircuitAdder withKnorm(double kNorm) {
        this.kNorm = kNorm;
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
