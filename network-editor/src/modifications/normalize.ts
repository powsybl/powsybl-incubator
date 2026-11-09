import {
    INJECTION_CREATION_TYPES,
    isAttributeModification,
    mergeFeederBayMoves,
    type EquipmentAttributeModification,
    type EquipmentDeletion,
    type EquipmentModification,
    type EquipmentRename,
    type InjectionCreation,
    type MoveFeederBay,
    type MoveVoltageLevelFeederBays,
    type NetworkModification,
    type NodeInjectionCreation,
    type SwitchCreation,
    type SwitchedInjectionCreation,
} from './modifications.ts';

type Creation = InjectionCreation | NodeInjectionCreation | SwitchedInjectionCreation | SwitchCreation;

type Kind = 'DELETION' | 'RENAME' | 'CREATION' | 'PROPERTIES' | 'SWITCH_STATE' | 'MOVE';

const OUTPUT_ORDER: Kind[] = ['DELETION', 'RENAME', 'CREATION', 'PROPERTIES', 'SWITCH_STATE', 'MOVE'];

const CREATION_TYPES = new Set<string>([
    ...Object.values(INJECTION_CREATION_TYPES),
    'NODE_INJECTION_CREATION',
    'SWITCHED_INJECTION_CREATION',
    'SWITCH_CREATION',
]);

interface EquipmentHistory {
    originalId: string;
    currentId: string;
    creation?: Creation;
    deletion?: EquipmentDeletion;
    properties?: { type: EquipmentModification['type']; fields: Record<string, unknown> };
    switchOpen?: boolean;
    move?: { voltageLevelId: string; bay: MoveFeederBay };
}

/**
 * Turns the raw list of edits into the shortest equivalent change set:
 * 1. group the edits by equipment, following renames;
 * 2. emit one compact history per equipment (create + delete cancel out, edits fold into creations...);
 * 3. order the result by kind, so that deletions free ids before renames and creations reuse them.
 */
export function normalizeModifications(modificationList: readonly NetworkModification[]): NetworkModification[] {
    const histories = groupByEquipment(modificationList);
    if (hasRenameCycle(histories)) return mergeFeederBayMoves(modificationList);

    const emitted = histories.flatMap(emit);
    const ordered = OUTPUT_ORDER.flatMap((kind) => emitted.filter((modification) => kindOf(modification) === kind));
    return mergeFeederBayMoves(ordered);
}

/** `byCurrentId` follows renames: a later edit targets the equipment by its latest id. */
function groupByEquipment(modificationList: readonly NetworkModification[]): EquipmentHistory[] {
    const histories: EquipmentHistory[] = [];
    const byCurrentId = new Map<string, EquipmentHistory>();

    const start = (equipmentId: string, creation?: Creation): EquipmentHistory => {
        const history: EquipmentHistory = { originalId: equipmentId, currentId: equipmentId, creation };
        histories.push(history);
        byCurrentId.set(equipmentId, history);
        return history;
    };

    for (const modification of splitMoves(modificationList)) {
        const equipmentId = equipmentIdOf(modification);
        if (kindOf(modification) === 'CREATION') {
            start(equipmentId, structuredClone(modification as Creation));
            continue;
        }
        const history = byCurrentId.get(equipmentId) ?? start(equipmentId);
        record(history, modification);

        byCurrentId.delete(equipmentId);
        if (!history.deletion) byCurrentId.set(history.currentId, history);
    }
    return histories;
}

function splitMoves(modificationList: readonly NetworkModification[]): NetworkModification[] {
    return modificationList.flatMap<NetworkModification>((modification) =>
        modification.type === 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS'
            ? modification.feederBays.map((bay) => ({ ...modification, feederBays: [bay] }))
            : [modification],
    );
}

function record(history: EquipmentHistory, modification: NetworkModification): void {
    switch (kindOf(modification)) {
        case 'DELETION':
            history.deletion = modification as EquipmentDeletion;
            break;
        case 'RENAME':
            history.currentId = (modification as EquipmentRename).newEquipmentId;
            break;
        case 'SWITCH_STATE':
            history.switchOpen = (modification as EquipmentAttributeModification).equipmentAttributeValue;
            break;
        case 'MOVE': {
            const move = modification as MoveVoltageLevelFeederBays;
            history.move = { voltageLevelId: move.voltageLevelId, bay: move.feederBays[0] };
            break;
        }
        case 'PROPERTIES': {
            const { type, equipmentId, ...fields } = modification as EquipmentModification;
            history.properties = { type, fields: { ...history.properties?.fields, ...fields } };
            break;
        }
    }
}

/** A rename chain like A→B, B→A cannot be replayed as single renames: the list is then kept as is. */
function hasRenameCycle(histories: readonly EquipmentHistory[]): boolean {
    const renamed = histories.filter((history) => !history.creation && history.originalId !== history.currentId);
    const freedIds = new Set(renamed.map((history) => history.originalId));
    return renamed.some((history) => freedIds.has(history.currentId));
}

