import { availableOperations, creatableTypesFor } from './operations.ts';
import { defaultsOf, EQUIPMENT_ID, schemaFor, type PropertyDescriptor } from './properties.ts';
import { injectionCreation } from '../modifications/modifications.ts';
import {
    SWITCH_TYPES,
    toDirection,
    toElementType,
    type BayPosition,
    type BusbarTarget,
    type CreateSpec,
    type EditOperation,
    type EditTarget,
    type ElementType,
    type EquipmentProperties,
    type EquipmentTarget,
    type FeederPlacement,
    type PickedSwitch,
} from '../types.ts';

export type ActionSubject =
    | { kind: 'TYPE'; type: ElementType }
    | { kind: 'BUSBAR'; busbarSectionId: string }
    | { kind: 'SELECTION'; size: number };

export interface ActionEquipment {
    id: string;
    type: ElementType;
    vlId: string;
}

// `any`, not `unknown`: only `Record<string, any>` accepts a host DTO declared as an interface.
export type ActionValues = Readonly<Record<string, any>>;

export interface EditorAction {
    id: string;
    operation: EditOperation;
    subject?: ActionSubject;
    form: readonly PropertyDescriptor[];
    initial: ActionValues;
    equipment?: ActionEquipment;
    run(values?: ActionValues): boolean;
}

/** `OPERATION:TYPE` for a creation (e.g. `CREATE_FEEDER_BAY:LOAD`), the operation otherwise. */
export function actionKey(action: EditorAction): string {
    return action.subject?.kind === 'TYPE'
        ? `${action.operation}:${action.subject.type}`
        : action.operation;
}

export interface ActionHost {
    // Reads: what the forms show and which actions are offered.
    selectedTargets(): readonly EquipmentTarget[];
    getProperties(equipmentId: string): EquipmentProperties;
    getBayPosition(equipmentId: string): BayPosition | undefined;
    proposedOrder(target: BusbarTarget, insertion?: number): number | undefined;
    proposeEquipmentId(type: ElementType): string;
    moveDestinations(equipmentId: string): BusbarTarget[];
    canBeginBayMove(equipmentId: string): boolean;

    // Edits: each one validates, then pushes a command; false when refused.
    create(targetId: string, operation: EditOperation, spec: CreateSpec): boolean;
    createSwitchedInjection(targetId: string, spec: CreateSpec): boolean;
    beginSwitch(targetId: string, type: ElementType): boolean;
    createSwitch(spec: CreateSpec): boolean;
    replaceEquipment(equipmentId: string, newEquipment: CreateSpec): boolean;
    renameEquipment(equipmentId: string, newId: string): boolean;
    applyProperties(equipmentId: string, changes: EquipmentProperties): boolean;
    deleteElements(equipmentIds: readonly string[], kind: 'element' | 'bay'): boolean;
    beginBayMove(equipmentId: string): boolean;
    setBayPosition(equipmentId: string, position: BayPosition): boolean;
    moveFeederBay(equipmentId: string, busbarTargetId: string): boolean;
}

const ORDER: PropertyDescriptor = { key: 'connectionPosition', type: 'number', required: true, min: 0 };

const SWITCH_KIND: PropertyDescriptor = {
    key: 'switchKind',
    type: 'select',
    options: [...SWITCH_TYPES],
    required: true,
    defaultValue: 'BREAKER',
};

const DIRECTION: PropertyDescriptor = {
    key: 'connectionDirection',
    type: 'select',
    options: ['TOP', 'BOTTOM'],
    required: true,
    defaultValue: 'BOTTOM',
};

export function buildActions(
    host: ActionHost,
    target: EditTarget,
    insertion?: number,
): EditorAction[] {
    const batch = host.selectedTargets();
    if (batch.length > 1 && batch.some((selected) => selected.id === target.id)) {
        return batchActions(host, target, batch);
    }

    return availableOperations(target).flatMap((operation) => {
        const creatable = creatableTypesFor(operation);
        if (creatable.size > 0) {
            // A replacement only offers the other types.
            const current = operation === 'REPLACE' && target.kind === 'EQUIPMENT' ? target.type : undefined;
            return [...creatable]
                .filter((type) => type !== current)
                .map((type) => createAction(host, target, operation, type, insertion));
        }
        if (operation === 'RENAME') return renameAction(host, target);
        if (target.kind === 'EQUIPMENT') return equipmentActions(host, target, operation);
        return [];
    });
}

