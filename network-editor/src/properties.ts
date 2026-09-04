import type { ElementType, EquipmentProperties } from './core/types';

export type PropertyMode = 'create' | 'edit';

export interface PropertyDescriptor {
    key: string;
    type: 'number' | 'string' | 'boolean' | 'select';
    unit?: string;
    options?: string[];
    required?: boolean;
    min?: number;
    max?: number;
    defaultValue?: number | string | boolean;
    editOnly?: boolean;
    readOnly?: boolean;
}

export const EQUIPMENT_ID: PropertyDescriptor = {
    key: 'equipmentId',
    type: 'string',
    required: true,
};

const CONNECTED: PropertyDescriptor = {
    key: 'connected',
    type: 'boolean',
    defaultValue: true,
    editOnly: true,
};

const SWITCH_SCHEMA: PropertyDescriptor[] = [
    { key: 'open', type: 'boolean', required: true, defaultValue: false },
];

const BRANCH_SCHEMA: PropertyDescriptor[] = [
    EQUIPMENT_ID,
    { key: 'r', unit: 'Ω', type: 'number', required: true, min: 0, defaultValue: 0.1 },
    { key: 'x', unit: 'Ω', type: 'number', required: true, defaultValue: 1 },
    { key: 'connected1', type: 'boolean', defaultValue: true, editOnly: true },
    { key: 'connected2', type: 'boolean', defaultValue: true, editOnly: true },
];

export const PROPERTY_SCHEMAS: Partial<Record<ElementType, PropertyDescriptor[]>> = {
    LOAD: [
        EQUIPMENT_ID,
        { key: 'p0', unit: 'MW', type: 'number', required: true, defaultValue: 10 },
        { key: 'q0', unit: 'MVar', type: 'number', required: true, defaultValue: 0 },
        CONNECTED,
    ],
    GENERATOR: [
        EQUIPMENT_ID,
        { key: 'targetP', unit: 'MW', type: 'number', required: true, defaultValue: 100 },
        { key: 'targetV', unit: 'kV', type: 'number', defaultValue: 400 },
        { key: 'targetQ', unit: 'MVar', type: 'number', defaultValue: 0 },
        { key: 'voltageRegulatorOn', type: 'boolean', defaultValue: true },
        { key: 'minP', unit: 'MW', type: 'number', required: true, defaultValue: 0 },
        { key: 'maxP', unit: 'MW', type: 'number', required: true, defaultValue: 100 },
        CONNECTED,
    ],
    BATTERY: [
        EQUIPMENT_ID,
        { key: 'targetP', unit: 'MW', type: 'number', required: true, defaultValue: 0 },
        { key: 'targetQ', unit: 'MVar', type: 'number', required: true, defaultValue: 0 },
        { key: 'minP', unit: 'MW', type: 'number', required: true, defaultValue: -10 },
        { key: 'maxP', unit: 'MW', type: 'number', required: true, defaultValue: 10 },
        CONNECTED,
    ],
    SHUNT: [
        EQUIPMENT_ID,
        { key: 'sectionCount', type: 'number', required: true, min: 0, defaultValue: 1 },
        { key: 'maximumSectionCount', type: 'number', required: true, min: 1, defaultValue: 1 },
        { key: 'bPerSection', unit: 'S', type: 'number', required: true, defaultValue: 1e-5 },
        { key: 'gPerSection', unit: 'S', type: 'number', defaultValue: 0 },
        CONNECTED,
    ],
    STATIC_VAR_COMPENSATOR: [
        EQUIPMENT_ID,
        { key: 'bMin', unit: 'S', type: 'number', required: true, defaultValue: -0.01 },
        { key: 'bMax', unit: 'S', type: 'number', required: true, defaultValue: 0.01 },
        {
            key: 'regulationMode',
            type: 'select',
            options: ['VOLTAGE', 'REACTIVE_POWER'],
            required: true,
            defaultValue: 'VOLTAGE',
        },
        { key: 'regulating', type: 'boolean', required: true, defaultValue: false },
        { key: 'voltageSetpoint', unit: 'kV', type: 'number' },
        { key: 'reactivePowerSetpoint', unit: 'MVar', type: 'number' },
        CONNECTED,
    ],
    VSC_CONVERTER_STATION: [
        EQUIPMENT_ID,
        { key: 'lossFactor', unit: '%', type: 'number', required: true, min: 0, defaultValue: 1.1 },
        { key: 'voltageRegulatorOn', type: 'boolean', required: true, defaultValue: false },
        { key: 'voltageSetpoint', unit: 'kV', type: 'number' },
        { key: 'reactivePowerSetpoint', unit: 'MVar', type: 'number', defaultValue: 0 },
        CONNECTED,
    ],
    LCC_CONVERTER_STATION: [
        EQUIPMENT_ID,
        { key: 'lossFactor', unit: '%', type: 'number', required: true, min: 0, defaultValue: 1.1 },
        { key: 'powerFactor', type: 'number', required: true, min: -1, max: 1, defaultValue: 0.5 },
        CONNECTED,
    ],
    BOUNDARY_LINE: [
        EQUIPMENT_ID,
        { key: 'p0', unit: 'MW', type: 'number', required: true, defaultValue: 0 },
        { key: 'q0', unit: 'MVar', type: 'number', required: true, defaultValue: 0 },
        { key: 'r', unit: 'Ω', type: 'number', required: true, min: 0, defaultValue: 0.1 },
        { key: 'x', unit: 'Ω', type: 'number', required: true, defaultValue: 1 },
        { key: 'g', unit: 'S', type: 'number', required: true, min: 0, defaultValue: 0 },
        { key: 'b', unit: 'S', type: 'number', required: true, defaultValue: 0 },
        CONNECTED,
    ],
    GROUND: [CONNECTED],
    LINE: BRANCH_SCHEMA,
    TWO_WINDINGS_TRANSFORMER: BRANCH_SCHEMA,
    THREE_WINDINGS_TRANSFORMER: [1, 2, 3].flatMap((leg): PropertyDescriptor[] => [
        EQUIPMENT_ID,
        { key: `r${leg}`, unit: 'Ω', type: 'number', required: true, min: 0, defaultValue: 0.1 },
        { key: `x${leg}`, unit: 'Ω', type: 'number', required: true, defaultValue: 1 },
        { key: `connected${leg}`, type: 'boolean', defaultValue: true, editOnly: true },
    ]),
    BREAKER: SWITCH_SCHEMA,
    DISCONNECTOR: SWITCH_SCHEMA,
    LOAD_BREAK_SWITCH: SWITCH_SCHEMA,
};

