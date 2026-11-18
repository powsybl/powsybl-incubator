export { NetworkEditor, type DiagramViewBox } from './editor/NetworkEditor.ts';

export { CHANGE_FORMAT_VERSION } from './changes/changes.ts';

export type {
    Attributes,
    CreateBusbarSection,
    CreateCouplingDevice,
    CreateFeederBay,
    CreateInjection,
    CreateSwitch,
    EquipmentType,
    Extra,
    MoveFeederBay,
    NetworkChange,
    RemoveElement,
    RemoveFeederBay,
    Rename,
    SwitchKind,
    UpdateAttributes,
} from './changes/changes.ts';

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
    groupMenuItems,
    operationLabel,
    type ActionMenuEntry,
    type ActionMenuGroup,
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
