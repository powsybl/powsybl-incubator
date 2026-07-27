
export interface PropertyDescriptor {
    key: string;
    unit?: string;
    type: 'number' | 'integer' | 'string' | 'boolean' | 'select';
    options?: string[];
    required?: boolean;
    min?: number;
    max?: number;
    defaultValue?: number | string | boolean;
    help?: string;
}

export const ENERGY_SOURCES = ['OTHER', 'HYDRO', 'NUCLEAR', 'WIND', 'SOLAR', 'THERMAL'];
export const LOAD_TYPES = ['UNDEFINED', 'AUXILIARY', 'FICTITIOUS'];
export const SVC_REGULATION_MODES = ['VOLTAGE', 'REACTIVE_POWER'];
export const CONVERTERS_MODES = [
    'SIDE_1_RECTIFIER_SIDE_2_INVERTER',
    'SIDE_1_INVERTER_SIDE_2_RECTIFIER',
];

const CONNECTED: PropertyDescriptor = { key: 'connected', type: 'boolean', defaultValue: true };
const CONNECTED_SIDES: PropertyDescriptor[] = [
    { key: 'connected1', type: 'boolean', defaultValue: true },
    { key: 'connected2', type: 'boolean', defaultValue: true },
];
const REGULATED_ELEMENT: PropertyDescriptor = {
    key: 'regulated_element_id',
    type: 'string',
    help: 'Element whose voltage is regulated; empty means the station itself.',
};

const LOAD: PropertyDescriptor[] = [
    { key: 'p0', unit: 'MW', type: 'number', required: true, defaultValue: 10 },
    { key: 'q0', unit: 'MVar', type: 'number', required: true, defaultValue: 0 },
    CONNECTED,
];

const GENERATOR: PropertyDescriptor[] = [
    { key: 'target_p', unit: 'MW', type: 'number', required: true, defaultValue: 100 },
    {
        key: 'target_v',
        unit: 'kV',
        type: 'number',
        defaultValue: 400,
        help: 'Required when the voltage regulator is on.',
    },
    {
        key: 'target_q',
        unit: 'MVar',
        type: 'number',
        defaultValue: 0,
        help: 'Used when the generator does not regulate voltage.',
    },
    { key: 'voltage_regulator_on', type: 'boolean', defaultValue: true },
    REGULATED_ELEMENT,
    CONNECTED,
];

const BATTERY: PropertyDescriptor[] = [
    { key: 'target_p', unit: 'MW', type: 'number', required: true, defaultValue: 0 },
    { key: 'target_q', unit: 'MVar', type: 'number', required: true, defaultValue: 0 },
    CONNECTED,
];

const SWITCH: PropertyDescriptor[] = [
    { key: 'open', type: 'boolean', required: true, defaultValue: false },
];

// Capacitors and inductors are both shunt compensators to pypowsybl.
const SHUNT_COMPENSATOR: PropertyDescriptor[] = [
    {
        key: 'section_count',
        type: 'integer',
        required: true,
        min: 0,
        defaultValue: 1,
        help: 'Number of connected sections.',
    },
    CONNECTED,
];

const STATIC_VAR_COMPENSATOR: PropertyDescriptor[] = [
    {
        key: 'regulation_mode',
        type: 'select',
        options: SVC_REGULATION_MODES,
        required: true,
        defaultValue: 'VOLTAGE',
    },
    {
        key: 'voltage_setpoint',
        unit: 'kV',
        type: 'number',
        defaultValue: 400,
        help: 'Required when the regulation mode is VOLTAGE.',
    },
    {
        key: 'reactive_power_setpoint',
        unit: 'MVar',
        type: 'number',
        defaultValue: 0,
        help: 'Used when the regulation mode is REACTIVE_POWER.',
    },
    REGULATED_ELEMENT,
    CONNECTED,
];

const VSC_CONVERTER_STATION: PropertyDescriptor[] = [
    { key: 'target_v', unit: 'kV', type: 'number', defaultValue: 400 },
    { key: 'target_q', unit: 'MVar', type: 'number', defaultValue: 0 },
    { key: 'voltage_regulator_on', type: 'boolean', defaultValue: false },
    REGULATED_ELEMENT,
    CONNECTED,
];

const LCC_CONVERTER_STATION: PropertyDescriptor[] = [
    {
        key: 'power_factor',
        type: 'number',
        required: true,
        min: -1,
        max: 1,
        defaultValue: 0.8,
    },
    CONNECTED,
];

const HVDC_LINE: PropertyDescriptor[] = [
    { key: 'active_power_setpoint', unit: 'MW', type: 'number', required: true, defaultValue: 0 },
    {
        key: 'converters_mode',
        type: 'select',
        options: CONVERTERS_MODES,
        required: true,
        defaultValue: CONVERTERS_MODES[0],
    },
];

const LINE: PropertyDescriptor[] = [
    { key: 'r', unit: 'Ω', type: 'number', required: true, min: 0, defaultValue: 0.1 },
    { key: 'x', unit: 'Ω', type: 'number', required: true, defaultValue: 1 },
    { key: 'g1', unit: 'S', type: 'number', defaultValue: 0 },
    { key: 'b1', unit: 'S', type: 'number', defaultValue: 0 },
    { key: 'g2', unit: 'S', type: 'number', defaultValue: 0 },
    { key: 'b2', unit: 'S', type: 'number', defaultValue: 0 },
    ...CONNECTED_SIDES,
];

const BOUNDARY_LINE: PropertyDescriptor[] = [
    { key: 'p0', unit: 'MW', type: 'number', required: true, defaultValue: 0 },
    { key: 'q0', unit: 'MVar', type: 'number', required: true, defaultValue: 0 },
    CONNECTED,
];

const TWO_WINDINGS_TRANSFORMER: PropertyDescriptor[] = [
    { key: 'r', unit: 'Ω', type: 'number', required: true, min: 0, defaultValue: 0.5 },
    { key: 'x', unit: 'Ω', type: 'number', required: true, defaultValue: 10 },
    { key: 'g', unit: 'S', type: 'number', defaultValue: 0 },
    { key: 'b', unit: 'S', type: 'number', defaultValue: 0 },
    ...CONNECTED_SIDES,
];

export const PROPERTY_SCHEMAS: Readonly<Record<string, PropertyDescriptor[]>> = {
    LOAD,
    GENERATOR,
    BATTERY,

    BREAKER: SWITCH,
    DISCONNECTOR: SWITCH,
    LOAD_BREAK_SWITCH: SWITCH,

    CAPACITOR: SHUNT_COMPENSATOR,
    INDUCTOR: SHUNT_COMPENSATOR,
    STATIC_VAR_COMPENSATOR,

    VSC_CONVERTER_STATION,
    LCC_CONVERTER_STATION,
    HVDC_LINE,

    LINE,
    TIE_LINE: LINE,
    BOUNDARY_LINE,

    TWO_WINDINGS_TRANSFORMER,
    TWO_WINDINGS_TRANSFORMER_LEG: TWO_WINDINGS_TRANSFORMER,
    PHASE_SHIFT_TRANSFORMER: TWO_WINDINGS_TRANSFORMER,
    PHASE_SHIFT_TRANSFORMER_LEG: TWO_WINDINGS_TRANSFORMER,

    // Not listed: BUSBAR_SECTION and 3-winding transformers — pypowsybl has no
    // `update_*` surface for them in `EDITABLE_COMPONENTS`.
};

/** Editable attributes of a component type, empty when it has none. */
export function getPropertySchema(componentType: string | null): PropertyDescriptor[] {
    return (componentType && PROPERTY_SCHEMAS[componentType]) || [];
}
