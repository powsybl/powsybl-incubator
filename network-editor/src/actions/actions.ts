import { availableOperations, creatableTypesFor } from './operations.ts';
import { defaultsOf, EQUIPMENT_ID, schemaFor, type PropertyDescriptor } from './properties.ts';
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
    type PickedSwitch,
} from '../types.ts';

export type ActionSubject =
    | { kind: 'TYPE'; type: ElementType }
    | { kind: 'BUSBAR'; busbarSectionId: string }
    | { kind: 'SELECTION'; size: number }
    | { kind: 'STATE'; open: boolean }

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

/**
 * `OPERATION:TYPE` for a creation (e.g. `CREATE_FEEDER_BAY:LOAD`) or a property edit (e.g. `UPDATE_PROPERTIES:LOAD`),
 * the operation otherwise.
 */
export function actionKey(action: EditorAction): string {
    if (action.subject?.kind === 'TYPE') return `${action.operation}:${action.subject.type}`;
    if (action.operation === 'UPDATE_PROPERTIES' && action.equipment) return `${action.operation}:${action.equipment.type}`;
    return action.operation;
}

interface ActionHost {
    // Reads: what the forms show and which actions are offered.
    selectedTargets(): readonly EquipmentTarget[];
    getProperties(equipmentId: string): EquipmentProperties;
    getBayPosition(equipmentId: string): BayPosition | undefined;
    proposedOrder(target: BusbarTarget, insertion?: number): number | undefined;
    proposeEquipmentId(type: ElementType): string;
    moveDestinations(equipmentId: string): BusbarTarget[];
    couplingTargets(targetId: string): BusbarTarget[];
    canBeginBayMove(equipmentId: string): boolean;

    // Edits: each one validates, then pushes a command; false when refused.
    create(targetId: string, operation: EditOperation, spec: CreateSpec): boolean;
    createSwitchedInjection(targetId: string, spec: CreateSpec): boolean;
    beginSwitch(targetId: string, type: ElementType): boolean;
    createSwitch(spec: CreateSpec): boolean;
    replaceEquipment(equipmentId: string, newEquipment: CreateSpec): boolean;
    renameEquipment(equipmentId: string, newId: string): boolean;
    applyProperties(
        equipmentId: string,
        changes: EquipmentProperties,
        fields?: Readonly<Record<string, unknown>>,
    ): boolean;
    deleteElements(equipmentIds: readonly string[], kind: 'element' | 'bay'): boolean;
    beginBayMove(equipmentId: string): boolean;
    createBusbar(targetId: string, equipmentId: string): boolean;
    createCoupling(targetId: string, otherId: string): boolean;
    setBayPosition(equipmentId: string, position: BayPosition): boolean;
    moveFeederBay(equipmentId: string, busbarTargetId: string): boolean;
    group(edit: () => boolean): boolean;
}

const ORDER: PropertyDescriptor = { key: 'positionOrder', type: 'number', required: true, min: 0 };

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
        if (operation === 'CREATE_BUSBAR') return [busbarAction(host, target)];
        if (operation === 'CREATE_COUPLING') return couplingActions(host, target);
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
    const {
        equipmentId,
        positionOrder,
        direction,
        switchKind,
        voltageLevelId: _vlId,
        busOrBusbarSectionId: _busbarId,
        ...rest
    } = values;
    const provisionalId = text(equipmentId);
    if (!provisionalId) return undefined;

    const schema = schemaFor(type, 'create');
    return {
        type,
        properties: schemaValues(schema, rest),
        provisionalId,
        order: typeof positionOrder === 'number' ? positionOrder : undefined,
        direction: toDirection(text(direction)),
        switchType: switchKind === undefined ? undefined : toElementType(text(switchKind)),
        fields: extraValues(schema, rest),
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

    const initial: ActionValues =
        onBusbar && target.kind === 'BUSBAR'
            ? {
                  ...creationInitial(host, form, type),
                  voltageLevelId: target.vlId,
                  busOrBusbarSectionId: target.busbarSectionId,
                  positionOrder: host.proposedOrder(target, insertion),
              }
            : creationInitial(host, form, type);

    return {
        id: `${createOperationId(target, operation)}:${type}`,
        operation,
        subject: { kind: 'TYPE', type },
        form,
        initial,
        run: (values = {}) => {
            if (target.kind === 'BUSBAR' && !isOnTarget(target, values)) return false;
            const spec = specFromValues(type, values);
            if (!spec) return false;
            if (behindSwitch && target.kind === 'NODE') {
                return host.createSwitchedInjection(target.id, spec);
            }
            return host.create(target.id, operation, spec);
        },
    };
}

