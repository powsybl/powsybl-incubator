import {
    rename,
    withExtra,
    type Attributes,
    type CreateBusbarSection,
    type CreateCouplingDevice,
    type CreateFeederBay,
    type CreateInjection,
    type CreateSwitch,
    type EquipmentType,
    type Extra,
    type MoveFeederBay,
    type NetworkChange,
    type RemoveElement,
    type RemoveFeederBay,
    type UpdateAttributes,
} from './changes.ts';

type Creation = CreateFeederBay | CreateInjection | CreateSwitch | CreateBusbarSection;

type Removal = RemoveFeederBay | RemoveElement;

type Kind = 'REMOVAL' | 'RENAME' | 'CREATION' | 'COUPLING' | 'ATTRIBUTES' | 'MOVE';

const OUTPUT_ORDER: Kind[] = ['REMOVAL', 'RENAME', 'CREATION', 'COUPLING', 'ATTRIBUTES', 'MOVE'];

interface CouplingHistory {
    change: CreateCouplingDevice;
    ends: [EquipmentHistory, EquipmentHistory];
}

interface EquipmentHistory {
    originalId: string;
    currentId: string;
    creation?: Creation;
    removal?: Removal;
    update?: { equipmentType?: EquipmentType; attributes: Attributes; extra: Extra };
    move?: MoveFeederBay;
}

/**
 * Turns the raw list of edits into the shortest equivalent change set:
 * 1. group the edits by equipment, following renames;
 * 2. emit one compact history per equipment (create + remove cancel out, edits fold into creations...);
 * 3. order the result by kind, so that removals free ids before renames and creations reuse them.
 */
export function normalizeChanges(changes: readonly NetworkChange[]): NetworkChange[] {
    const { histories, couplings } = groupByEquipment(changes);
    if (hasRenameCycle(histories)) return [...changes];

    const emitted = [...histories.flatMap(emit), ...couplings.map(emitCoupling)];
    return OUTPUT_ORDER.flatMap((kind) => emitted.filter((change) => kindOf(change) === kind));
}

/** `byCurrentId` follows renames: a later edit targets the equipment by its latest id. */
function groupByEquipment(
    changes: readonly NetworkChange[],
): { histories: EquipmentHistory[]; couplings: CouplingHistory[] } {
    const histories: EquipmentHistory[] = [];
    const couplings: CouplingHistory[] = [];
    const byCurrentId = new Map<string, EquipmentHistory>();

    const start = (equipmentId: string, creation?: Creation): EquipmentHistory => {
        const history: EquipmentHistory = { originalId: equipmentId, currentId: equipmentId, creation };
        histories.push(history);
        byCurrentId.set(equipmentId, history);
        return history;
    };
    const historyOf = (equipmentId: string): EquipmentHistory => byCurrentId.get(equipmentId) ?? start(equipmentId);

    for (const change of changes) {
        if (change.kind === 'CREATE_COUPLING_DEVICE') {
            const ends: [EquipmentHistory, EquipmentHistory] = [
                historyOf(change.busOrBusbarSectionId1),
                historyOf(change.busOrBusbarSectionId2),
            ];
            couplings.push({ change, ends });
            continue;
        }

        const equipmentId = equipmentIdOf(change);
        if (kindOf(change) === 'CREATION') {
            start(equipmentId, structuredClone(change as Creation));
            continue;
        }
        const history = historyOf(equipmentId);
        record(history, change);

        byCurrentId.delete(equipmentId);
        if (!history.removal) byCurrentId.set(history.currentId, history);
    }
    return { histories, couplings };
}

function emitCoupling({ change, ends }: CouplingHistory): CreateCouplingDevice {
    return { ...change, busOrBusbarSectionId1: ends[0].currentId, busOrBusbarSectionId2: ends[1].currentId };
}

function record(history: EquipmentHistory, change: NetworkChange): void {
    switch (change.kind) {
        case 'REMOVE_FEEDER_BAY':
        case 'REMOVE_ELEMENT':
            history.removal = change;
            break;
        case 'RENAME':
            history.currentId = change.newId;
            break;
        case 'MOVE_FEEDER_BAY':
            history.move = change;
            break;
        case 'UPDATE_ATTRIBUTES':
            history.update = {
                equipmentType: change.equipmentType ?? history.update?.equipmentType,
                attributes: { ...history.update?.attributes, ...change.attributes },
                extra: { ...history.update?.extra, ...change.extra },
            };
            break;
    }
}

