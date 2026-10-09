export { NetworkEditor, type DiagramViewBox } from './core/NetworkEditor';

export type {
    AttributeModification,
    BatteryCreation,
    BoundaryLineCreation,
    ConnectionDirection,
    ConverterStationCreation,
    ElementDeletion,
    EquipmentAttributeModification,
    EquipmentDeletion,
    EquipmentModification,
    EquipmentRename,
    EnergySource,
    GeneratorCreation,
    InjectionCreation,
    InjectionCreationFields,
    LccConverterStationCreation,
    LoadCreation,
    LoadType,
    MoveFeederBay,
    MoveVoltageLevelFeederBays,
    NetworkModification,
    NodeInjectionCreation,
    ShuntCompensatorCreation,
    StaticVarCompensatorCreation,
    SwitchCreation,
    SwitchedInjectionCreation,
    SwitchKind,
} from './core/modifications';

export {
    PROPERTY_SCHEMAS,
    schemaFor,
    type PropertyDescriptor,
    type PropertyMode,
} from './properties';

export { actionKey, type ActionEquipment, type ActionSubject, type EditorAction } from './core/actions';

export {
    actionFor,
    actionLabel,
    describeTarget,
    describeTargets,
    menuItemsFor,
    type ActionMenuItem,
    type ActionSource,
    type MenuSubjects,
} from './menu';

export { OPERATIONS, type OperationSpec } from './core/operations';

export {
    BAY_SLOT_CLASS,
    NODE_TARGET_CLASS,
    PENDING_CREATE_CLASS,
    SELECTED_CLASS,
    SWITCH_END_CLASS,
    SWITCH_START_CLASS,
} from './core/types';

export type {
    BusbarTarget,
    ChangeSet,
    CreationPlacement,
    EditOperation,
    EditorEventListener,
    EditorEventName,
    EditorEvents,
    EditMode,
    EditorOptions,
    EditTarget,
    ElementType,
    EquipmentProperties,
    BayMoveGesture,
    BaySlotCandidate,
    Gesture,
    EquipmentTarget,
    NodeTarget,
    SelectedElement,
    SLDMetadata,
    SymbolProvider,
    SwitchEnd,
    SwitchGesture,
    TargetEvent,
    ViewerCallbacks,
} from './core/types';
