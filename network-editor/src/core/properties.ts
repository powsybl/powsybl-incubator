import type { ElementType } from './types';

export interface PropertyDescriptor {
    key: string;
    label: string;
    unit?: string;
    type: 'number' | 'string' | 'boolean' | 'select';
    options?: string[];
    required?: boolean;
    min?: number;
    max?: number;
    defaultValue?: number | string | boolean;
}

export type EquipmentProperties = Record<string, number | string | boolean>;

export const PROPERTY_SCHEMAS: Partial<Record<ElementType, PropertyDescriptor[]>> = {
    LOAD: [
        { key: 'p0', label: 'Puissance active', unit: 'MW', type: 'number', required: true, defaultValue: 10 },
        { key: 'q0', label: 'Puissance réactive', unit: 'MVar', type: 'number', required: true, defaultValue: 0 },
        { key: 'connected', label: 'Connectée', type: 'boolean', required: true, defaultValue: true },
    ],
    GENERATOR: [
        { key: 'target_p', label: 'Consigne P', unit: 'MW', type: 'number', required: true, defaultValue: 100 },
        { key: 'target_v', label: 'Consigne V', unit: 'kV', type: 'number', defaultValue: 400 },
        { key: 'target_q', label: 'Consigne Q', unit: 'MVar', type: 'number', defaultValue: 0 },
        { key: 'voltage_regulator_on', label: 'Régulation de tension', type: 'boolean', defaultValue: true },
        { key: 'connected', label: 'Connecté', type: 'boolean', defaultValue: true },
    ],
    LINE: [
        { key: 'r', label: 'Résistance', unit: 'Ω', type: 'number', required: true, min: 0, defaultValue: 0.1 },
        { key: 'x', label: 'Réactance', unit: 'Ω', type: 'number', required: true, defaultValue: 1 },
        { key: 'connected1', label: 'Connectée côté 1', type: 'boolean', defaultValue: true },
        { key: 'connected2', label: 'Connectée côté 2', type: 'boolean', defaultValue: true },
    ],
    TWO_WINDINGS_TRANSFORMER: [
        { key: 'r', label: 'Résistance', unit: 'Ω', type: 'number', required: true, min: 0, defaultValue: 0.1 },
        { key: 'x', label: 'Réactance', unit: 'Ω', type: 'number', required: true, defaultValue: 1 },
        { key: 'connected1', label: 'Connecté côté 1', type: 'boolean', defaultValue: true },
        { key: 'connected2', label: 'Connecté côté 2', type: 'boolean', defaultValue: true },
    ],
    BREAKER: [
        { key: 'open', label: 'Ouvert', type: 'boolean', required: true, defaultValue: false },
    ],
    CAPACITOR: [
        { key: 'section_count', label: 'Nombre de sections', type: 'number', required: true, min: 0, defaultValue: 1 },
        { key: 'connected', label: 'Connecté', type: 'boolean', defaultValue: true },
    ],
};
