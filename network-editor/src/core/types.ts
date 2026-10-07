import type {
    SLDMetadata,
    OnNextVoltageCallbackType,
    OnBreakerCallbackType,
    OnFeederCallbackType,
    OnBusCallbackType,
    OnToggleSldHoverCallbackType,
    SldComponentOptions,
} from '@powsybl/network-viewer-core';

export type { SLDMetadata };

export type EquipmentProperties = Record<string, number | string | boolean>;

export type ElementType =
    | 'BUS'
    | 'LOAD'
    | 'GENERATOR'
    | 'BATTERY'
    | 'SHUNT'
    | 'STATIC_VAR_COMPENSATOR'
    | 'VSC_CONVERTER_STATION'
    | 'LCC_CONVERTER_STATION'
    | 'BOUNDARY_LINE'
    | 'GROUND'
    | 'LINE'
    | 'TWO_WINDINGS_TRANSFORMER'
    | 'THREE_WINDINGS_TRANSFORMER'
    | 'BREAKER'
    | 'DISCONNECTOR'
    | 'LOAD_BREAK_SWITCH'
    | 'UNKNOWN';

export const INJECTION_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    'LOAD',
    'GENERATOR',
    'BATTERY',
    'SHUNT',
    'STATIC_VAR_COMPENSATOR',
    'VSC_CONVERTER_STATION',
    'LCC_CONVERTER_STATION',
    'BOUNDARY_LINE',
]);

export const BUSBAR_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    'BUS',
]);

export const BRANCH_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    'LINE',
    'TWO_WINDINGS_TRANSFORMER',
    'THREE_WINDINGS_TRANSFORMER',
]);

export const SWITCH_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    'BREAKER',
    'DISCONNECTOR',
    'LOAD_BREAK_SWITCH',
]);

export const DELETABLE_BAY_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    ...INJECTION_TYPES,
    ...BRANCH_TYPES,
]);

export const DELETABLE_TYPES: ReadonlySet<ElementType> = new Set<ElementType>([
    ...DELETABLE_BAY_TYPES,
    ...SWITCH_TYPES,
]);

export const SELECTED_CLASS = 'ne-selected';

export const NODE_TARGET_CLASS = 'ne-node-target';

export const PENDING_CREATE_CLASS = 'ne-pending-create';

export const SWITCH_START_CLASS = 'ne-switch-start';

export const SWITCH_END_CLASS = 'ne-switch-end';

export const BAY_SLOT_CLASS = 'ne-bay-slot';

