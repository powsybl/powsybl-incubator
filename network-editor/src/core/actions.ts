import { availableOperations, creatableTypesFor } from './operations';
import { defaultsOf, schemaFor, type PropertyDescriptor } from '../properties';
import {
    IMPLEMENTED_OPERATIONS,
    toDirection,
    type BayInsertion,
    type BayPosition,
    type BusbarTarget,
    type CreateSpec,
    type EditOperation,
    type EditTarget,
    type ElementType,
    type EquipmentProperties,
    type EquipmentTarget,
} from './types';

export type ActionSubject =
    | { kind: 'TYPE'; type: ElementType }
    | { kind: 'BUSBAR'; busbarSectionId: string };


export interface EditorAction {
    id: string;
    operation: EditOperation;
    subject?: ActionSubject;
    /** false when the operation is announced but not implemented yet. */
    enabled: boolean;
    form: readonly PropertyDescriptor[];
    initial: EquipmentProperties;
    run(values?: EquipmentProperties): boolean;
}

export interface ActionHost {
    create(targetId: string, spec: CreateSpec): boolean;
    applyProperties(equipmentId: string, changes: EquipmentProperties): boolean;
    deleteElement(equipmentId: string): boolean;
    deleteFeederBay(equipmentId: string): boolean;
    moveDestinations(equipmentId: string): BusbarTarget[];
    moveFeederBay(equipmentId: string, busbarTargetId: string): boolean;
    renameEquipment(equipmentId: string, newId: string): boolean;
    getBayPosition(equipmentId: string): BayPosition | undefined;
    setBayPosition(equipmentId: string, position: BayPosition): boolean;
    getProperties(equipmentId: string): EquipmentProperties;
    proposedOrder(target: BusbarTarget, insertion?: BayInsertion): number | undefined;
}

const EQUIPMENT_ID: PropertyDescriptor = { key: 'equipmentId', type: 'string', required: true };

const ORDER: PropertyDescriptor = { key: 'order', type: 'number', required: true, min: 0 };

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
    return availableOperations(target).flatMap((operation) => {
        if (!IMPLEMENTED_OPERATIONS.has(operation)) {
            return [{ id: id(target, operation), operation, enabled: false, form: [], initial: {}, run: no }];
        }

        const creatable = creatableTypesFor(operation);
        if (creatable.size > 0) {
            return [...creatable].map((type) =>
                createAction(host, target, operation, type, insertion),
            );
        }
        return target.kind === 'EQUIPMENT' ? equipmentActions(host, target, operation) : [];
    });
}

function createAction(
    host: ActionHost,
    target: EditTarget,
    operation: EditOperation,
    type: ElementType,
    insertion?: BayInsertion,
): EditorAction {
    const onBusbar = target.kind === 'BUSBAR';
    const form = [
        EQUIPMENT_ID,
        ...(onBusbar ? [ORDER, DIRECTION] : []),
        ...schemaFor(type, 'create'),
    ];

    const initial = defaultsOf(form);
    initial.equipmentId = 'NEW_' + type

    if (onBusbar) {
        const order = host.proposedOrder(target, insertion);
        if (order !== undefined) initial.order = order;
    }

    return {
        id: `${id(target, operation)}:${type}`,
        operation,
        subject: { kind: 'TYPE', type },
        enabled: true,
        form,
        initial,
        run: (values = {}) => {
            const { equipmentId, order, direction, ...properties } = values;
            const provisionalId = text(equipmentId);
            if (!provisionalId) return false;
            return host.create(target.id, {
                type,
                properties,
                provisionalId,
                order: typeof order === 'number' ? order : undefined,
                direction: toDirection(text(direction)),
            });
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
                enabled: true,
                form: [],
                initial: {},
                run: () => host.moveFeederBay(equipmentId, destination.id),
            }));

        case 'UPDATE_BAY_POSITION': {
            const current = host.getBayPosition(equipmentId);
            if (!current) return [];
            return [
                {
                    id: id(target, operation),
                    operation,
                    enabled: true,
                    form: [ORDER, DIRECTION],
                    initial: { order: current.order, direction: current.direction },
                    run: (values = {}) =>
                        host.setBayPosition(equipmentId, {
                            order: Number(values.order),
                            direction: toDirection(text(values.direction)) ?? current.direction,
                        }),
                },
            ];
        }

        case 'UPDATE_PROPERTIES': {
            const form = schemaFor(target.type, 'edit');
            return [
                {
                    id: id(target, operation),
                    operation,
                    enabled: true,
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
                    enabled: true,
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

function plain(target: EditTarget, operation: EditOperation, run: () => boolean): EditorAction {
    return { id: id(target, operation), operation, enabled: true, form: [], initial: {}, run };
}

function id(target: EditTarget, operation: EditOperation): string {
    return `${target.id}:${operation}`;
}

function text(value: EquipmentProperties[string] | undefined): string | undefined {
    return typeof value === 'string' ? value : undefined;
}

function no(): boolean {
    return false;
}
