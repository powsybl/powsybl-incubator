export { NetworkEditor } from './core/NetworkEditor';

export {
    PROPERTY_SCHEMAS,
    propertiesFor,
    schemaFor,
    validateProperties,
    validateValues,
    type PropertyDescriptor,
    type PropertyMode,
} from './properties';

export type { ActionSubject, EditorAction } from './core/actions';

export {
    NODE_TARGET_CLASS,
    PENDING_CREATE_CLASS,
    SELECTED_CLASS,
    SELECTION_CANDIDATE_CLASS,
    SELECTION_FIRST_CLASS,
} from './core/types';

export type {
    BayInsertion,
    BusbarTarget,
    ChangeOp,
    ChangeSet,
    ChangeSetEntry,
    EditOperation,
    EditorEventListener,
    EditorEventName,
    EditorEvents,
    EditorOptions,
    EditTarget,
    ElementType,
    EquipmentProperties,
    EquipmentTarget,
    GapTarget,
    NodeTarget,
    SelectionState,
    SLDMetadata,
    TargetEvent,
    ViewerCallbacks,
} from './core/types';
