import type {
    SLDMetadata,
    OnNextVoltageCallbackType,
    OnBreakerCallbackType,
    OnFeederCallbackType,
    OnBusCallbackType,
    OnToggleSldHoverCallbackType,
} from '@powsybl/network-viewer-core';

export type { SLDMetadata };

export type ElementType =
    | 'BUS'
    | 'LOAD'
    | 'GENERATOR'
    | 'LINE'
    | 'CAPACITOR'
    | 'BREAKER'
    | 'THREE_WINDINGS_TRANSFORMER'
    | 'TWO_WINDINGS_TRANSFORMER'
    | 'UNKNOWN';

export const DELETABLE_BAY_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    'LOAD',
    'GENERATOR',
    'TWO_WINDINGS_TRANSFORMER',
    'THREE_WINDINGS_TRANSFORMER',
    'CAPACITOR',
    'LINE'
]);

export const DELETABLE_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    'BREAKER',
    ...DELETABLE_BAY_TYPES,
]);

export const SELECTABLE_TYPES: ReadonlySet<ElementType> = DELETABLE_TYPES;
export const SELECTED_CLASS = 'ne-selected';


export const COMPONENT_TYPE_MAP: Readonly<Record<string, ElementType>> = {
    BUS: 'BUS',
    BUSBAR_SECTION: 'BUS',
    LOAD: 'LOAD',
    GENERATOR: 'GENERATOR',
    LINE: 'LINE',
    BOUNDARY_LINE: 'LINE',
    CAPACITOR: 'CAPACITOR',
    BREAKER: 'BREAKER',
    DISCONNECTOR: 'BREAKER',
    LOAD_BREAK_SWITCH: 'BREAKER',
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
    type: ElementType;
    label: string;
    deletable: boolean;
    bayDeletable: boolean;
}

export interface EquipmentContextMenuEvent {
    info: EquipmentInfo;
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
    'element:removed': { id: string; type: ElementType };
    'element:selected': { id: string | null ; type: ElementType | null };
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

export const BAY_TRAVERSABLE_TYPES: ReadonlySet<string> = new Set([
    'NODE',            // fictitious internal nodes ("idINTERNAL_…")
    'BUS_CONNECTION',  // busbar attachment points ("idBUSCO_…")
    ...SWITCH_TYPES,
]);