export const COMPONENT_TYPE_MAP: Readonly<Record<string, ElementType>> = {
    BUSBAR_SECTION: 'BUS',
    LOAD: 'LOAD',
    GENERATOR: 'GENERATOR',
    BATTERY: 'BATTERY',
    LINE: 'LINE',
    BOUNDARY_LINE: 'BOUNDARY_LINE',
    CAPACITOR: 'SHUNT',
    INDUCTOR: 'SHUNT',
    STATIC_VAR_COMPENSATOR: 'STATIC_VAR_COMPENSATOR',
    GROUND: 'GROUND',
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

export type FeederDirection = 'TOP' | 'BOTTOM';

export function toDirection(direction: string | undefined): FeederDirection | undefined {
    return direction === 'TOP' || direction === 'BOTTOM' ? direction : undefined;
}

export type SwitchEnd = NodeTarget | BusbarTarget;

export interface SwitchGesture {
    first: NodeTarget;
    candidates: readonly SwitchEnd[];
    type: ElementType;
    second?: SwitchEnd;
}

export type PickedSwitch = SwitchGesture & { second: SwitchEnd };

export interface SwitchEnds {
    first: string;
    second: string;
}

export interface DiagramPoint {
    x: number;
    y: number;
}

export interface DiagramSpan {
    left: number;
    right: number;
    y: number;
}

export interface BaySlotCandidate {
    order: number;
    x: number;
    y: number;
}

export interface BayMoveGesture {
    equipmentId: string;
    direction: FeederDirection;
    candidates: readonly BaySlotCandidate[];
}

export type Gesture =
    | ({ kind: 'SWITCH' } & SwitchGesture)
    | ({ kind: 'BAY_MOVE' } & BayMoveGesture);

export interface NodeMetadata {
    id: string;
    equipmentId?: string;
    iidmNode?: number;
    iidmNode1?: number;
    iidmNode2?: number;
    order?: number;
    busbarIndex?: number;
    sectionIndex?: number;
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

export interface NodeDiagnostic {
    iidmNode?: number;
    hidden: boolean;
}

export interface NodeTarget {
    kind: 'NODE';
    id: string;
    vlId: string;
    node: number;
    occupied?: boolean;
}

export interface BaySlot {
    vlId: string;
    sectionIndex: number;
}

export interface BusbarTarget extends BaySlot {
    kind: 'BUSBAR';
    id: string;
    busbarSectionId: string;
    busbarIndex: number;
    node: number;
}

export interface EquipmentTarget {
    kind: 'EQUIPMENT';
    id: string;
    vlId: string;
    equipmentId: string;
    type: ElementType;
    node?: number;
    order?: number;
    direction?: FeederDirection;
    claimed?: boolean;
    created?: boolean;
    createdBy?: EditOperation;
    hostNodeId?: string;
}

export type EditTarget = NodeTarget | BusbarTarget | EquipmentTarget;

export interface CreationPlacement {
    elementType: ElementType;
    vlId: string;
    busbarSectionId?: string;
    node?: number;
    order?: number;
    direction?: FeederDirection;
}

export type EditOperation =
    | 'CREATE_INJECTION'
    | 'CREATE_SWITCH'
    | 'CREATE_SWITCHED_INJECTION'
    | 'CREATE_FEEDER_BAY'
    | 'CREATE_BUSBAR'
    | 'DELETE'
    | 'DELETE_BAY'
    | 'REPLACE'
    | 'UPDATE_PROPERTIES'
    | 'UPDATE_BAY_POSITION'
    | 'FLIP_BAY_DIRECTION'
    | 'MOVE_BAY'
    | 'RENAME';

export const ORDER_STEP = 10;

/** powsybl-diagram default for `LayoutParameters.cellWidth`. */
export const DEFAULT_CELL_WIDTH = 50;

export interface BayPosition {
    order: number;
    direction: FeederDirection;
}


export interface PendingOrders {
    claims: readonly OrderClaim[];
    vacated: ReadonlySet<string>;
}

export const NO_PENDING_ORDERS: PendingOrders = { claims: [], vacated: new Set() };

export interface OrderClaim extends BaySlot {
    iidmNode?: number;
    direction?: FeederDirection;
    order: number;
    vacatedNodeId?: string;
    x?: number;
}

export interface CreateSpec {
    type: ElementType;
    properties: EquipmentProperties;
    provisionalId: string;
    direction?: FeederDirection;
    order?: number;
    switchType?: ElementType;
}

export interface TargetEvent {
    targets: readonly EditTarget[];
    trigger: 'click' | 'contextmenu';
    position: { x: number; y: number };
    insertion?: number;
}

export interface ViewerCallbacks {
    onNextVoltage?: OnNextVoltageCallbackType;
    onBreaker?: OnBreakerCallbackType;
    onFeeder?: OnFeederCallbackType;
    onBus?: OnBusCallbackType;
    onToggleHover?: OnToggleSldHoverCallbackType;
}

/** The part of powsybl-diagram `LayoutParameters` the editor reads. */
export interface LayoutParams {
    cellWidth: number;
}

export interface EditorMetadata extends Omit<SLDMetadata, 'nodes' | 'wires' | 'layoutParams'> {
    nodes: NodeMetadata[];
    wires: WireMetadata[];
    feederInfos?: FeederInfoMetadata[];
    layoutParams?: Partial<LayoutParams>;
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
    onTargets?: (event: TargetEvent) => void;
    initialProperties?: Record<string, EquipmentProperties>;
}

export const EDITOR_OPTION_DEFAULTS = {
    svgType: 'voltage-level',
    minWidth: 200,
    minHeight: 200,
    maxWidth: 2000,
    maxHeight: 2000,
    selectionBackColor: 'white',
} as const;

export type ChangeOp = ChangeSetEntry['op'];

export type ChangeSetEntry =
    | {
          op: 'create';
          equipmentId: string;
          payload: {
              equipmentType: ElementType;
              vlId: string;
              node: number;
              order?: number;
              direction?: FeederDirection;
              properties: EquipmentProperties;
          };
      }
    | {
          op: 'create-bay';
          equipmentId: string;
          payload: {
              equipmentType: ElementType;
              busbarSectionId: string;
              order: number;
              direction: FeederDirection;
              properties: EquipmentProperties;
          };
      }
    | {
          op: 'create-switch';
          equipmentId: string;
          payload: {
              equipmentType: ElementType;
              vlId: string;
              node1: number;
              node2: number;
              properties: EquipmentProperties;
          };
      }
    | {
          op: 'create-switched-injection';
          equipmentId: string;
          payload: {
              equipmentType: ElementType;
              vlId: string;
              node: number;
              switchType: ElementType;
              switchId: string;
              properties: EquipmentProperties;
          };
      }
    | {
          op: 'move-bay';
          equipmentId: string;
          payload: { node: number; targetBusbarSectionId: string; order?: number; direction?: FeederDirection };
      }
    | {
          op: 'update-position';
          equipmentId: string;
          payload: { node: number; order: number; direction: FeederDirection };
      }
    | {
          op: 'update';
          equipmentId: string;
          equipmentType: ElementType;
          payload: EquipmentProperties;
      }

      | {
          op: 'replace';
          equipmentId: string;
          payload: {
              equipmentType: ElementType;
              newId: string;
              properties: EquipmentProperties;
          };
      }

    | { op: 'delete' | 'delete-bay'; equipmentId: string; equipmentType: ElementType, order?: number, direction?: FeederDirection }
    | { op: 'rename'; equipmentId: string; payload: { newId: string } };

export type ChangeSet = ChangeSetEntry[];

export interface SelectedElement {
    id: string;
    type: ElementType;
}

export interface EditorEvents {
    'targets:changed': { targets: readonly EditTarget[] };
    'element:selected': { elements: readonly SelectedElement[] };
    'history:changed': { canUndo: boolean; canRedo: boolean };
    'model:changed': { changeSet: ChangeSet };
    'gesture:changed': { gesture: Gesture | null };
}

export type EditorEventName = keyof EditorEvents;

export type EditorEvent = {
    [K in EditorEventName]: { name: K } & EditorEvents[K];
}[EditorEventName];

export type EditorEventListener = (event: EditorEvent) => void;

export type EditorEmit = <K extends EditorEventName>(
    name: K,
    payload: EditorEvents[K],
) => void;

export const HIDDEN_NODE_TYPE = 'NODE';

export const BAY_TRAVERSABLE_TYPES: ReadonlySet<string> = new Set([
    HIDDEN_NODE_TYPE,
    ...SWITCH_TYPES,
]);

export const BUSBAR_SECTION_TYPE = 'BUSBAR_SECTION';

export const NODE_COMPONENT_TYPE: Readonly<Record<ElementType, string>> = {
    BUS: BUSBAR_SECTION_TYPE,
    LOAD: 'LOAD',
    GENERATOR: 'GENERATOR',
    BATTERY: 'BATTERY',
    SHUNT: 'CAPACITOR',
    STATIC_VAR_COMPENSATOR: 'STATIC_VAR_COMPENSATOR',
    VSC_CONVERTER_STATION: 'VSC_CONVERTER_STATION',
    LCC_CONVERTER_STATION: 'LCC_CONVERTER_STATION',
    BOUNDARY_LINE: 'BOUNDARY_LINE',
    GROUND: 'GROUND',
    LINE: 'LINE',
    TWO_WINDINGS_TRANSFORMER: 'TWO_WINDINGS_TRANSFORMER',
    THREE_WINDINGS_TRANSFORMER: 'THREE_WINDINGS_TRANSFORMER',
    BREAKER: 'BREAKER',
    DISCONNECTOR: 'DISCONNECTOR',
    LOAD_BREAK_SWITCH: 'LOAD_BREAK_SWITCH',
    UNKNOWN: HIDDEN_NODE_TYPE,
};

export function nodeComponentType(type: ElementType, properties: EquipmentProperties = {}): string {
    if (type === 'SHUNT') {
        const bPerSection = properties.bPerSection;
        return typeof bPerSection === 'number' && bPerSection < 0 ? 'INDUCTOR' : 'CAPACITOR';
    }
    return NODE_COMPONENT_TYPE[type];
}

export interface SymbolProvider {
    componentSize(componentType: string): { width: number; height: number };
    createSymbol(componentType: string, options?: SldComponentOptions): SVGGElement;
}

const CREATED_NODE_PREFIX = 'ne-';

export function createdNodeId(equipmentId: string): string {
    return `${CREATED_NODE_PREFIX}${equipmentId}`;
}

export function isCreatedNodeId(nodeId: string): boolean {
    return nodeId.startsWith(CREATED_NODE_PREFIX);
}
