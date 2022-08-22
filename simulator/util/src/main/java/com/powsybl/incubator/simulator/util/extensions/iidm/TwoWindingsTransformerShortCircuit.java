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

    private final double coeffRo; // Ro = Rd * CoeffRo
    private final double coeffXo;
    private final boolean freeFluxes; // free fluxes mean that magnetizing impedance Zm is infinite, by default, fluxes are forced and Zm exists
    private final LegConnectionType leg1ConnectionType;
    private final LegConnectionType leg2ConnectionType;
    private boolean isPartOfGeneratingUnit;

    @Override
    public String getName() {
        return NAME;
    }

    public TwoWindingsTransformerShortCircuit(TwoWindingsTransformer twt, boolean isPartOfGeneratingUnit, double coeffRo, double coeffXo, boolean freeFluxes,
                                              LegConnectionType leg1ConnectionType, LegConnectionType leg2ConnectionType) {
        super(twt);
        this.isPartOfGeneratingUnit = isPartOfGeneratingUnit;
        this.coeffRo = coeffRo;
        this.coeffXo = coeffXo;
        this.freeFluxes = freeFluxes;
        this.leg1ConnectionType = Objects.requireNonNull(leg1ConnectionType);
        this.leg2ConnectionType = Objects.requireNonNull(leg2ConnectionType);
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
}
