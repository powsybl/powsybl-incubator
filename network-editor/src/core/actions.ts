import { availableOperations, creatableTypesFor } from './operations';
import { defaultsOf, EQUIPMENT_ID, schemaFor, type PropertyDescriptor } from '../properties';
import {
    SWITCH_TYPES,
    toDirection,
    toElementType,
    type BayInsertion,
    type BayPosition,
    type BusbarTarget,
    type CreateSpec,
    type EditOperation,
    type EditTarget,
    type ElementType,
    type EquipmentProperties,
    type EquipmentTarget,
    type PickedSwitch,
} from './types';

export type ActionSubject =
    | { kind: 'TYPE'; type: ElementType }
    | { kind: 'BUSBAR'; busbarSectionId: string }
    | { kind: 'SELECTION'; size: number };

export interface EditorAction {
    id: string;
    operation: EditOperation;
    subject?: ActionSubject;
    form: readonly PropertyDescriptor[];
    initial: EquipmentProperties;
    run(values?: EquipmentProperties): boolean;
}

export interface ActionHost {
    create(targetId: string, operation: EditOperation, spec: CreateSpec): boolean;
    applyProperties(equipmentId: string, changes: EquipmentProperties): boolean;
    deleteElement(equipmentId: string): boolean;
    deleteFeederBay(equipmentId: string): boolean;
    deleteElements(equipmentIds: readonly string[], kind: 'element' | 'bay'): boolean;
    selectedTargets(): readonly EquipmentTarget[];
    moveDestinations(equipmentId: string): BusbarTarget[];
    moveFeederBay(equipmentId: string, busbarTargetId: string): boolean;
    renameEquipment(equipmentId: string, newId: string): boolean;
    beginSwitch(targetId: string, type: ElementType): boolean;
    createSwitch(spec: CreateSpec): boolean;
    beginBayMove(equipmentId: string): boolean;
    createSwitchedInjection(targetId: string, spec: CreateSpec): boolean;
    getBayPosition(equipmentId: string): BayPosition | undefined;
    setBayPosition(equipmentId: string, position: BayPosition): boolean;
    getProperties(equipmentId: string): EquipmentProperties;
    proposedOrder(target: BusbarTarget, insertion?: BayInsertion): number | undefined;
}

const ORDER: PropertyDescriptor = { key: 'order', type: 'number', required: true, min: 0 };

const SWITCH_KIND: PropertyDescriptor = {
    key: 'switchKind',
    type: 'select',
    options: [...SWITCH_TYPES],
    required: true,
    defaultValue: 'BREAKER',
};

const DIRECTION: PropertyDescriptor = {
    key: 'direction',
    type: 'select',
    options: ['TOP', 'BOTTOM'],
    required: true,
    defaultValue: 'BOTTOM',
};

export function buildActions(
    host: ActionHost,
    target: EditTarget,
    insertion?: BayInsertion,
): EditorAction[] {
    const batch = host.selectedTargets();
    if (batch.length > 1 && batch.some((selected) => selected.id === target.id)) {
        return batchActions(host, target, batch);
    }

    return availableOperations(target).flatMap((operation) => {
        const creatable = creatableTypesFor(operation);
        if (creatable.size > 0) {
            return [...creatable].map((type) =>
                createAction(host, target, operation, type, insertion),
            );
        }
        return target.kind === 'EQUIPMENT' ? equipmentActions(host, target, operation) : [];
    });
}

function createForm(
    type: ElementType,
    onBusbar: boolean,
    behindSwitch: boolean,
): PropertyDescriptor[] {
    return [
        EQUIPMENT_ID,
        ...(onBusbar ? [ORDER, DIRECTION] : []),
        ...(behindSwitch ? [SWITCH_KIND] : []),
        ...schemaFor(type, 'create'),
    ];
}

function specFromValues(type: ElementType, values: EquipmentProperties): CreateSpec | undefined {
    const { equipmentId, order, direction, switchKind, ...properties } = values;
    const provisionalId = text(equipmentId);
    if (!provisionalId) return undefined;

    return {
        type,
        properties,
        provisionalId,
        order: typeof order === 'number' ? order : undefined,
        direction: toDirection(text(direction)),
        switchType: switchKind === undefined ? undefined : toElementType(text(switchKind)),
    };
}

