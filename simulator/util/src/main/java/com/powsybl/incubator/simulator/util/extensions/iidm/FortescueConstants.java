package com.powsybl.incubator.simulator.util.extensions.iidm;

public final class FortescueConstants {

    private FortescueConstants() {
    }

    public static final boolean DEFAULT_TO_GROUND = false;
    public static final double DEFAULT_GROUNDING_R = 0.;
    public static final double DEFAULT_GROUNDING_X = 0.;
    public static final double DEFAULT_COEFF_RO = 1;
    public static final double DEFAULT_COEFF_XO = 1;
    public static final double DEFAULT_COEFF_RI = 1;
    public static final double DEFAULT_COEFF_XI = 1;
    public static final boolean DEFAULT_FREE_FLUXES = true;
    public static final LegConnectionType DEFAULT_LEG1_CONNECTION_TYPE = LegConnectionType.DELTA; // TODO : check if default connection acceptable
    public static final LegConnectionType DEFAULT_LEG2_CONNECTION_TYPE = LegConnectionType.Y_GROUNDED; // TODO : check if default connection acceptable
    public static final LegConnectionType DEFAULT_LEG3_CONNECTION_TYPE = LegConnectionType.DELTA; // TODO : check if default connection acceptable

    public static final GeneratorFortescue.GeneratorType DEFAULT_GENERATOR_FORTESCUE_TYPE = GeneratorFortescue.GeneratorType.ROTATING_MACHINE;
}
