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

export const SWITCH_TYPES: ReadonlySet<string> = new Set([
    'BREAKER',
    'DISCONNECTOR',
    'LOAD_BREAK_SWITCH',
]);

export const INJECTION_TYPES: ReadonlySet<string> = new Set([
    'LOAD',
    'GENERATOR',
    'BATTERY',
    'CAPACITOR',
    'INDUCTOR',
    'STATIC_VAR_COMPENSATOR',
    'VSC_CONVERTER_STATION',
    'LCC_CONVERTER_STATION',
    'BOUNDARY_LINE',
]);

export const BRANCH_TYPES: ReadonlySet<string> = new Set([
    'LINE',
    'TIE_LINE',
    'HVDC_LINE',
    'TWO_WINDINGS_TRANSFORMER',
    'TWO_WINDINGS_TRANSFORMER_LEG',
    'PHASE_SHIFT_TRANSFORMER',
    'PHASE_SHIFT_TRANSFORMER_LEG',
    'THREE_WINDINGS_TRANSFORMER',
    'THREE_WINDINGS_TRANSFORMER_LEG',
]);

export const DELETABLE_BAY_TYPES: ReadonlySet<string> = new Set([
    ...INJECTION_TYPES,
    ...BRANCH_TYPES,
]);

export const DELETABLE_TYPES: ReadonlySet<string> = new Set([
    ...DELETABLE_BAY_TYPES,
    ...SWITCH_TYPES,
]);

export const SELECTABLE_TYPES: ReadonlySet<string> = DELETABLE_TYPES;
export const SELECTED_CLASS = 'ne-selected';

export const BUSBAR_SECTION_TYPE = 'BUSBAR_SECTION';

export type FeederDirection = 'TOP' | 'BOTTOM';


export interface NodeMetadata {
    id: string;
    equipmentId?: string;
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

export interface EquipmentInfo {
    equipmentId: string;
    componentType: string;
    label: string;
    deletable: boolean;
    bayDeletable: boolean;
}

export interface EquipmentContextMenuEvent {
    info: EquipmentInfo;
    position: { x: number; y: number };
}

export interface BusbarConnectionTarget {
    kind: 'busbar';
    busbarSectionId: string;
    voltageLevelId: string;
    svgId: string;
    direction: FeederDirection;
    position: { x: number; y: number };
    previousEquipmentId?: string;
    nextEquipmentId?: string;
}

export type ConnectionTarget = BusbarConnectionTarget;

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
    initialProperties?: Record<string, EquipmentProperties>;
    connectionPointsInteractive?: boolean;
    connectionPointRadius?: number;
}

export const EDITOR_OPTION_DEFAULTS = {
    svgType: 'SLD',
    minWidth: 200,
    minHeight: 200,
    maxWidth: 2000,
    maxHeight: 2000,
    selectionBackColor: 'white',
    connectionPointsInteractive: true,
    connectionPointRadius: 40,
} as const;

export type ChangeOp = 'create' | 'delete' | 'update' | 'delete-bay';

export interface ChangeSetEntry {
    op: ChangeOp;
    componentType: string;
    equipmentId: string;
    payload?: Record<string, unknown>;
}

export type ChangeSet = ChangeSetEntry[];

export interface EditorEvents {
    'element:added': { id: string; componentType: string; voltageLevelId: string };
    'element:removed': { id: string; componentType: string };
    'element:selected': { id: string | null; componentType: string | null };
    'connection-point:picked': ConnectionTarget;
    'properties:changed': { id: string; changes: Record<string, unknown> };
    'history:changed': { canUndo: boolean; canRedo: boolean };
    'model:changed': { changeSet: ChangeSet };
}

export type EditorEventName = keyof EditorEvents;

export type EditorEventListener = <K extends EditorEventName>(
    name: K,
    payload: EditorEvents[K],
) => void;

export const BAY_TRAVERSABLE_TYPES: ReadonlySet<string> = new Set([
    'NODE',            // fictitious internal nodes ("idINTERNAL_…")
    'BUS_CONNECTION',  // busbar attachment points ("idBUSCO_…")
    ...SWITCH_TYPES,
]);

export function isFeederNode(node: NodeMetadata): boolean {
    return node.direction === 'TOP' || node.direction === 'BOTTOM';
}

