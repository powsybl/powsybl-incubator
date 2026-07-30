import type {
    SLDMetadata,
    OnNextVoltageCallbackType,
    OnBreakerCallbackType,
    OnFeederCallbackType,
    OnBusCallbackType,
    OnToggleSldHoverCallbackType,
} from '@powsybl/network-viewer-core';

export type { SLDMetadata };

export type EquipmentProperties = Record<string, number | string | boolean>;

export type ElementType =
    | 'BUS'
    | 'LOAD'
    | 'GENERATOR'
    | 'LINE'
    | 'SHUNT'
    | 'VSC_CONVERTER_STATION'
    | 'LCC_CONVERTER_STATION'
    | 'BREAKER'
    | 'DISCONNECTOR'
    | 'LOAD_BREAK_SWITCH'
    | 'THREE_WINDINGS_TRANSFORMER'
    | 'TWO_WINDINGS_TRANSFORMER'
    | 'UNKNOWN';

export const DELETABLE_BAY_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    'LOAD',
    'GENERATOR',
    'TWO_WINDINGS_TRANSFORMER',
    'THREE_WINDINGS_TRANSFORMER',
    'SHUNT',
    'VSC_CONVERTER_STATION',
    'LCC_CONVERTER_STATION',
    'LINE'
]);

export const DELETABLE_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    'BREAKER',
    'DISCONNECTOR',
    'LOAD_BREAK_SWITCH',
    ...DELETABLE_BAY_TYPES,
]);

export const SELECTABLE_TYPES: ReadonlySet<ElementType> = DELETABLE_TYPES;
export const SELECTED_CLASS = 'ne-selected';

export const CREATABLE_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    'LOAD',
    'GENERATOR',
]);

export const CONNECTION_POINT_CLASS = 'ne-connection-point';


export const COMPONENT_TYPE_MAP: Readonly<Record<string, ElementType>> = {
    BUSBAR_SECTION: 'BUS',
    LOAD: 'LOAD',
    GENERATOR: 'GENERATOR',
    LINE: 'LINE',
    BOUNDARY_LINE: 'LINE',
    CAPACITOR: 'SHUNT',
    INDUCTOR: 'SHUNT',
    VSC_CONVERTER_STATION: 'VSC_CONVERTER_STATION',
    LCC_CONVERTER_STATION: 'LCC_CONVERTER_STATION',
    BREAKER: 'BREAKER',
    DISCONNECTOR: 'DISCONNECTOR',
    LOAD_BREAK_SWITCH: 'LOAD_BREAK_SWITCH',
    THREE_WINDINGS_TRANSFORMER: 'THREE_WINDINGS_TRANSFORMER',
    THREE_WINDINGS_TRANSFORMER_LEG: 'THREE_WINDINGS_TRANSFORMER',
    TWO_WINDINGS_TRANSFORMER: 'TWO_WINDINGS_TRANSFORMER',
    TWO_WINDINGS_TRANSFORMER_LEG: 'TWO_WINDINGS_TRANSFORMER',
    PHASE_SHIFT_TRANSFORMER: 'TWO_WINDINGS_TRANSFORMER',
    PHASE_SHIFT_TRANSFORMER_LEG: 'TWO_WINDINGS_TRANSFORMER',
};

export function toElementType(componentType: string | undefined): ElementType {
    if (!componentType) return 'UNKNOWN';
    return COMPONENT_TYPE_MAP[componentType] ?? 'UNKNOWN';
}


export interface NodeMetadata {
    id: string;
    equipmentId?: string;
    iidmNode?: number;
    componentType: string;
    vid?: string;
    nextVId?: string;
    direction?: string;
    open?: boolean;
    vlabel?: boolean;
    rotationAngle?: number;
    labels?: unknown[];
}

export interface WireMetadata {
    id: string;
    nodeId1: string;
    nodeId2: string;
    straight?: boolean;
    snakeLine?: boolean;
}

export interface FeederInfoMetadata {
    id: string;
    equipmentId: string;
    componentType: string;
}

export interface DeleteScope {
    nodes: NodeMetadata[];
    wires: WireMetadata[];
}

export interface EquipmentInfo {
    equipmentId: string;
    type: ElementType;
    label: string;
    deletable: boolean;
    bayDeletable: boolean;
}

export interface EquipmentContextMenuEvent {
    info: EquipmentInfo;
    position: { x: number; y: number };
}

export interface ConnectionAnchor {
    kind: 'FREE_NODE';
    vlId: string;
    attachedTo: string;
    node?: number;
}

export interface ConnectionPoint {
    id: string;
    anchor: ConnectionAnchor;
}

export interface CreateEquipmentSpec {
    type: ElementType;
    properties: EquipmentProperties;
    provisionalId?: string;
}

export interface ConnectionPointClickEvent {
    point: ConnectionPoint;
    position: { x: number; y: number };
}

export interface ViewerCallbacks {
    onNextVoltage?: OnNextVoltageCallbackType;
    onBreaker?: OnBreakerCallbackType;
    onFeeder?: OnFeederCallbackType;
    onBus?: OnBusCallbackType;
    onToggleHover?: OnToggleSldHoverCallbackType;
}

export interface EditorMetadata extends Omit<SLDMetadata, 'nodes' | 'wires'> {
    nodes: NodeMetadata[];
    wires: WireMetadata[];
    feederInfos?: FeederInfoMetadata[];
}

export interface EditorOptions {
    container: HTMLElement;
    svgContent: string;
    metadata: SLDMetadata;
    svgType?: string;
    minWidth?: number;
    minHeight?: number;
    maxWidth?: number;
    maxHeight?: number;
    selectionBackColor?: string;
    callbacks?: ViewerCallbacks;
    onEvent?: EditorEventListener;
    onEquipmentContextMenu?: (event: EquipmentContextMenuEvent) => void;
    onConnectionPointClick?: (event: ConnectionPointClickEvent) => void;
    initialProperties?: Record<string, EquipmentProperties>;
}

export const EDITOR_OPTION_DEFAULTS = {
    svgType: 'SLD',
    minWidth: 200,
    minHeight: 200,
    maxWidth: 2000,
    maxHeight: 2000,
    selectionBackColor: 'white',
} as const;

export type ChangeOp = 'create' | 'delete' | 'update' | 'delete-bay';

export interface ChangeSetEntry {
    op: ChangeOp;
    equipmentType: ElementType;
    equipmentId: string;
    payload?: Record<string, unknown>;
}

export type ChangeSet = ChangeSetEntry[];

export interface EditorEvents {
    'element:added': { id: string; type: ElementType; voltageLevelId: string };
    'element:created': { id: string; type: ElementType; anchor: ConnectionAnchor };
    'element:removed': { id: string; type: ElementType };
    'connection:changed': { points: readonly ConnectionPoint[] };
    'element:selected': { id: string | null ; type: ElementType | null };
    'properties:changed': { id: string; changes: Record<string, unknown> };
    'history:changed': { canUndo: boolean; canRedo: boolean };
    'model:changed': { changeSet: ChangeSet };
}

export type EditorEventName = keyof EditorEvents;

export type EditorEventListener = <K extends EditorEventName>(
    name: K,
    payload: EditorEvents[K],
) => void;

export const SWITCH_TYPES: ReadonlySet<string> = new Set([
    'BREAKER',
    'DISCONNECTOR',
    'LOAD_BREAK_SWITCH',
]);

export const HIDDEN_NODE_TYPE = 'NODE';

export const BAY_TRAVERSABLE_TYPES: ReadonlySet<string> = new Set([
    HIDDEN_NODE_TYPE,
    ...SWITCH_TYPES,
]);

