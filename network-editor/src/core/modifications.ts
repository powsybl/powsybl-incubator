import type { ElementType, EquipmentProperties, FeederDirection } from './types';

export type ConnectionDirection = 'TOP' | 'BOTTOM' | 'UNDEFINED';

/** AttributeModification: one field of an equipment modification. */
export interface AttributeModification<T> {
    value?: T;
    op: 'SET' | 'UNSET';
}

export interface InjectionCreationFields {
    equipmentId: string;
    equipmentName: string | null;
    voltageLevelId: string;
    busOrBusbarSectionId: string;
    connectionName: string | null;
    connectionDirection: ConnectionDirection | null;
    connectionPosition: number | null;
    terminalConnected: boolean;
}

export type LoadType = 'UNDEFINED' | 'AUXILIARY' | 'FICTITIOUS';

export interface LoadCreation extends InjectionCreationFields {
    type: 'LOAD_CREATION';
    loadType: LoadType;
    p0: number;
    q0: number;
}

export type EnergySource = 'HYDRO' | 'NUCLEAR' | 'WIND' | 'THERMAL' | 'SOLAR' | 'OTHER';

export interface GeneratorCreation extends InjectionCreationFields {
    type: 'GENERATOR_CREATION';
    energySource: EnergySource;
    targetP: number;
    targetQ?: number;
    targetV?: number;
    voltageRegulationOn: boolean;
    minP: number;
    maxP: number;
}

export interface BatteryCreation extends InjectionCreationFields {
    type: 'BATTERY_CREATION';
    targetP: number;
    targetQ: number;
    minP: number;
    maxP: number;
}

export interface ShuntCompensatorCreation extends InjectionCreationFields {
    type: 'SHUNT_COMPENSATOR_CREATION';
    sectionCount: number;
    maximumSectionCount: number;
    maxSusceptance: number;
}

export interface StaticVarCompensatorCreation extends InjectionCreationFields {
    type: 'STATIC_VAR_COMPENSATOR_CREATION';
    minSusceptance: number;
    maxSusceptance: number;
    regulationMode: string;
    isRegulating: boolean;
    voltageSetpoint?: number;
    reactivePowerSetpoint?: number;
}

export interface ConverterStationCreation extends InjectionCreationFields {
    type: 'CONVERTER_STATION_CREATION';
    lossFactor: number;
    voltageRegulationOn: boolean;
    voltageSetpoint?: number;
    reactivePowerSetpoint?: number;
}

export interface LccConverterStationCreation extends InjectionCreationFields {
    type: 'LCC_CONVERTER_STATION_CREATION';
    lossFactor: number;
    powerFactor: number;
}

/** Proposal. */
export interface BoundaryLineCreation extends InjectionCreationFields {
    type: 'BOUNDARY_LINE_CREATION';
    p0: number;
    q0: number;
    r: number;
    x: number;
    g: number;
    b: number;
}

export type InjectionCreation =
    | LoadCreation
    | GeneratorCreation
    | BatteryCreation
    | ShuntCompensatorCreation
    | StaticVarCompensatorCreation
    | ConverterStationCreation
    | LccConverterStationCreation
    | BoundaryLineCreation;

export interface EquipmentModification {
    type: `${string}_MODIFICATION`;
    equipmentId: string;
    [field: string]: string | AttributeModification<number | string | boolean>;
}

export interface EquipmentAttributeModification {
    type: 'EQUIPMENT_ATTRIBUTE_MODIFICATION';
    equipmentId: string;
    equipmentType: 'SWITCH';
    equipmentAttributeName: 'open';
    equipmentAttributeValue: boolean;
}

export interface EquipmentDeletion {
    type: 'EQUIPMENT_DELETION' | 'ELEMENT_DELETION';
    equipmentId: string;
    equipmentType: string;
}

export interface MoveFeederBay {
    equipmentId: string;
    busbarSectionId: string | null;
    connectionSide: string | null;
    connectionPosition: number | null;
    connectionName: string | null;
    connectionDirection: ConnectionDirection | null;
}

export interface MoveVoltageLevelFeederBays {
    type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS';
    voltageLevelId: string;
    feederBays: MoveFeederBay[];
}

export interface NodeInjectionCreation {
    type: 'NODE_INJECTION_CREATION';
    voltageLevelId: string;
    node: number;
    creation: Omit<InjectionCreation, 'busOrBusbarSectionId' | 'connectionPosition' | 'connectionDirection'>;
}

export interface SwitchedInjectionCreation {
    type: 'SWITCHED_INJECTION_CREATION';
    voltageLevelId: string;
    node: number;
    switchId: string;
    switchKind: SwitchKind;
    creation: NodeInjectionCreation['creation'];
}

export type SwitchKind = 'BREAKER' | 'DISCONNECTOR' | 'LOAD_BREAK_SWITCH';

export interface SwitchCreation {
    type: 'SWITCH_CREATION';
    equipmentId: string;
    voltageLevelId: string;
    switchKind: SwitchKind;
    node1: number;
    node2: number;
    open: boolean;
}

export interface EquipmentRename {
    type: 'EQUIPMENT_RENAME';
    equipmentId: string;
    newEquipmentId: string;
    newConnectionName?: string;
}

export type NetworkModification =
    | InjectionCreation
    | EquipmentModification
    | EquipmentAttributeModification
    | EquipmentDeletion
    | MoveVoltageLevelFeederBays
    | NodeInjectionCreation
    | SwitchedInjectionCreation
    | SwitchCreation
    | EquipmentRename;

