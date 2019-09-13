package com.powsybl.substationdiagram.library;

public final class ComponentTypeName {
    public static final String ARROW = "ARROW";
    public static final String BUSBAR_SECTION = "BUSBAR_SECTION";
    public static final String BREAKER = "BREAKER";
    public static final String DISCONNECTOR = "DISCONNECTOR";
    public static final String GENERATOR = "GENERATOR";
    public static final String LINE = "LINE";
    public static final String LOAD = "LOAD";
    public static final String LOAD_BREAK_SWITCH = "LOAD_BREAK_SWITCH";
    public static final String NODE = "NODE";
    public static final String CAPACITOR = "CAPACITOR";
    public static final String INDUCTOR = "INDUCTOR";
    public static final String STATIC_VAR_COMPENSATOR = "STATIC_VAR_COMPENSATOR";
    public static final String TWO_WINDINGS_TRANSFORMER = "TWO_WINDINGS_TRANSFORMER";
    public static final String THREE_WINDINGS_TRANSFORMER = "THREE_WINDINGS_TRANSFORMER";
    public static final String VSC_CONVERTER_STATION = "VSC_CONVERTER_STATION";
    public static final String DANGLING_LINE = "DANGLING_LINE";
    public static final String PHASE_SHIFT_TRANSFORMER = "PHASE_SHIFT_TRANSFORMER";

    private ComponentTypeName() {
        throw new AssertionError();
    }
}
