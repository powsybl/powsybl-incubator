import {
    BUSBAR_TYPES,
    DELETABLE_BAY_TYPES,
    DELETABLE_TYPES,
    INJECTION_TYPES,
    SWITCH_TYPES,
    toElementType,
    type EditOperation,
    type EditTarget,
    type ElementType,
    type NodeMetadata,
} from './types';

export interface OperationSpec {
    label: string;
    danger?: boolean;
    creates?: ReadonlySet<ElementType>;
    appliesTo(target: EditTarget): boolean;
}

export const OPERATIONS: Record<EditOperation, OperationSpec> = {
    CREATE_INJECTION: {
        label: 'Add injection',
        creates: INJECTION_TYPES,
        appliesTo: (target) => target.kind === 'NODE' && !target.occupied,
    },
    CREATE_SWITCH: {
        label: 'Add a switch',
        creates: SWITCH_TYPES,
        appliesTo: (target) => target.kind === 'NODE',
    },
    CREATE_SWITCHED_INJECTION: {
        label: 'Add behind a switch',
        creates: INJECTION_TYPES,
        appliesTo: (target) => target.kind === 'NODE',
    },
    CREATE_FEEDER_BAY: {
        label: 'Create a feeder bay',
        creates: INJECTION_TYPES,
        appliesTo: (target) => target.kind === 'BUSBAR',
    },
    CREATE_BUSBAR: {
        label: 'Create a busbar',
        creates: BUSBAR_TYPES,
        appliesTo: (target) => target.kind === 'BUSBAR',
    },
    DELETE: {
        label: 'Delete',
        danger: true,
        appliesTo: (target) => target.kind === 'EQUIPMENT' && DELETABLE_TYPES.has(target.type),
    },
    DELETE_BAY: {
        label: 'Delete feeder bay',
        danger: true,
        appliesTo: (target) => target.kind === 'EQUIPMENT' && DELETABLE_BAY_TYPES.has(target.type),
    },
    MOVE_BAY: {
        label: 'Move feeder bay',
        appliesTo: (target) => target.kind === 'EQUIPMENT' && DELETABLE_BAY_TYPES.has(target.type),
    },
    RENAME: {
        label: 'Rename',
        appliesTo: (target) => target.kind === 'EQUIPMENT' && !SWITCH_TYPES.has(target.type),
    },
    UPDATE_PROPERTIES: {
        label: 'Properties',
        appliesTo: (target) => target.kind === 'EQUIPMENT',
    },
    UPDATE_BAY_POSITION: {
        label: 'Change bay position',
        appliesTo: (target) => target.kind === 'EQUIPMENT' && target.order !== undefined,
    },
    FLIP_BAY_DIRECTION: {
        label: 'Flip bay direction',
        appliesTo: (target) => target.kind === 'EQUIPMENT' && target.order !== undefined,
    },
};

const OPERATION_NAMES = Object.keys(OPERATIONS) as EditOperation[];

const NO_TYPES: ReadonlySet<ElementType> = new Set<ElementType>();

const CREATED_OPERATIONS: ReadonlySet<EditOperation> = new Set<EditOperation>([
    'DELETE',
    'RENAME',
    'UPDATE_PROPERTIES',
    'UPDATE_BAY_POSITION',
    'FLIP_BAY_DIRECTION',
]);

export function availableOperations(target: EditTarget): EditOperation[] {
    if (target.kind === 'EQUIPMENT' && target.claimed) return ['UPDATE_PROPERTIES'];

    const created = target.kind === 'EQUIPMENT' && target.created;
    return OPERATION_NAMES.filter(
        (operation) =>
            OPERATIONS[operation].appliesTo(target) &&
            (!created || CREATED_OPERATIONS.has(operation)),
    );
}

export function creatableTypesFor(operation: EditOperation): ReadonlySet<ElementType> {
    return OPERATIONS[operation].creates ?? NO_TYPES;
}

export function isSwitchNode(node: NodeMetadata): boolean {
    return SWITCH_TYPES.has(toElementType(node.componentType));
}