export function schemaFor(type: ElementType, mode: PropertyMode = 'edit'): PropertyDescriptor[] {
    const schema = PROPERTY_SCHEMAS[type] ?? [];
    return mode === 'create' ? schema.filter((descriptor) => !descriptor.editOnly) : schema;
}

export function defaultsOf(schema: readonly PropertyDescriptor[]): EquipmentProperties {
    const values: EquipmentProperties = {};
    for (const descriptor of schema) {
        if (descriptor.defaultValue === undefined) continue;
        values[descriptor.key] = descriptor.defaultValue;
    }
    return values;
}

export function validateValues(
    schema: readonly PropertyDescriptor[],
    values: EquipmentProperties,
): string[] {
    const invalid: string[] = [];

    for (const descriptor of schema) {
        const value = values[descriptor.key];

        if (value === undefined || value === '') {
            if (descriptor.required) invalid.push(descriptor.key);
        } else if (descriptor.type === 'number') {
            const numeric = Number(value);
            if (!Number.isFinite(numeric)) {
                invalid.push(descriptor.key);
            } else if (descriptor.min !== undefined && numeric < descriptor.min) {
                invalid.push(descriptor.key);
            } else if (descriptor.max !== undefined && numeric > descriptor.max) {
                invalid.push(descriptor.key);
            }
        } else if (descriptor.type === 'select' && descriptor.options) {
            if (!descriptor.options.includes(String(value))) invalid.push(descriptor.key);
        }
    }
    return invalid;
}
