export { NetworkEditor, type DiagramViewBox } from './editor/NetworkEditor.ts';

export type {
    AttributeModification,
    BatteryCreation,
    BoundaryLineCreation,
    ConnectionDirection,
    ConverterStationCreation,
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
} from './modifications/modifications.ts';

export {
    PROPERTY_SCHEMAS,
    schemaFor,
    type PropertyDescriptor,
    type PropertyMode,
} from './actions/properties.ts';

export { actionKey, type ActionEquipment, type ActionSubject, type EditorAction } from './actions/actions.ts';

export {
    actionLabel,
    describeTargets,
    type ActionMenuItem,
} from './actions/menu.ts';

export {
    BAY_SLOT_CLASS,
    NODE_TARGET_CLASS,
    PENDING_CREATE_CLASS,
    SELECTED_CLASS,
    SWITCH_END_CLASS,
    SWITCH_START_CLASS,
} from './types.ts';

export type {
    BusbarTarget,
    CreationPlacement,
    EditOperation,
    EditorEvent,
    EditorEventListener,
    EditorEventName,
    EditorEvents,
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
    SwitchEnd,
    SwitchGesture,
    ViewerCallbacks,
} from './types.ts';