function createAction(
    host: ActionHost,
    target: EditTarget,
    operation: EditOperation,
    type: ElementType,
    insertion?: BayInsertion,
): EditorAction {
    if (operation === 'CREATE_SWITCH' && target.kind === 'NODE') {
        return {
            id: `${id(target, operation)}:${type}`,
            operation,
            subject: { kind: 'TYPE', type },
            form: [],
            initial: {},
            run: () => host.beginSwitch(target.id, type),
        };
    }

    const onBusbar = operation === 'CREATE_FEEDER_BAY';
    const behindSwitch = operation === 'CREATE_SWITCHED_INJECTION';
    const form = createForm(type, onBusbar, behindSwitch);

    const initial = defaultsOf(form);
    initial.equipmentId = 'NEW_' + type;

    if (onBusbar && target.kind === 'BUSBAR') {
        const order = host.proposedOrder(target, insertion);
        if (order !== undefined) initial.order = order;
    }

    return {
        id: `${id(target, operation)}:${type}`,
        operation,
        subject: { kind: 'TYPE', type },
        form,
        initial,
        run: (values = {}) => {
            const spec = specFromValues(type, values);
            if (!spec) return false;
            if (behindSwitch && target.kind === 'NODE') {
                return host.createSwitchedInjection(target.id, spec);
            }
            return host.create(target.id, operation, spec);
        },
    };
}

export function switchAction(host: ActionHost, picked: PickedSwitch): EditorAction {
    const form = createForm(picked.type, false, false);
    const initial = defaultsOf(form);
    initial.equipmentId = 'NEW_' + picked.type;

    return {
        id: `${picked.first.id}:CREATE_SWITCH:${picked.second.id}`,
        operation: 'CREATE_SWITCH',
        subject: { kind: 'TYPE', type: picked.type },
        form,
        initial,
        run: (values = {}) => {
            const spec = specFromValues(picked.type, values);
            return spec ? host.createSwitch(spec) : false;
        },
    };
}

function equipmentActions(
    host: ActionHost,
    target: EquipmentTarget,
    operation: EditOperation,
): EditorAction[] {
    const { equipmentId } = target;

    switch (operation) {
        case 'DELETE':
            return [plain(target, operation, () => host.deleteElement(equipmentId))];

        case 'DELETE_BAY':
            return [plain(target, operation, () => host.deleteFeederBay(equipmentId))];

        case 'MOVE_BAY':
            return host.moveDestinations(equipmentId).map((destination) => ({
                id: `${id(target, operation)}:${destination.id}`,
                operation,
                subject: { kind: 'BUSBAR', busbarSectionId: destination.busbarSectionId },
                form: [],
                initial: {},
                run: () => host.moveFeederBay(equipmentId, destination.id),
            }));

        case 'UPDATE_BAY_POSITION':
            return [plain(target, operation, () => host.beginBayMove(equipmentId))];

        case 'FLIP_BAY_DIRECTION': {
            const current = host.getBayPosition(equipmentId);
            if (!current) return [];
            return [
                plain(target, operation, () =>
                    host.setBayPosition(equipmentId, {
                        order: current.order,
                        direction: current.direction === 'TOP' ? 'BOTTOM' : 'TOP',
                    }),
                ),
            ];
        }

        case 'UPDATE_PROPERTIES': {
            const form = schemaFor(target.type, target.created ? 'create' : 'edit').map(
                (descriptor) =>
                    descriptor.key === 'equipmentId'
                        ? { ...descriptor, readOnly: true }
                        : descriptor,
            );
            return [
                {
                    id: id(target, operation),
                    operation,
                    form,
                    initial: {
                        ...defaultsOf(form.filter((descriptor) => descriptor.editOnly)),
                        ...host.getProperties(equipmentId),
                        equipmentId,
                    },
                    run: (values = {}) => {
                        const { equipmentId: _newId, ...properties } = values;
                        const stored = host.getProperties(equipmentId);
                        return host.applyProperties(
                            equipmentId,
                            Object.fromEntries(
                                Object.entries(properties).filter(([key, v]) => v !== stored[key]),
                            ),
                        );
                    },
                },
            ];
        }

        case 'RENAME':
            return [
                {
                    id: id(target, operation),
                    operation,
                    form: [EQUIPMENT_ID],
                    initial: { equipmentId },
                    run: (values = {}) => {
                        const newId = text(values.equipmentId);
                        return newId ? host.renameEquipment(equipmentId, newId) : false;
                    },
                },
            ];

        default:
            return [];
    }
}

function batchActions(
    host: ActionHost,
    target: EditTarget,
    batch: readonly EquipmentTarget[],
): EditorAction[] {
    const equipmentIds = batch.map((selected) => selected.equipmentId);

    return ([
        ['DELETE', 'element'],
        ['DELETE_BAY', 'bay'],
    ] as const)
        .filter(([operation]) =>
            batch.every((selected) => availableOperations(selected).includes(operation)),
        )
        .map(([operation, kind]) => ({
            id: id(target, operation),
            operation,
            subject: { kind: 'SELECTION' as const, size: batch.length },
            form: [],
            initial: {},
            run: () => host.deleteElements(equipmentIds, kind),
        }));
}

function plain(target: EditTarget, operation: EditOperation, run: () => boolean): EditorAction {
    return { id: id(target, operation), operation, form: [], initial: {}, run };
}

function id(target: EditTarget, operation: EditOperation): string {
    return `${target.id}:${operation}`;
}

function text(value: EquipmentProperties[string] | undefined): string | undefined {
    return typeof value === 'string' ? value : undefined;
}
