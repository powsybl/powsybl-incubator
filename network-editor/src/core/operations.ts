import {
    DELETABLE_BAY_TYPES,
    DELETABLE_TYPES,
    INJECTION_TYPES,
    SWITCH_TYPES,
    toElementType,
    type EditOperation,
    type EditTarget,
    type ElementType,
    type EquipmentTarget,
    type NodeMetadata,
} from './types';

const NO_TYPES: ReadonlySet<ElementType> = new Set<ElementType>();

const CREATED_OPERATIONS: ReadonlySet<EditOperation> = new Set<EditOperation>([
    'DELETE',
    'RENAME',
    'UPDATE_PROPERTIES',
]);

export function availableOperations(target: EditTarget): EditOperation[] {
    switch (target.kind) {
        case 'NODE':
            return target.occupied
                ? ['CREATE_SWITCH', 'CREATE_SWITCHED_INJECTION']
                : ['CREATE_INJECTION', 'CREATE_SWITCH', 'CREATE_SWITCHED_INJECTION'];
        case 'BUSBAR':
            return ['CREATE_FEEDER_BAY', 'CREATE_COUPLING'];
        case 'EQUIPMENT':
            return operationsForTarget(target);
    }
}

function operationsForTarget(target: EquipmentTarget): EditOperation[] {
    if (target.pending) return ['UPDATE_PROPERTIES'];

    const operations = operationsForEquipment(target.type).filter(
        (operation) => !target.created || CREATED_OPERATIONS.has(operation),
    );
    if (target.order !== undefined) operations.push('UPDATE_BAY_POSITION');
    return operations;
}

export function operationsForEquipment(type: ElementType): EditOperation[] {
    const operations: EditOperation[] = [];
    if (DELETABLE_TYPES.has(type)) operations.push('DELETE');
    if (DELETABLE_BAY_TYPES.has(type)) operations.push('DELETE_BAY', 'MOVE_BAY');
    if (!SWITCH_TYPES.has(type)) operations.push('RENAME');
    operations.push('UPDATE_PROPERTIES');
    return operations;
}

export function creatableTypesFor(operation: EditOperation): ReadonlySet<ElementType> {
    switch (operation) {
        case 'CREATE_INJECTION':
        case 'CREATE_SWITCHED_INJECTION':
        case 'CREATE_FEEDER_BAY':
            return INJECTION_TYPES;
        case 'CREATE_SWITCH':
        case 'CREATE_COUPLING':
            return SWITCH_TYPES;
        case 'DELETE':
        case 'DELETE_BAY':
        case 'MOVE_BAY':
        case 'RENAME':
        case 'UPDATE_PROPERTIES':
        case 'UPDATE_BAY_POSITION':
            return NO_TYPES;
    }
}

export function isSwitchNode(node: NodeMetadata): boolean {
    return SWITCH_TYPES.has(toElementType(node.componentType));
}
