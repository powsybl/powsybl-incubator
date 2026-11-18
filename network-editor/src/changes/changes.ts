import type { ElementType, EquipmentProperties, FeederDirection } from '../types.ts';

/**
 * The change set emitted by the editor: what the user did on the diagram, in the PowSyBl vocabulary.
 * Each `kind` is named after its powsybl-iidm-modification class when there is one (CreateFeederBay,
 * CreateCouplingDevice, RemoveFeederBay, MoveFeederBay), and its fields after the builder parameters or the
 * pypowsybl columns. Attribute names are the IIDM ones (p0, targetP, voltageRegulatorOn...).
 * See docs/format-change-set.md.
 */
export const CHANGE_FORMAT_VERSION = 1;

export type EquipmentType =
    | 'LOAD'
    | 'GENERATOR'
    | 'BATTERY'
    | 'SHUNT_COMPENSATOR'
    | 'STATIC_VAR_COMPENSATOR'
    | 'VSC_CONVERTER_STATION'
    | 'LCC_CONVERTER_STATION'
    | 'BOUNDARY_LINE'
    | 'GROUND'
    | 'LINE'
    | 'TWO_WINDINGS_TRANSFORMER'
    | 'THREE_WINDINGS_TRANSFORMER'
    | 'SWITCH'
    | 'BUSBAR_SECTION';

export type SwitchKind = 'BREAKER' | 'DISCONNECTOR' | 'LOAD_BREAK_SWITCH';

/** IIDM attribute values by attribute name. */
export type Attributes = EquipmentProperties;

/** Host data the editor does not interpret: carried and merged as is, ignored by a PowSyBl backend. */
export type Extra = Record<string, unknown>;

/** A new injection in a new feeder bay on a busbar section: CreateFeederBay, pypowsybl `create_<type>_bay`. */
export interface CreateFeederBay {
    kind: 'CREATE_FEEDER_BAY';
    equipmentType: EquipmentType;
    id: string;
    voltageLevelId: string;
    busOrBusbarSectionId: string;
    positionOrder?: number;
    direction?: FeederDirection;
    attributes: Attributes;
    extra?: Extra;
}

/** A new injection on an existing node, or behind a new switch from that node: IIDM adder `setNode`. */
export interface CreateInjection {
    kind: 'CREATE_INJECTION';
    equipmentType: EquipmentType;
    id: string;
    voltageLevelId: string;
    node: number;
    switch?: { id: string; kind: SwitchKind };
    attributes: Attributes;
    extra?: Extra;
}

/** A new switch between two existing nodes: `NodeBreakerView.newSwitch()`, pypowsybl `create_switches`. */
export interface CreateSwitch {
    kind: 'CREATE_SWITCH';
    id: string;
    voltageLevelId: string;
    switchKind: SwitchKind;
    node1: number;
    node2: number;
    open: boolean;
}

/** A new busbar section, placed with BusbarSectionPosition: pypowsybl `create_busbar_sections`. */
export interface CreateBusbarSection {
    kind: 'CREATE_BUSBAR_SECTION';
    id: string;
    voltageLevelId: string;
    busbarIndex: number;
    sectionIndex: number;
}

/** CreateCouplingDevice, pypowsybl `create_coupling_device`. */
export interface CreateCouplingDevice {
    kind: 'CREATE_COUPLING_DEVICE';
    voltageLevelId: string;
    busOrBusbarSectionId1: string;
    busOrBusbarSectionId2: string;
}

/** RemoveFeederBay, pypowsybl `remove_feeder_bays`: the connectable and its switches. */
export interface RemoveFeederBay {
    kind: 'REMOVE_FEEDER_BAY';
    connectableId: string;
    equipmentType?: EquipmentType;
}

/** One identifiable alone: `remove()`, pypowsybl `remove_elements`. */
export interface RemoveElement {
    kind: 'REMOVE_ELEMENT';
    id: string;
    equipmentType?: EquipmentType;
}

/** MoveFeederBay to another busbar section, and/or a new ConnectablePosition (order, direction). */
export interface MoveFeederBay {
    kind: 'MOVE_FEEDER_BAY';
    connectableId: string;
    targetVoltageLevelId: string;
    targetBusOrBusbarSectionId?: string;
    positionOrder?: number;
    direction?: FeederDirection;
    side?: string;
}

/** IIDM setters, pypowsybl `update_<type>s`. A switch opens or closes through `open`. */
export interface UpdateAttributes {
    kind: 'UPDATE_ATTRIBUTES';
    equipmentType?: EquipmentType;
    id: string;
    attributes: Attributes;
    extra?: Extra;
}

/** Specific to the editor: IIDM has no id change, the backend decides how to apply it. */
export interface Rename {
    kind: 'RENAME';
    id: string;
    newId: string;
}

export type NetworkChange =
    | CreateFeederBay
    | CreateInjection
    | CreateSwitch
    | CreateBusbarSection
    | CreateCouplingDevice
    | RemoveFeederBay
    | RemoveElement
    | MoveFeederBay
    | UpdateAttributes
    | Rename;