const CREATION_TYPES: Partial<Record<ElementType, InjectionCreation['type']>> = {
    LOAD: 'LOAD_CREATION',
    GENERATOR: 'GENERATOR_CREATION',
    BATTERY: 'BATTERY_CREATION',
    SHUNT: 'SHUNT_COMPENSATOR_CREATION',
    STATIC_VAR_COMPENSATOR: 'STATIC_VAR_COMPENSATOR_CREATION',
    VSC_CONVERTER_STATION: 'CONVERTER_STATION_CREATION',
    LCC_CONVERTER_STATION: 'LCC_CONVERTER_STATION_CREATION',
    BOUNDARY_LINE: 'BOUNDARY_LINE_CREATION',
};

const MODIFICATION_TYPES: Partial<Record<ElementType, EquipmentModification['type']>> = {
    VSC_CONVERTER_STATION: 'CONVERTER_STATION_MODIFICATION',
    LCC_CONVERTER_STATION: 'LCC_CONVERTER_STATION_MODIFICATION',
};

const IDENTIFIABLE_TYPES: Record<ElementType, string> = {
    BUS: 'BUSBAR_SECTION',
    LOAD: 'LOAD',
    GENERATOR: 'GENERATOR',
    BATTERY: 'BATTERY',
    SHUNT: 'SHUNT_COMPENSATOR',
    STATIC_VAR_COMPENSATOR: 'STATIC_VAR_COMPENSATOR',
    VSC_CONVERTER_STATION: 'HVDC_CONVERTER_STATION',
    LCC_CONVERTER_STATION: 'HVDC_CONVERTER_STATION',
    BOUNDARY_LINE: 'BOUNDARY_LINE',
    GROUND: 'GROUND',
    LINE: 'LINE',
    TWO_WINDINGS_TRANSFORMER: 'TWO_WINDINGS_TRANSFORMER',
    THREE_WINDINGS_TRANSFORMER: 'THREE_WINDINGS_TRANSFORMER',
    BREAKER: 'SWITCH',
    DISCONNECTOR: 'SWITCH',
    LOAD_BREAK_SWITCH: 'SWITCH',
    UNKNOWN: 'UNKNOWN',
};

export function identifiableType(type: ElementType): string {
    return IDENTIFIABLE_TYPES[type];
}

function creationType(type: ElementType): string {
    return CREATION_TYPES[type] ?? `${identifiableType(type)}_CREATION`;
}

export function switchKind(type: ElementType): SwitchKind {
    return type === 'DISCONNECTOR' || type === 'LOAD_BREAK_SWITCH' ? type : 'BREAKER';
}

export interface FeederPlacement {
    voltageLevelId: string;
    busbarSectionId: string;
    order: number | null;
    direction: FeederDirection | undefined;
}

export function injectionCreation(
    type: ElementType,
    equipmentId: string,
    placement: FeederPlacement,
    properties: EquipmentProperties,
): InjectionCreation {
    return {
        ...injectionFields(type, equipmentId, placement.voltageLevelId, properties),
        busOrBusbarSectionId: placement.busbarSectionId,
        connectionDirection: placement.direction ?? null,
        connectionPosition: placement.order,
    } as InjectionCreation;
}

export function injectionFields(
    type: ElementType,
    equipmentId: string,
    voltageLevelId: string,
    properties: EquipmentProperties,
): NodeInjectionCreation['creation'] {
    return {
        type: creationType(type),
        equipmentId,
        equipmentName: null,
        voltageLevelId,
        connectionName: equipmentId,
        terminalConnected: true,
        ...properties,
    } as NodeInjectionCreation['creation'];
}

export function nodeInjectionCreation(
    type: ElementType,
    equipmentId: string,
    voltageLevelId: string,
    node: number,
    properties: EquipmentProperties,
): NodeInjectionCreation {
    return {
        type: 'NODE_INJECTION_CREATION',
        voltageLevelId,
        node,
        creation: injectionFields(type, equipmentId, voltageLevelId, properties),
    };
}

export function equipmentModification(
    type: ElementType,
    equipmentId: string,
    changes: EquipmentProperties,
): NetworkModification {
    if (identifiableType(type) === 'SWITCH' && typeof changes.open === 'boolean') {
        return {
            type: 'EQUIPMENT_ATTRIBUTE_MODIFICATION',
            equipmentId,
            equipmentType: 'SWITCH',
            equipmentAttributeName: 'open',
            equipmentAttributeValue: changes.open,
        };
    }
    const modification: EquipmentModification = {
        type: MODIFICATION_TYPES[type] ?? `${identifiableType(type)}_MODIFICATION`,
        equipmentId,
    };
    for (const [field, value] of Object.entries(changes)) {
        modification[field] = { value, op: 'SET' };
    }
    return modification;
}

export function moveFeederBay(
    voltageLevelId: string,
    equipmentId: string,
    busbarSectionId: string | null,
    order: number | undefined,
    direction: FeederDirection | undefined,
    connectionSide: string | null,
): MoveVoltageLevelFeederBays {
    return {
        type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
        voltageLevelId,
        feederBays: [
            {
                equipmentId,
                busbarSectionId,
                connectionSide,
                connectionPosition: order ?? null,
                connectionName: null,
                connectionDirection: direction ?? null,
            },
        ],
    };
}

export function mergeFeederBayMoves(modifications: readonly NetworkModification[]): NetworkModification[] {
    const merged: NetworkModification[] = [];
    const movesByVoltageLevel = new Map<string, MoveVoltageLevelFeederBays>();

    for (const modification of modifications) {
        if (modification.type !== 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS') {
            merged.push(modification);
            continue;
        }
        const existing = movesByVoltageLevel.get(modification.voltageLevelId);
        if (existing) {
            existing.feederBays.push(...modification.feederBays);
            continue;
        }
        const copy = { ...modification, feederBays: [...modification.feederBays] };
        movesByVoltageLevel.set(modification.voltageLevelId, copy);
        merged.push(copy);
    }
    return merged;
}