function createForm(
    type: ElementType,
    onBusbar: boolean,
    behindSwitch: boolean,
): PropertyDescriptor[] {
    const properties = schemaFor(type,'create').filter(p => p !== EQUIPMENT_ID);
    return [
        EQUIPMENT_ID,
        ...(onBusbar ? [ORDER, DIRECTION] : []),
        ...(behindSwitch ? [SWITCH_KIND] : []),
        ...properties
    ];
}

function creationInitial(
    host: ActionHost,
    form: readonly PropertyDescriptor[],
    type: ElementType,
): EquipmentProperties {
    return { ...defaultsOf(form), equipmentId: host.proposeEquipmentId(type) };
}

function specFromValues(type: ElementType, values: ActionValues): CreateSpec | undefined {
    const { equipmentId, connectionPosition, connectionDirection, switchKind, ...rest } = values;
    const provisionalId = text(equipmentId);
    if (!provisionalId) return undefined;

    return {
        type,
        properties: schemaValues(schemaFor(type, 'create'), rest),
        provisionalId,
        order: typeof connectionPosition === 'number' ? connectionPosition : undefined,
        direction: toDirection(text(connectionDirection)),
        switchType: switchKind === undefined ? undefined : toElementType(text(switchKind)),
        fields: rest,
    };
}

function createAction(
    host: ActionHost,
    target: EditTarget,
    operation: EditOperation,
    type: ElementType,
    insertion?: number,
): EditorAction {
    if (operation === 'CREATE_SWITCH' && target.kind === 'NODE') {
        return {
            id: `${createOperationId(target, operation)}:${type}`,
            operation,
            subject: { kind: 'TYPE', type },
            form: [],
            initial: {},
            run: () => host.beginSwitch(target.id, type),
        };
    }

    if (operation === 'REPLACE' && target.kind === 'EQUIPMENT') {
        const form = [...schemaFor(type, 'create')];
        const initial = creationInitial(host, form, type);
        return {
            id: `${createOperationId(target, operation)}:${type}`,
            operation,
            subject: { kind: 'TYPE', type },
            form,
            initial,
            equipment: equipmentOf(target),
            run: (values = {}) => {
                const spec = specFromValues(type, values);
                return spec ? host.replaceEquipment(target.equipmentId, spec) : false;
            },
        };
    }

    const onBusbar = operation === 'CREATE_FEEDER_BAY';
    const behindSwitch = operation === 'CREATE_SWITCHED_INJECTION';
    const form = createForm(type, onBusbar, behindSwitch);

    const initial =
        onBusbar && target.kind === 'BUSBAR'
            ? proposedCreation(target, type, {
                  ...creationInitial(host, form, type),
                  connectionPosition: host.proposedOrder(target, insertion) ?? null,
              })
            : creationInitial(host, form, type);

    return {
        id: `${createOperationId(target, operation)}:${type}`,
        operation,
        subject: { kind: 'TYPE', type },
        form,
        initial,
        run: (values = {}) => {
            if (target.kind === 'BUSBAR' && !isOnTarget(target, values)) return false;
            const spec = specFromValues(type, followingId(values, initial));
            if (!spec) return false;
            if (behindSwitch && target.kind === 'NODE') {
                return host.createSwitchedInjection(target.id, spec);
            }
            return host.create(target.id, operation, spec);
        },
    };
}

/** The creation `run` would produce from these form values: what a host form opens on. */
function proposedCreation(target: BusbarTarget, type: ElementType, values: ActionValues): ActionValues {
    const { equipmentId, ...rest } = values;
    return {
        ...injectionCreation(
            type,
            text(equipmentId) ?? '',
            placementOf(target, values),
            schemaValues(schemaFor(type, 'create'), rest),
        ),
    };
}

// A host form may offer another voltage level or busbar: the editor only creates on its target.
function isOnTarget(target: BusbarTarget, values: ActionValues): boolean {
    const { voltageLevelId, busOrBusbarSectionId } = values;
    return (voltageLevelId === undefined || voltageLevelId === target.vlId)
        && (busOrBusbarSectionId === undefined || busOrBusbarSectionId === target.busbarSectionId);
}