function emit(history: EquipmentHistory): NetworkModification[] {
    if (history.creation && history.deletion) return [];
    if (history.creation) return emitCreated(history, history.creation);
    if (history.deletion) return [{ ...history.deletion, equipmentId: history.originalId }];
    return emitExisting(history);
}

function emitExisting(history: EquipmentHistory): NetworkModification[] {
    const emitted: NetworkModification[] = [];
    if (history.originalId !== history.currentId) emitted.push(renameOf(history));
    if (history.properties) emitted.push(propertiesOf(history, history.properties));
    if (history.switchOpen !== undefined) emitted.push(switchStateOf(history, history.switchOpen));
    if (history.move) emitted.push(moveOf(history, history.move));
    return emitted;
}

function emitCreated(history: EquipmentHistory, creation: Creation): NetworkModification[] {
    switch (creation.type) {
        case 'SWITCH_CREATION':
            return withMove(history, switchCreationOf(history, creation));
        case 'NODE_INJECTION_CREATION':
        case 'SWITCHED_INJECTION_CREATION':
            // The switch of a switched injection keeps its id: a rename only touches the injection.
            applyHistory(history, creation.creation);
            return withMove(history, creation);
        default:
            return [busbarCreationOf(history, creation)];
    }
}

function switchCreationOf(history: EquipmentHistory, creation: SwitchCreation): SwitchCreation {
    creation.equipmentId = history.currentId;
    if (history.switchOpen !== undefined) creation.open = history.switchOpen;
    return creation;
}

function busbarCreationOf(history: EquipmentHistory, creation: InjectionCreation): InjectionCreation {
    applyHistory(history, creation);
    if (history.move) applyPlacement(creation, history.move.bay);
    return creation;
}

function withMove(history: EquipmentHistory, creation: Creation): NetworkModification[] {
    return history.move ? [creation, moveOf(history, history.move)] : [creation];
}

/** Folds the rename and the property edits of a created injection into its creation. */
function applyHistory(
    history: EquipmentHistory,
    injection: { equipmentId: string; connectionName: string | null },
): void {
    injection.equipmentId = history.currentId;
    if (history.originalId !== history.currentId) injection.connectionName = history.currentId;
    if (history.properties) applyFields(injection, history.properties.fields);
}

function applyFields(target: object, fields: Record<string, unknown>): void {
    const record = target as Record<string, unknown>;
    for (const [field, change] of Object.entries(fields)) {
        // A host field the editor does not know (name, free properties...) is carried as is.
        if (!isAttributeModification(change)) record[field] = change;
        else if (change.op === 'SET') record[field] = change.value;
        else delete record[field];
    }
}

function applyPlacement(creation: InjectionCreation, bay: MoveFeederBay): void {
    if (bay.busbarSectionId !== null) creation.busOrBusbarSectionId = bay.busbarSectionId;
    creation.connectionPosition = bay.connectionPosition;
    creation.connectionDirection = bay.connectionDirection;
}

function renameOf(history: EquipmentHistory): EquipmentRename {
    return {
        type: 'EQUIPMENT_RENAME',
        equipmentId: history.originalId,
        newEquipmentId: history.currentId,
        newConnectionName: history.currentId,
    };
}

function propertiesOf(
    history: EquipmentHistory,
    properties: NonNullable<EquipmentHistory['properties']>,
): EquipmentModification {
    return { type: properties.type, equipmentId: history.currentId, ...properties.fields };
}

function switchStateOf(history: EquipmentHistory, open: boolean): EquipmentAttributeModification {
    return {
        type: 'EQUIPMENT_ATTRIBUTE_MODIFICATION',
        equipmentId: history.currentId,
        equipmentType: 'SWITCH',
        equipmentAttributeName: 'open',
        equipmentAttributeValue: open,
    };
}

function moveOf(history: EquipmentHistory, move: NonNullable<EquipmentHistory['move']>): MoveVoltageLevelFeederBays {
    return {
        type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
        voltageLevelId: move.voltageLevelId,
        feederBays: [{ ...move.bay, equipmentId: history.currentId }],
    };
}

function kindOf(modification: NetworkModification): Kind {
    if (CREATION_TYPES.has(modification.type)) return 'CREATION';
    switch (modification.type) {
        case 'EQUIPMENT_DELETION':
        case 'ELEMENT_DELETION':
            return 'DELETION';
        case 'EQUIPMENT_RENAME':
            return 'RENAME';
        case 'EQUIPMENT_ATTRIBUTE_MODIFICATION':
            return 'SWITCH_STATE';
        case 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS':
            return 'MOVE';
        default:
            return 'PROPERTIES';
    }
}

function equipmentIdOf(modification: NetworkModification): string {
    switch (modification.type) {
        case 'NODE_INJECTION_CREATION':
        case 'SWITCHED_INJECTION_CREATION':
            return modification.creation.equipmentId;
        case 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS':
            return modification.feederBays[0].equipmentId;
        default:
            return modification.equipmentId;
    }
}
