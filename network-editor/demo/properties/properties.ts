import type { ElementType } from '../../src';


export interface PropertyDescriptor {
    key: string;
    unit?: string;
    type: 'number' | 'string' | 'boolean' | 'select';
    options?: string[];
    required?: boolean;
    min?: number;
    max?: number;
    defaultValue?: number | string | boolean;
}

export const PROPERTY_SCHEMAS: Partial<Record<ElementType, PropertyDescriptor[]>> = {
    LOAD: [
        { key: 'p0', unit: 'MW', type: 'number', required: true, defaultValue: 10 },
        { key: 'q0', unit: 'MVar', type: 'number', required: true, defaultValue: 0 },
        { key: 'connected', type: 'boolean', required: true, defaultValue: true },
    ],
    GENERATOR: [
        { key: 'targetP', unit: 'MW', type: 'number', required: true, defaultValue: 100 },
        { key: 'targetV', unit: 'kV', type: 'number', defaultValue: 400 },
        { key: 'targetQ', unit: 'MVar', type: 'number', defaultValue: 0 },
        { key: 'voltageRegulatorOn', type: 'boolean', defaultValue: true },
        { key: 'connected', type: 'boolean', defaultValue: true },
    ],
    LINE: [
        { key: 'r', unit: 'Ω', type: 'number', required: true, min: 0, defaultValue: 0.1 },
        { key: 'x', unit: 'Ω', type: 'number', required: true, defaultValue: 1 },
        { key: 'connected1', type: 'boolean', defaultValue: true },
        { key: 'connected2', type: 'boolean', defaultValue: true },
    ],
    TWO_WINDINGS_TRANSFORMER: [
        { key: 'r', unit: 'Ω', type: 'number', required: true, min: 0, defaultValue: 0.1 },
        { key: 'x', unit: 'Ω', type: 'number', required: true, defaultValue: 1 },
        { key: 'connected1', type: 'boolean', defaultValue: true },
        { key: 'connected2', type: 'boolean', defaultValue: true },
    ],
    BREAKER: [
        { key: 'open', type: 'boolean', required: true, defaultValue: false },
    ],
    CAPACITOR: [
        { key: 'sectionCount', type: 'number', required: true, min: 0, defaultValue: 1 },
        { key: 'connected', type: 'boolean', defaultValue: true },
    ],
};
