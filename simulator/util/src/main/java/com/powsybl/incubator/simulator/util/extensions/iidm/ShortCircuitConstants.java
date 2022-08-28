/**
 * Copyright (c) 2022, Jean-Baptiste Heyberger & Geoffroy Jamgotchian
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.incubator.simulator.util.extensions.iidm;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at gmail.com>
 * @author Jean-Baptiste Heyberger <jbheyberger at gmail.com>
 */
public final class ShortCircuitConstants {

    private ShortCircuitConstants() {
    }

    public static final double DEFAULT_TRANS_RD = 0;
    public static final double DEFAULT_SUB_TRANS_RD = 0;
    public static final boolean DEFAULT_TO_GROUND = false;
    public static final double DEFAULT_GROUNDING_R = 0.;
    public static final double DEFAULT_GROUNDING_X = 0.;
    public static final double DEFAULT_COEFF_RO = 1;
    public static final double DEFAULT_COEFF_XO = 1;
    public static final double DEFAULT_COEFF_RI = 1;
    public static final double DEFAULT_COEFF_XI = 1;
    public static final double DEFAULT_COS_PHI = 0.85;
    public static final double DEFAULT_RATED_U = 100.;
    public static final boolean DEFAULT_FREE_FLUXES = true;
    public static final LegConnectionType DEFAULT_LEG1_CONNECTION_TYPE = LegConnectionType.DELTA; // TODO : check if default connection acceptable
    public static final LegConnectionType DEFAULT_LEG2_CONNECTION_TYPE = LegConnectionType.Y_GROUNDED; // TODO : check if default connection acceptable
    public static final LegConnectionType DEFAULT_LEG3_CONNECTION_TYPE = LegConnectionType.DELTA; // TODO : check if default connection acceptable

    public static final double DEFAULT_TRANS_XD = 20.; //supposed in ohms // TODO : check most standard value
    public static final double DEFAULT_SUB_TRANS_XD = 20.; //supposed in ohms // TODO : check most standard value
    public static final double DEFAULT_STEP_UP_XD = 0.; //supposed in ohms // TODO : check most standard value

    public static final GeneratorShortCircuit2.GeneratorType DEFAULT_GENERATOR_TYPE = GeneratorShortCircuit2.GeneratorType.ROTATING_MACHINE;
    public static final double DEFAULT_CQ = 1.1;
    public static final double DEFAULT_R1_X1_RATIO = 1.;
    public static final double DEFAULT_IKQ = 10;
}