/** A rename chain like A→B, B→A cannot be replayed as single renames: the list is then kept as is. */
function hasRenameCycle(histories: readonly EquipmentHistory[]): boolean {
    const renamed = histories.filter((history) => !history.creation && history.originalId !== history.currentId);
    const freedIds = new Set(renamed.map((history) => history.originalId));
    return renamed.some((history) => freedIds.has(history.currentId));
}

function emit(history: EquipmentHistory): NetworkChange[] {
    if (history.creation && history.removal) return [];
    if (history.creation) return emitCreated(history, history.creation);
    if (history.removal) return [removalOf(history, history.removal)];
    return emitExisting(history);
}

function emitExisting(history: EquipmentHistory): NetworkChange[] {
    const emitted: NetworkChange[] = [];
    if (history.originalId !== history.currentId) emitted.push(rename(history.originalId, history.currentId));
    if (history.update) emitted.push(updateOf(history, history.update));
    if (history.move) emitted.push(moveOf(history, history.move));
    return emitted;
}

/** Edits of a created equipment fold into its creation: a single change creates it as it ends up. */
function emitCreated(history: EquipmentHistory, creation: Creation): NetworkChange[] {
    creation.id = history.currentId;
    switch (creation.kind) {
        case 'CREATE_BUSBAR_SECTION':
            return [creation];
        case 'CREATE_SWITCH': {
            const open = history.update?.attributes.open;
            if (typeof open === 'boolean') creation.open = open;
            return withMove(history, creation);
        }
        case 'CREATE_INJECTION':
            // The switch of an injection behind a switch keeps its id: a rename only touches the injection.
            return withMove(history, withUpdate(history, creation));
        case 'CREATE_FEEDER_BAY':
            return [withPlacement(history, withUpdate(history, creation))];
    }
}

function withUpdate<T extends CreateFeederBay | CreateInjection>(history: EquipmentHistory, creation: T): T {
    if (!history.update) return creation;
    creation.attributes = { ...creation.attributes, ...history.update.attributes };
    return withExtra(creation, { ...creation.extra, ...history.update.extra });
}

/** A created bay that is moved is created where it ends up. */
function withPlacement(history: EquipmentHistory, creation: CreateFeederBay): CreateFeederBay {
    const move = history.move;
    if (!move) return creation;
    if (move.targetBusOrBusbarSectionId !== undefined) creation.busOrBusbarSectionId = move.targetBusOrBusbarSectionId;
    if (move.positionOrder !== undefined) creation.positionOrder = move.positionOrder;
    if (move.direction !== undefined) creation.direction = move.direction;
    return creation;
}

function withMove(history: EquipmentHistory, creation: Creation): NetworkChange[] {
    return history.move ? [creation, moveOf(history, history.move)] : [creation];
}

function removalOf(history: EquipmentHistory, removal: Removal): Removal {
    return removal.kind === 'REMOVE_FEEDER_BAY'
        ? { ...removal, connectableId: history.originalId }
        : { ...removal, id: history.originalId };
}

function updateOf(history: EquipmentHistory, update: NonNullable<EquipmentHistory['update']>): UpdateAttributes {
    const change: UpdateAttributes = {
        kind: 'UPDATE_ATTRIBUTES',
        ...(update.equipmentType ? { equipmentType: update.equipmentType } : {}),
        id: history.currentId,
        attributes: update.attributes,
    };
    return withExtra(change, update.extra);
}

function moveOf(history: EquipmentHistory, move: MoveFeederBay): MoveFeederBay {
    return { ...move, connectableId: history.currentId };
}

function kindOf(change: NetworkChange): Kind {
    switch (change.kind) {
        case 'REMOVE_FEEDER_BAY':
        case 'REMOVE_ELEMENT':
            return 'REMOVAL';
        case 'RENAME':
            return 'RENAME';
        case 'CREATE_COUPLING_DEVICE':
            return 'COUPLING';
        case 'UPDATE_ATTRIBUTES':
            return 'ATTRIBUTES';
        case 'MOVE_FEEDER_BAY':
            return 'MOVE';
        default:
            return 'CREATION';
    }
}

function equipmentIdOf(change: Exclude<NetworkChange, CreateCouplingDevice>): string {
    return change.kind === 'REMOVE_FEEDER_BAY' || change.kind === 'MOVE_FEEDER_BAY' ? change.connectableId : change.id;
}