// A host form may offer another voltage level or busbar: the editor only creates on its target.
function isOnTarget(target: BusbarTarget, values: ActionValues): boolean {
    const { voltageLevelId, busOrBusbarSectionId } = values;
    return (voltageLevelId === undefined || voltageLevelId === target.vlId)
        && (busOrBusbarSectionId === undefined || busOrBusbarSectionId === target.busbarSectionId);
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

/** The values of the keys the schema does not know: host data carried as is in the change `extra`. */
function extraValues(schema: readonly PropertyDescriptor[], values: ActionValues): Record<string, unknown> {
    const known = new Set(schema.map(({ key }) => key));
    return Object.fromEntries(
        Object.entries(values).filter(([key, value]) => !known.has(key) && value !== null && value !== undefined),
    );
}

/**
 * Applies host values as one step, or not at all: the bay placement goes through the bay edits,
 * the attributes through a property edit that also carries the unknown keys as `extra`.
 */
function editEquipment(
    host: ActionHost,
    target: EquipmentTarget,
    form: readonly PropertyDescriptor[],
    values: ActionValues,
): boolean {
    const { equipmentId: _id, voltageLevelId, busOrBusbarSectionId, positionOrder, direction, ...rest } = values;
    // A bay only moves within its voltage level.
    if (voltageLevelId !== undefined && voltageLevelId !== target.vlId) return false;

    const known = schemaValues(form, rest);
    const extra = extraValues(form, rest);
    const hasEdits = Object.keys(known).length > 0 || Object.keys(extra).length > 0;
    const busbar = text(busOrBusbarSectionId);

    return host.group(() => {
        if (busbar !== undefined && !moveToBusbar(host, target.equipmentId, busbar)) return false;
        const position = bayPositionChange(host.getBayPosition(target.equipmentId), positionOrder, direction);
        if (position && !host.setBayPosition(target.equipmentId, position)) return false;
        if (hasEdits && !host.applyProperties(target.equipmentId, known, extra)) return false;
        return busbar !== undefined || position !== undefined || hasEdits;
    });
}

function moveToBusbar(host: ActionHost, equipmentId: string, busbarSectionId: string): boolean {
    const destination = host
        .moveDestinations(equipmentId)
        .find((candidate) => candidate.busbarSectionId === busbarSectionId);
    return destination !== undefined && host.moveFeederBay(equipmentId, destination.id);
}

/** The bay position the host values ask for (`positionOrder`, `direction`), when it moves the bay. */
function bayPositionChange(current: BayPosition | undefined, positionOrder: unknown, direction: unknown): BayPosition | undefined {
    if (!current) return undefined;
    const order = typeof positionOrder === 'number' ? positionOrder : current.order;
    const newDirection = toDirection(text(direction)) ?? current.direction;
    return order === current.order && newDirection === current.direction ? undefined : { order, direction: newDirection };
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

        case 'TOGGLE_SWITCH': {
            const open = host.getProperties(equipmentId).open;
            if (typeof open != "boolean") return [];

            return [
                plain(
                    target,
                    operation,
                    () => host.applyProperties(equipmentId, { open: !open }),
                    { kind: 'STATE', open }
                )
            ]
        }

        case 'UPDATE_PROPERTIES': {
            const form = schemaFor(target.type, target.created ? 'create' : 'edit').map(
                (descriptor) =>
                    descriptor.key === 'equipmentId'
                        ? { ...descriptor, readOnly: true }
                        : descriptor,
            );
            const { equipmentId: _id, ...known } = schemaValues(form, host.getProperties(equipmentId));
            return [
                {
                    id: createOperationId(target, operation),
                    operation,
                    form,
                    initial: { ...known, equipmentId },
                    equipment: equipmentOf(target),
                    run: (values = {}) => editEquipment(host, target, form, values),
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

function busbarAction(host: ActionHost, target: EditTarget): EditorAction {
    return {
        id: createOperationId(target, 'CREATE_BUSBAR'),
        operation: 'CREATE_BUSBAR',
        form: [EQUIPMENT_ID],
        initial: { equipmentId: host.proposeEquipmentId('BUS') },
        run: (values = {}) => {
            const equipmentId = text(values.equipmentId);
            return equipmentId ? host.createBusbar(target.id, equipmentId) : false;
        },
    };
}

function couplingActions(host: ActionHost, target: EditTarget): EditorAction[] {
    return host.couplingTargets(target.id).map((other) => ({
        id: `${createOperationId(target, 'CREATE_COUPLING')}:${other.id}`,
        operation: 'CREATE_COUPLING' as const,
        subject: { kind: 'BUSBAR' as const, busbarSectionId: other.busbarSectionId },
        form: [],
        initial: {},
        run: () => host.createCoupling(target.id, other.id),
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


function plain(
    target: EquipmentTarget,
    operation: EditOperation,run: () => boolean,
    subject?:ActionSubject
): EditorAction
{
    return {
        id: createOperationId(target, operation),
        operation,
        subject,
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
