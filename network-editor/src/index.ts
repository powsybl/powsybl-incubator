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

export type { PropertyDescriptor } from './actions/properties.ts';

export {
    actionKey,
    type ActionEquipment,
    type ActionSubject,
    type ActionValues,
    type EditorAction,
} from './actions/actions.ts';

export {
    actionLabel,
    describeTargets,
    type ActionMenuItem,
} from './actions/menu.ts';

export type {
    BusbarTarget,
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