const EQUIPMENT_TYPES: Record<Exclude<ElementType, 'UNKNOWN'>, EquipmentType> = {
    BUS: 'BUSBAR_SECTION',
    LOAD: 'LOAD',
    GENERATOR: 'GENERATOR',
    BATTERY: 'BATTERY',
    SHUNT: 'SHUNT_COMPENSATOR',
    STATIC_VAR_COMPENSATOR: 'STATIC_VAR_COMPENSATOR',
    VSC_CONVERTER_STATION: 'VSC_CONVERTER_STATION',
    LCC_CONVERTER_STATION: 'LCC_CONVERTER_STATION',
    BOUNDARY_LINE: 'BOUNDARY_LINE',
    GROUND: 'GROUND',
    LINE: 'LINE',
    TWO_WINDINGS_TRANSFORMER: 'TWO_WINDINGS_TRANSFORMER',
    THREE_WINDINGS_TRANSFORMER: 'THREE_WINDINGS_TRANSFORMER',
    BREAKER: 'SWITCH',
    DISCONNECTOR: 'SWITCH',
    LOAD_BREAK_SWITCH: 'SWITCH',
};

/** The IIDM type of an element, undefined when the diagram does not tell it. */
export function equipmentTypeOf(type: ElementType): EquipmentType | undefined {
    return type === 'UNKNOWN' ? undefined : EQUIPMENT_TYPES[type];
}

export function switchKind(type: ElementType): SwitchKind {
    return type === 'DISCONNECTOR' || type === 'LOAD_BREAK_SWITCH' ? type : 'BREAKER';
}

export interface FeederBayPlacement {
    voltageLevelId: string;
    busbarSectionId: string;
    order?: number;
    direction?: FeederDirection;
}

export function createFeederBay(
    type: ElementType,
    id: string,
    placement: FeederBayPlacement,
    attributes: Attributes,
    extra: Extra = {},
): CreateFeederBay {
    return withExtra<CreateFeederBay>({
        kind: 'CREATE_FEEDER_BAY',
        equipmentType: equipmentTypeOf(type) as EquipmentType,
        id,
        voltageLevelId: placement.voltageLevelId,
        busOrBusbarSectionId: placement.busbarSectionId,
        ...defined({ positionOrder: placement.order, direction: placement.direction }),
        attributes: { ...attributes },
    }, extra);
}

export function createInjection(
    type: ElementType,
    id: string,
    voltageLevelId: string,
    node: number,
    attributes: Attributes,
    extra: Extra = {},
    behindSwitch?: { id: string; type: ElementType },
): CreateInjection {
    return withExtra<CreateInjection>({
        kind: 'CREATE_INJECTION',
        equipmentType: equipmentTypeOf(type) as EquipmentType,
        id,
        voltageLevelId,
        node,
        ...(behindSwitch ? { switch: { id: behindSwitch.id, kind: switchKind(behindSwitch.type) } } : {}),
        attributes: { ...attributes },
    }, extra);
}

export function createSwitch(
    type: ElementType,
    id: string,
    voltageLevelId: string,
    node1: number,
    node2: number,
    open: boolean,
): CreateSwitch {
    return { kind: 'CREATE_SWITCH', id, voltageLevelId, switchKind: switchKind(type), node1, node2, open };
}

export function createBusbarSection(id: string, voltageLevelId: string, busbarIndex: number): CreateBusbarSection {
    return { kind: 'CREATE_BUSBAR_SECTION', id, voltageLevelId, busbarIndex, sectionIndex: 1 };
}

export function createCouplingDevice(
    voltageLevelId: string,
    busbarSectionId1: string,
    busbarSectionId2: string,
): CreateCouplingDevice {
    return {
        kind: 'CREATE_COUPLING_DEVICE',
        voltageLevelId,
        busOrBusbarSectionId1: busbarSectionId1,
        busOrBusbarSectionId2: busbarSectionId2,
    };
}

export function removal(id: string, type: ElementType, withBay: boolean): RemoveFeederBay | RemoveElement {
    const equipmentType = equipmentTypeOf(type);
    return withBay
        ? { kind: 'REMOVE_FEEDER_BAY', connectableId: id, ...defined({ equipmentType }) }
        : { kind: 'REMOVE_ELEMENT', id, ...defined({ equipmentType }) };
}

export function moveFeederBay(
    voltageLevelId: string,
    connectableId: string,
    busbarSectionId: string | null,
    order: number | undefined,
    direction: FeederDirection | undefined,
    side: string | null,
): MoveFeederBay {
    return {
        kind: 'MOVE_FEEDER_BAY',
        connectableId,
        targetVoltageLevelId: voltageLevelId,
        ...defined({
            targetBusOrBusbarSectionId: busbarSectionId ?? undefined,
            positionOrder: order,
            direction,
            side: side ?? undefined,
        }),
    };
}

export function updateAttributes(type: ElementType, id: string, attributes: Attributes, extra: Extra = {}): UpdateAttributes {
    return withExtra<UpdateAttributes>({
        kind: 'UPDATE_ATTRIBUTES',
        ...defined({ equipmentType: equipmentTypeOf(type) }),
        id,
        attributes: { ...attributes },
    }, extra);
}

export function rename(id: string, newId: string): Rename {
    return { kind: 'RENAME', id, newId };
}

/** `extra` only when the host gave something: a PowSyBl backend never sees an empty one. */
export function withExtra<T extends { extra?: Extra }>(change: T, extra: Extra): T {
    return Object.keys(extra).length > 0 ? { ...change, extra: { ...extra } } : change;
}

/** The fields that have a value: optional fields stay absent from the JSON instead of null. */
function defined<T extends Record<string, unknown>>(fields: T): Partial<T> {
    return Object.fromEntries(Object.entries(fields).filter(([, value]) => value !== undefined)) as Partial<T>;
}
