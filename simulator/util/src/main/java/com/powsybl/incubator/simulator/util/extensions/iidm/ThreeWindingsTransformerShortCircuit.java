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

    public static String NAME = "threeWindingsTransformerShortCircuit";

    private final double leg1CoeffRo;
    private final double leg2CoeffRo;
    private final double leg3CoeffRo;
    private final double leg1CoeffXo;
    private final double leg2CoeffXo;
    private final double leg3CoeffXo;
    private final boolean leg1FreeFluxes;
    private final boolean leg2FreeFluxes;
    private final boolean leg3FreeFluxes;
    private final LegConnectionType leg1ConnectionType;
    private final LegConnectionType leg2ConnectionType;
    private final LegConnectionType leg3ConnectionType;

    @Override
    public String getName() {
        return NAME;
    }

    public ThreeWindingsTransformerShortCircuit(ThreeWindingsTransformer extendable,
                                                double leg1CoeffRo, double leg2CoeffRo, double leg3CoeffRo,
                                                double leg1CoeffXo, double leg2CoeffXo, double leg3CoeffXo,
                                                boolean leg1FreeFluxes, boolean leg2FreeFluxes, boolean leg3FreeFluxes,
                                                LegConnectionType leg1ConnectionType, LegConnectionType leg2ConnectionType, LegConnectionType leg3ConnectionType) {
        super(extendable);
        this.leg1CoeffRo = leg1CoeffRo;
        this.leg2CoeffRo = leg2CoeffRo;
        this.leg3CoeffRo = leg3CoeffRo;
        this.leg1CoeffXo = leg1CoeffXo;
        this.leg2CoeffXo = leg2CoeffXo;
        this.leg3CoeffXo = leg3CoeffXo;
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
}
