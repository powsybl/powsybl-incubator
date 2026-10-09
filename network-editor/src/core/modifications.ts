import { toDirection, type ElementType, type EquipmentProperties } from './types';

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
    type: 'EQUIPMENT_DELETION';
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

export interface ElementDeletion {
    type: 'ELEMENT_DELETION';
    equipmentId: string;
    equipmentType: string;
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
    | ElementDeletion
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
    LOAD: 'LOAD_MODIFICATION',
    GENERATOR: 'GENERATOR_MODIFICATION',
    BATTERY: 'BATTERY_MODIFICATION',
    SHUNT: 'SHUNT_COMPENSATOR_MODIFICATION',
    VSC_CONVERTER_STATION: 'CONVERTER_STATION_MODIFICATION',
    LCC_CONVERTER_STATION: 'LCC_CONVERTER_STATION_MODIFICATION',
    LINE: 'LINE_MODIFICATION',
    TWO_WINDINGS_TRANSFORMER: 'TWO_WINDINGS_TRANSFORMER_MODIFICATION',
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
    direction: string | undefined;
}

function connectionDirection(direction: string | undefined): ConnectionDirection | null {
    return toDirection(direction) ?? null;
}

export function injectionCreation(
    type: ElementType,
    equipmentId: string,
    placement: FeederPlacement,
    properties: EquipmentProperties,
): InjectionCreation {
    return {
        type: creationType(type),
        equipmentId,
        equipmentName: null,
        voltageLevelId: placement.voltageLevelId,
        busOrBusbarSectionId: placement.busbarSectionId,
        connectionName: equipmentId,
        connectionDirection: connectionDirection(placement.direction),
        connectionPosition: placement.order,
        terminalConnected: true,
        ...properties,
    } as InjectionCreation;
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
        creation: {
            type: creationType(type),
            equipmentId,
            equipmentName: null,
            voltageLevelId,
            connectionName: equipmentId,
            terminalConnected: true,
            ...properties,
        } as NodeInjectionCreation['creation'],
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
    direction: string | undefined,
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
                connectionDirection: connectionDirection(direction),
            },
        ],
    };
}