// The proposed connection name, left untouched, follows the equipment id the form hands back.
function followingId(values: ActionValues, initial: ActionValues): ActionValues {
    if (values.connectionName !== initial.connectionName) return values;
    const { connectionName: _proposed, ...rest } = values;
    return rest;
}

function placementOf(target: BusbarTarget, values: ActionValues): FeederPlacement {
    return {
        voltageLevelId: target.vlId,
        busbarSectionId: target.busbarSectionId,
        order: typeof values.connectionPosition === 'number' ? values.connectionPosition : null,
        direction: toDirection(text(values.connectionDirection)),
    };
}

/** Scalar values of the keys the schema knows: what the editor stores as properties. */
function schemaValues(schema: readonly PropertyDescriptor[], values: ActionValues): EquipmentProperties {
    const properties: EquipmentProperties = {};
    for (const { key } of schema) {
        const value = values[key];
        if (typeof value === 'number' || typeof value === 'string' || typeof value === 'boolean') {
            properties[key] = value;
        }
    }
    return properties;
}

export function switchAction(host: ActionHost, picked: PickedSwitch): EditorAction {
    const form = createForm(picked.type, false, false);
    const initial = creationInitial(host, form, picked.type);

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
            return [plain(target, operation, () => host.deleteElements([equipmentId], 'element'))];

        case 'DELETE_BAY':
            return [plain(target, operation, () => host.deleteElements([equipmentId], 'bay'))];

        case 'MOVE_BAY':
            return host.moveDestinations(equipmentId).map((destination) => ({
                id: `${createOperationId(target, operation)}:${destination.id}`,
                operation,
                subject: { kind: 'BUSBAR', busbarSectionId: destination.busbarSectionId },
                form: [],
                initial: {},
                equipment: equipmentOf(target),
                run: () => host.moveFeederBay(equipmentId, destination.id),
            }));

        case 'UPDATE_BAY_POSITION':
            if (!host.canBeginBayMove(equipmentId)) return [];
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
                    id: createOperationId(target, operation),
                    operation,
                    form,
                    initial: {
                        ...defaultsOf(form.filter((descriptor) => descriptor.editOnly)),
                        ...host.getProperties(equipmentId),
                        equipmentId,
                    },
                    equipment: equipmentOf(target),
                    run: (values = {}) => {
                        const { equipmentId: _newId, ...properties } = values;
                        const stored = host.getProperties(equipmentId);
                        return host.applyProperties(
                            equipmentId,
                            Object.fromEntries(
                                Object.entries(schemaValues(form, properties)).filter(([key, v]) => v !== stored[key]),
                            ),
                        );
                    },
                },
            ];
        }

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
            id: createOperationId(target, operation),
            operation,
            subject: { kind: 'SELECTION' as const, size: batch.length },
            form: [],
            initial: {},
            run: () => host.deleteElements(equipmentIds, kind),
        }));
}

function renameAction(host: ActionHost, target: EditTarget): EditorAction[] {
    const equipment: ActionEquipment | undefined =
        target.kind === 'EQUIPMENT'
            ? equipmentOf(target)
            : target.kind === 'BUSBAR'
              ? { id: target.busbarSectionId, type: 'BUS', vlId: target.vlId }
              : undefined;
    if (!equipment) return [];
    const equipmentId = equipment.id;

    return [
        {
            id: createOperationId(target, 'RENAME'),
            operation: 'RENAME',
            form: [EQUIPMENT_ID],
            initial: { equipmentId },
            equipment,
            run: (values = {}) => {
                const newId = text(values.equipmentId);
                return newId ? host.renameEquipment(equipmentId, newId) : false;
            },
        },
    ];
}


function plain(target: EquipmentTarget, operation: EditOperation, run: () => boolean): EditorAction {
    return {
        id: createOperationId(target, operation),
        operation,
        form: [],
        initial: {},
        equipment: equipmentOf(target),
        run,
    };
}

function equipmentOf(target: EquipmentTarget): ActionEquipment {
    return { id: target.equipmentId, type: target.type, vlId: target.vlId };
}

function createOperationId(target: EditTarget, operation: EditOperation): string {
    return `${target.id}:${operation}`;
}

function text(value: unknown): string | undefined {
    return typeof value === 'string' ? value : undefined;
}
