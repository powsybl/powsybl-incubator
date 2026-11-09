import { beforeAll, describe, expect, it } from 'vitest';
import { EditorCore } from './EditorCore.ts';
import { EditorModel } from '../model/EditorModel.ts';
import { SvgDomService } from '../dom/SvgDomService.ts';
import { actionKey, type EditorAction } from '../actions/actions.ts';
import type {
    BayPosition,
    CreateSpec,
    EditorEvent,
    EditorEventListener,
    EquipmentProperties,
    SLDMetadata,
} from '../types.ts';
import svgContent from '../../demo/data/reseau.svg?raw';
import metadata from '../../demo/data/reseau_metadata.json';
import properties from '../../demo/data/reseau_properties.json';

const BUSBAR = 'idMTPELP6_95_1';
const EXISTING_LOAD = 'MTPEL6TR611';

beforeAll(() => {
    // jsdom has neither CSS.escape nor SVG geometry.
    (globalThis as { CSS?: unknown }).CSS = { escape: (value: string) => value };
    Object.assign(SVGElement.prototype, {
        getBBox: () => ({ x: 0, y: 0, width: 10, height: 10 }),
        getCTM: () => null,
        getScreenCTM: () => null,
    });
});

function setup(options: {
    onEvent?: EditorEventListener;
    isSupported?: (action: EditorAction) => boolean;
} = {}): { core: EditorCore; model: EditorModel; container: HTMLElement } {
    const container = document.createElement('div');
    container.innerHTML = svgContent;
    const model = new EditorModel(
        structuredClone(metadata) as unknown as SLDMetadata,
        structuredClone(properties) as Record<string, EquipmentProperties>,
    );
    const symbols = {
        componentSize: () => ({ width: 10, height: 10 }),
        createSymbol: () => document.createElementNS('http://www.w3.org/2000/svg', 'g'),
    };
    return {
        core: new EditorCore(model, new SvgDomService(container), symbols, options.onEvent, options.isSupported),
        model,
        container,
    };
}

function load(provisionalId: string, extra: Partial<CreateSpec> = {}): CreateSpec {
    return { type: 'LOAD', provisionalId, properties: { p0: 10, q0: 0 }, ...extra };
}

/** Moves a pending bay to the first free order after `from`. */
function moveToFreeOrder(core: EditorCore, equipmentId: string, from: number, direction: BayPosition['direction']): number {
    let order = from + 1;
    while (!core.setBayPosition(equipmentId, { order, direction })) order++;
    return order;
}

function canTakeOrder(core: EditorCore, order: number): boolean {
    const taken = core.create(BUSBAR, 'CREATE_FEEDER_BAY', load('PROBE', { order }));
    if (taken) core.undo();
    return taken;
}

function placements(core: EditorCore): string[] {
    return core.getModifications().map((modification) => {
        const { equipmentId, connectionPosition, connectionDirection } = modification as Record<string, unknown>;
        return `${equipmentId}@${connectionPosition}/${connectionDirection}`;
    });
}

function clickNode(container: HTMLElement, nodeId: string, shiftKey = false): void {
    const node = container.querySelector<SVGElement>(`[id="${CSS.escape(nodeId)}"]`);
    if (!node) throw new Error(`Missing SVG node ${nodeId}`);
    node.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, button: 0, shiftKey }));
}

function nodeIdForEquipment(model: EditorModel, equipmentId: string): string {
    const node = model.getNodesForEquipment(equipmentId)[0];
    if (!node) throw new Error(`Missing model node for ${equipmentId}`);
    return node.id;
}

describe('EditorCore pending creations', () => {
    it('keeps a created equipment and a new one with its former id apart', () => {
        const { core, model } = setup();
        core.create(BUSBAR, 'CREATE_FEEDER_BAY', load('NEW_LOAD', { order: 45, direction: 'BOTTOM' }));
        core.renameEquipment('NEW_LOAD', 'NEW_LOAD1');
        expect(core.create(BUSBAR, 'CREATE_FEEDER_BAY', load('NEW_LOAD', { order: 47, direction: 'TOP' }))).toBe(true);

        const renamed = model.getNodesForEquipment('NEW_LOAD1')[0];
        const created = model.getNodesForEquipment('NEW_LOAD')[0];
        expect(renamed.id).not.toBe(created.id);

        const order = moveToFreeOrder(core, 'NEW_LOAD', 47, 'TOP');
        expect(placements(core)).toEqual(['NEW_LOAD1@45/BOTTOM', `NEW_LOAD@${order}/TOP`]);

        core.undo();
        core.undo();
        expect(model.getNodeById(renamed.id)).toBe(renamed);
        expect(placements(core)).toEqual(['NEW_LOAD1@45/BOTTOM']);
    });

    it('does not bring a deleted creation back when its id is reused', () => {
        const { core } = setup();
        core.create(BUSBAR, 'CREATE_FEEDER_BAY', load('X', { order: 45 }));
        core.deleteElements(['X'], 'element');
        core.create(BUSBAR, 'CREATE_FEEDER_BAY', load('X', { order: 45 }));

        const order = moveToFreeOrder(core, 'X', 45, 'BOTTOM');
        expect(placements(core)).toEqual([`X@${order}/BOTTOM`]);
        expect(canTakeOrder(core, 45)).toBe(true);
    });

    it('reserves the switch id of a switched injection', () => {
        const { core, model } = setup();
        const [first, second] = model.collectTargets().filter((target) => target.kind === 'NODE');
        const behindBreaker = (id: string) => load(id, { switchType: 'BREAKER' });

        expect(core.createSwitchedInjection(first.id, behindBreaker('X'))).toBe(true);
        core.renameEquipment('X', 'Y');

        expect(core.createSwitchedInjection(second.id, behindBreaker('X'))).toBe(false);
        expect(core.createSwitchedInjection(second.id, behindBreaker('Z'))).toBe(true);

        core.undo();
        core.undo();
        core.undo();
        expect(core.createSwitchedInjection(second.id, behindBreaker('X'))).toBe(true);
    });

    it('folds every change of a creation into one modification, step by step on undo', () => {
        const { core } = setup();
        core.create(BUSBAR, 'CREATE_FEEDER_BAY', load('X'));
        core.renameEquipment('X', 'Y');
        core.applyProperties('Y', { p0: 5 });
        expect(core.getModifications()).toEqual([expect.objectContaining({ equipmentId: 'Y', p0: 5 })]);

        core.deleteElements(['Y'], 'bay');
        expect(core.getModifications()).toEqual([]);

        core.undo();
        core.undo();
        expect(core.getModifications()).toEqual([expect.objectContaining({ equipmentId: 'Y', p0: 10 })]);
    });

    it('frees the order of a moved or deleted creation', () => {
        const { core } = setup();
        core.create(BUSBAR, 'CREATE_FEEDER_BAY', load('X'));
        const before = core.getBayPosition('X')!.order;
        const order = moveToFreeOrder(core, 'X', before, 'BOTTOM');
        expect(canTakeOrder(core, order)).toBe(false);
        expect(canTakeOrder(core, before)).toBe(true);

        core.deleteElements(['X'], 'element');
        expect(canTakeOrder(core, order)).toBe(true);
    });

    it('creates a feeder bay at the position and direction its form hands back', () => {
        const { core, model } = setup();
        const busbar = model.collectTargets().find((target) => target.id === BUSBAR)!;
        const action = core.actionsFor(busbar).find((candidate) => actionKey(candidate) === 'CREATE_FEEDER_BAY:LOAD')!;
        const proposed = action.initial.connectionPosition;
        expect(typeof proposed).toBe('number');

        expect(action.run({ ...action.initial, equipmentId: 'X', connectionDirection: 'TOP' })).toBe(true);
        expect(placements(core)).toEqual([`X@${proposed}/TOP`]);
        expect(core.getProperties('X')).not.toHaveProperty('connectionPosition');
        expect(core.getProperties('X')).not.toHaveProperty('connectionDirection');
    });

    it('proposes the very modification a feeder bay creation produces', () => {
        const { core, model } = setup();
        const busbar = model.collectTargets().find((target) => target.id === BUSBAR)!;
        const action = core.actionsFor(busbar).find((candidate) => actionKey(candidate) === 'CREATE_FEEDER_BAY:LOAD')!;
        expect(action.initial).toMatchObject({ type: 'LOAD_CREATION', busOrBusbarSectionId: expect.any(String) });

        expect(action.run(action.initial)).toBe(true);
        expect(core.getModifications()).toEqual([action.initial]);
        expect(core.getProperties(String(action.initial.equipmentId))).not.toHaveProperty('busOrBusbarSectionId');
    });
});

describe('EditorCore creations from a host form', () => {
    const FREE_PROPERTIES = [{ name: 'a', value: 'b' }];

    function feederBayAction(core: EditorCore, model: EditorModel): EditorAction {
        const busbar = model.collectTargets().find((target) => target.id === BUSBAR)!;
        return core.actionsFor(busbar).find((candidate) => actionKey(candidate) === 'CREATE_FEEDER_BAY:LOAD')!;
    }

    it('carries the fields it does not know into the modification', () => {
        const { core, model } = setup();
        const action = feederBayAction(core, model);
        action.run({ ...action.initial, equipmentId: 'X', equipmentName: 'Load X', properties: FREE_PROPERTIES });

        expect(core.getModifications()).toEqual([
            expect.objectContaining({ equipmentId: 'X', equipmentName: 'Load X', properties: FREE_PROPERTIES }),
        ]);
        expect(core.getProperties('X')).not.toHaveProperty('properties');
    });

    it('keeps the host fields through a rename and a property edit', () => {
        const { core, model } = setup();
        const action = feederBayAction(core, model);
        action.run({ ...action.initial, equipmentId: 'X', properties: FREE_PROPERTIES });
        core.renameEquipment('X', 'Y');
        core.applyProperties('Y', { p0: 5 });

        expect(core.getModifications()).toEqual([
            expect.objectContaining({ equipmentId: 'Y', p0: 5, properties: FREE_PROPERTIES }),
        ]);
    });

    it('sends the position the editor holds, not the one the form entered', () => {
        const { core, model } = setup();
        const action = feederBayAction(core, model);
        action.run({ ...action.initial, equipmentId: 'X', connectionDirection: 'TOP' });
        const { order } = core.getBayPosition('X')!;
        core.setBayPosition('X', { order, direction: 'BOTTOM' });

        expect(placements(core)).toEqual([`X@${order}/BOTTOM`]);
    });

    it('lets an untouched connection name follow the equipment id', () => {
        const { core, model } = setup();
        const action = feederBayAction(core, model);
        action.run({ ...action.initial, equipmentId: 'X' });

        expect(core.getModifications()).toEqual([expect.objectContaining({ connectionName: 'X' })]);
    });

    it('refuses a taken position, another busbar or another voltage level', () => {
        const { core, model } = setup();
        const action = feederBayAction(core, model);
        expect(action.run({ ...action.initial, equipmentId: 'X' })).toBe(true);

        expect(action.run({ ...action.initial, equipmentId: 'Y' })).toBe(false);
        expect(action.run({ ...action.initial, equipmentId: 'Z', busOrBusbarSectionId: 'OTHER' })).toBe(false);
        expect(action.run({ ...action.initial, equipmentId: 'Z', voltageLevelId: 'OTHER' })).toBe(false);
    });

    it('creates the bay at the bottom when the form leaves the direction undefined', () => {
        const { core, model } = setup();
        const action = feederBayAction(core, model);
        action.run({ ...action.initial, equipmentId: 'X', connectionDirection: 'UNDEFINED' });

        expect(core.getBayPosition('X')?.direction).toBe('BOTTOM');
    });

    it('takes every draft off the diagram on clear', () => {
        const { core, model } = setup();
        const action = feederBayAction(core, model);
        action.run({ ...action.initial, equipmentId: 'X' });
        core.clearModifications();

        expect(model.getNodesForEquipment('X')).toEqual([]);
        expect(core.getModifications()).toEqual([]);
    });
});

describe('EditorCore fork moves', () => {
    // The demo load and generator share a breaker and the busbar disconnectors: one cell, ordered by its smallest order.
    const FORK_GENERATOR = 'MTPELIN2';

    it('moves every feeder of a fork, keeping their gaps, as one step', () => {
        const { core } = setup();
        const { direction } = core.getBayPosition(EXISTING_LOAD)!;

        expect(core.setBayPosition(EXISTING_LOAD, { order: 120, direction })).toBe(true);
        expect(core.getBayPosition(EXISTING_LOAD)?.order).toBe(120);
        expect(core.getBayPosition(FORK_GENERATOR)?.order).toBe(121);

        core.undo();
        expect(core.getBayPosition(EXISTING_LOAD)?.order).toBe(90);
        expect(core.getBayPosition(FORK_GENERATOR)?.order).toBe(91);
    });

    it('refuses the move when a fork feeder would take a taken order', () => {
        const { core } = setup();
        const { direction } = core.getBayPosition(FORK_GENERATOR)!;

        // The load would land on order 110, the last bay of the section.
        expect(core.setBayPosition(FORK_GENERATOR, { order: 111, direction })).toBe(false);
        expect(core.getModifications()).toEqual([]);
    });
});

describe('EditorCore history', () => {
    it('redoes an undone step and drops the redo stack on a new edit', () => {
        const { core } = setup();
        core.renameEquipment(EXISTING_LOAD, 'A');
        const renamed = core.getModifications();

        core.undo();
        expect(core.getModifications()).toEqual([]);
        core.redo();
        expect(core.getModifications()).toEqual(renamed);

        core.undo();
        core.renameEquipment(EXISTING_LOAD, 'B');
        core.redo();
        expect(core.getModifications()).toEqual([
            expect.objectContaining({ type: 'EQUIPMENT_RENAME', newEquipmentId: 'B' }),
        ]);
    });

    it('ignores undo and redo on an empty history', () => {
        const { core } = setup();
        core.undo();
        core.redo();
        expect(core.getModifications()).toEqual([]);
    });

    it('reverts existing equipments on clear', () => {
        const { core } = setup();
        const p0 = core.getProperties(EXISTING_LOAD).p0;
        core.applyProperties(EXISTING_LOAD, { p0: 1234 });
        core.clearModifications();

        expect(core.getProperties(EXISTING_LOAD).p0).toBe(p0);
        core.redo();
        expect(core.getModifications()).toEqual([]);
    });
});

describe('EditorCore renames', () => {
    it('sends nothing for an equipment renamed back to its id', () => {
        const { core } = setup();
        core.renameEquipment(EXISTING_LOAD, 'A');
        core.renameEquipment('A', 'B');
        expect(core.getModifications()).toEqual([
            expect.objectContaining({ type: 'EQUIPMENT_RENAME', equipmentId: EXISTING_LOAD, newEquipmentId: 'B' }),
        ]);

        core.renameEquipment('B', EXISTING_LOAD);
        expect(core.getModifications()).toEqual([]);
    });
});

describe('EditorCore property edits', () => {
    const SET = <T,>(value: T) => ({ value, op: 'SET' as const });

    function editAction(core: EditorCore, model: EditorModel): EditorAction {
        const load = model.collectTargets().find(
            (target) => target.kind === 'EQUIPMENT' && target.equipmentId === EXISTING_LOAD,
        )!;
        return core.actionsFor(load).find((candidate) => actionKey(candidate) === 'UPDATE_PROPERTIES:LOAD')!;
    }

    function modification(fields: Record<string, unknown>) {
        return { type: 'LOAD_MODIFICATION', equipmentId: EXISTING_LOAD, ...fields };
    }

    it('opens on the modification setting every value the editor knows', () => {
        const { core, model } = setup();

        expect(editAction(core, model).initial).toMatchObject(
            modification({ p0: SET(core.getProperties(EXISTING_LOAD).p0) }),
        );
    });

    it('sends every field of the host modification, null ones aside', () => {
        const { core, model } = setup();
        const freeProperties = [{ name: 'a', value: 'b', added: true }];
        const applied = editAction(core, model).run(
            modification({
                p0: SET(12),
                q0: null,
                equipmentName: SET('Load A'),
                pMeasurementValue: SET(11),
                properties: freeProperties,
            }),
        );

        expect(applied).toBe(true);
        expect(core.getModifications()).toEqual([
            modification({ p0: SET(12), equipmentName: SET('Load A'), pMeasurementValue: SET(11), properties: freeProperties }),
        ]);
        expect(core.getProperties(EXISTING_LOAD)).toMatchObject({ p0: 12 });
        expect(core.getProperties(EXISTING_LOAD)).not.toHaveProperty('equipmentName');
    });

    it('applies a value the editor already holds: it may not know the real one', () => {
        const { core, model } = setup();

        expect(editAction(core, model).run(modification({ terminalConnected: SET(true) }))).toBe(true);
        expect(core.getModifications()).toEqual([modification({ terminalConnected: SET(true) })]);
    });

    it('refuses a modification without any field', () => {
        const { core, model } = setup();

        expect(editAction(core, model).run(modification({ p0: null }))).toBe(false);
        expect(core.getModifications()).toEqual([]);
    });

    // Same path as a direction change, which jsdom cannot test: a flip needs the SVG geometry.
    it('moves the bay when the modification changes its position', () => {
        const { core, model } = setup();
        const action = editAction(core, model);
        const { order, direction } = core.getBayPosition(EXISTING_LOAD)!;
        let next = order + 1;
        while (next < order + 100 && !action.run(modification({ connectionPosition: SET(next) }))) next++;

        expect(core.getBayPosition(EXISTING_LOAD)).toEqual({ order: next, direction });
        expect(core.getModifications()).toEqual([
            expect.objectContaining({
                type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
                // The load shares its cell with a generator: the fork moves as a whole.
                feederBays: [
                    expect.objectContaining({ equipmentId: EXISTING_LOAD, connectionPosition: next }),
                    expect.objectContaining({ equipmentId: 'MTPELIN2', connectionPosition: next + 1 }),
                ],
            }),
        ]);
    });

    it('moves the bay to the busbar section the modification names', () => {
        const { core, model } = setup();
        const busbars = core.moveDestinations(EXISTING_LOAD);
        const destination = busbars.find((busbar) => editAction(core, model).run(
            modification({ busOrBusbarSectionId: SET(busbar.busbarSectionId) }),
        ))!;

        expect(destination).toBeDefined();
        expect(core.getModifications()).toEqual([
            expect.objectContaining({
                type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
                feederBays: [expect.objectContaining({ equipmentId: EXISTING_LOAD, busbarSectionId: destination.busbarSectionId })],
            }),
        ]);
    });

    it('applies everything as one step, or nothing', () => {
        const { core, model } = setup();
        const action = editAction(core, model);

        const busbar = SET(core.moveDestinations(EXISTING_LOAD)[0].busbarSectionId);

        // The busbar move succeeds, the position is refused: the move is reverted.
        expect(action.run(modification({ busOrBusbarSectionId: busbar, connectionPosition: SET(-1) }))).toBe(false);
        expect(action.run(modification({ p0: SET(12), voltageLevelId: SET('OTHER_VL') }))).toBe(false);
        expect(core.getModifications()).toEqual([]);

        expect(action.run(modification({ busOrBusbarSectionId: busbar, p0: SET(12) }))).toBe(true);
        expect(core.getModifications()).toHaveLength(2);
        core.undo();
        expect(core.getModifications()).toEqual([]);
    });

    it('opens and closes a breaker through its attribute modification', () => {
        const { core, model } = setup();
        const breaker = model.collectTargets().find(
            (target) => target.kind === 'EQUIPMENT' && target.type === 'BREAKER',
        )!;
        const action = core.actionsFor(breaker).find((candidate) => actionKey(candidate) === 'UPDATE_PROPERTIES:BREAKER')!;
        const open = !action.initial.equipmentAttributeValue;

        expect(action.run({ ...action.initial, equipmentAttributeValue: open })).toBe(true);
        expect(core.getProperties(action.equipment!.id).open).toBe(open);
        expect(core.getModifications()).toEqual([
            {
                type: 'EQUIPMENT_ATTRIBUTE_MODIFICATION',
                equipmentId: action.equipment!.id,
                equipmentType: 'SWITCH',
                equipmentAttributeName: 'open',
                equipmentAttributeValue: open,
            },
        ]);
    });
});

describe('EditorCore public operations', () => {
    it('creates an injection on a node and refuses unsupported targets, types and ids', () => {
        const { core, model } = setup();
        const node = model.collectTargets().find((target) => target.kind === 'NODE')!;

        expect(core.create(node.id, 'CREATE_INJECTION', load('CREATED_ON_NODE'))).toBe(true);
        expect(core.getModifications()).toEqual([
            expect.objectContaining({
                type: 'NODE_INJECTION_CREATION',
                creation: expect.objectContaining({ type: 'LOAD_CREATION', equipmentId: 'CREATED_ON_NODE' }),
            }),
        ]);
        expect(core.create('UNKNOWN_NODE', 'CREATE_INJECTION', load('OTHER'))).toBe(false);
        expect(core.create(EXISTING_LOAD, 'CREATE_FEEDER_BAY', load('OTHER'))).toBe(false);
        expect(core.create(node.id, 'CREATE_INJECTION', { ...load('BAD_TYPE'), type: 'BREAKER' })).toBe(false);
        expect(core.create(node.id, 'CREATE_INJECTION', load(EXISTING_LOAD))).toBe(false);
    });

    it('proposes the next available equipment id', () => {
        const { core } = setup();
        expect(core.proposeEquipmentId('LOAD')).toBe('NEW_LOAD');

        expect(core.create(BUSBAR, 'CREATE_FEEDER_BAY', load('NEW_LOAD'))).toBe(true);
        expect(core.proposeEquipmentId('LOAD')).toBe('NEW_LOAD_1');
    });

    it('replaces an injection, refuses invalid replacements and restores it on undo', () => {
        const { core, model } = setup();

        expect(core.replaceEquipment(EXISTING_LOAD, load('NEW_GENERATOR'))).toBe(false);
        expect(core.replaceEquipment(EXISTING_LOAD, { ...load(EXISTING_LOAD), type: 'LOAD' })).toBe(false);
        expect(core.replaceEquipment(EXISTING_LOAD, { ...load('NEW_SWITCH'), type: 'BREAKER' })).toBe(false);
        expect(core.replaceEquipment(EXISTING_LOAD, { ...load('MTPELIN2'), type: 'GENERATOR' })).toBe(false);
        expect(core.getModifications()).toEqual([]);

        expect(core.replaceEquipment(EXISTING_LOAD, { ...load('NEW_GENERATOR'), type: 'GENERATOR' })).toBe(true);
        expect(model.getNodesForEquipment(EXISTING_LOAD)).toEqual([]);
        expect(model.getNodesForEquipment('NEW_GENERATOR')).not.toEqual([]);

        core.undo();
        expect(model.getNodesForEquipment(EXISTING_LOAD)).not.toEqual([]);
        expect(model.getNodesForEquipment('NEW_GENERATOR')).toEqual([]);
    });

    it('lists busbars in the same voltage level and moves a feeder bay', () => {
        const { core, model } = setup();
        const destinations = core.moveDestinations(EXISTING_LOAD);

        expect(destinations.length).toBeGreaterThan(0);
        const equipment = model.collectTargets().find(
            (target) => target.kind === 'EQUIPMENT' && target.equipmentId === EXISTING_LOAD,
        );
        expect(equipment?.kind).toBe('EQUIPMENT');
        if (equipment?.kind !== 'EQUIPMENT') return;
        expect(destinations.every((destination) => destination.vlId === equipment.vlId)).toBe(true);
        expect(core.moveFeederBay(EXISTING_LOAD, 'UNKNOWN_BUSBAR')).toBe(false);

        const destination = destinations.find((candidate) => core.moveFeederBay(EXISTING_LOAD, candidate.id));
        expect(destination).toBeDefined();
        expect(core.getModifications()).toEqual([
            expect.objectContaining({
                type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
                feederBays: [expect.objectContaining({
                    equipmentId: EXISTING_LOAD,
                    busbarSectionId: destination!.busbarSectionId,
                })],
            }),
        ]);
    });

    it('returns empty properties for unknown ids and refuses empty or unknown property edits', () => {
        const { core } = setup();
        expect(core.getProperties('UNKNOWN_EQUIPMENT')).toEqual({});
        expect(core.applyProperties(EXISTING_LOAD, {})).toBe(false);
        expect(core.applyProperties('UNKNOWN_EQUIPMENT', { p0: 5 })).toBe(false);
        expect(core.getModifications()).toEqual([]);
    });
});

describe('EditorCore gestures and events', () => {
    it('begins and cancels a switch gesture, and refuses invalid starts', () => {
        const events: EditorEvent[] = [];
        const { core, model } = setup({ onEvent: (event) => events.push(event) });
        const targets = model.collectTargets();
        const nodeTargets = targets.filter((target) => target.kind === 'NODE');
        const busbar = targets.find((target) => target.kind === 'BUSBAR')!;

        expect(core.createSwitch(load('NO_SWITCH_SELECTED'))).toBe(false);
        expect(core.beginSwitch(busbar.id, 'BREAKER')).toBe(false);
        expect(core.beginSwitch(nodeTargets[0].id, 'LOAD')).toBe(false);

        const start = nodeTargets.find((target) => core.beginSwitch(target.id, 'BREAKER'));
        expect(start).toBeDefined();
        expect(events.at(-1)).toMatchObject({ name: 'gesture:changed', gesture: { kind: 'SWITCH' } });

        core.cancelGesture();
        expect(events.at(-1)).toEqual({ name: 'gesture:changed', gesture: null, action: null });
    });

    it('begins bay moves for an existing feeder and a pending creation', () => {
        const { core } = setup();
        expect(core.canBeginBayMove(EXISTING_LOAD)).toBe(true);
        expect(core.beginBayMove(EXISTING_LOAD)).toBe(false);

        expect(core.create(BUSBAR, 'CREATE_FEEDER_BAY', load('PENDING_MOVE'))).toBe(true);
        expect(core.canBeginBayMove('PENDING_MOVE')).toBe(true);
        expect(core.beginBayMove('PENDING_MOVE')).toBe(false);
    });

    it('selects equipment, supports additive selection and clears it on a blank click', () => {
        const events: EditorEvent[] = [];
        const { core, model, container } = setup({ onEvent: (event) => events.push(event) });
        const firstNodeId = nodeIdForEquipment(model, EXISTING_LOAD);
        const secondNodeId = nodeIdForEquipment(model, 'MTPELIN2');

        clickNode(container, firstNodeId);
        expect(core.selectedTargets().map((target) => target.equipmentId)).toEqual([EXISTING_LOAD]);
        expect(container.querySelector(`[id="${CSS.escape(firstNodeId)}"]`)?.classList.contains('ne-selected')).toBe(true);

        clickNode(container, secondNodeId, true);
        expect(core.selectedTargets().map((target) => target.equipmentId)).toEqual([EXISTING_LOAD, 'MTPELIN2']);
        expect(events.filter((event) => event.name === 'element:selected')).toHaveLength(2);

        container.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, button: 0 }));
        expect(core.selectedTargets()).toEqual([]);
        expect(events.at(-1)).toEqual({ name: 'element:selected', elements: [] });
    });

    it('requests a context menu for supported targets and emits none when all actions are filtered', () => {
        const events: EditorEvent[] = [];
        const { core, model, container } = setup({ onEvent: (event) => events.push(event) });
        const nodeId = nodeIdForEquipment(model, EXISTING_LOAD);
        const node = container.querySelector<SVGElement>(`[id="${CSS.escape(nodeId)}"]`)!;
        const event = new MouseEvent('contextmenu', { bubbles: true, cancelable: true, clientX: 12, clientY: 34 });
        node.dispatchEvent(event);
        core.handleContextMenu(event);

        expect(events.at(-1)).toMatchObject({
            name: 'menu:requested',
            trigger: 'contextmenu',
            position: { x: 12, y: 34 },
        });
        expect((events.at(-1) as Extract<EditorEvent, { name: 'menu:requested' }>).targets.length).toBeGreaterThan(0);
        expect(event.defaultPrevented).toBe(true);

        const filteredEvents: EditorEvent[] = [];
        const filtered = setup({
            onEvent: (filteredEvent) => filteredEvents.push(filteredEvent),
            isSupported: () => false,
        });
        const filteredNodeId = nodeIdForEquipment(filtered.model, EXISTING_LOAD);
        const filteredNode = filtered.container.querySelector<SVGElement>(`[id="${CSS.escape(filteredNodeId)}"]`)!;
        const filteredEvent = new MouseEvent('contextmenu', { bubbles: true, cancelable: true });
        filteredNode.dispatchEvent(filteredEvent);
        filtered.core.handleContextMenu(filteredEvent);
        expect(filteredEvents).toEqual([]);
        expect(filteredEvent.defaultPrevented).toBe(false);
    });
});

describe('EditorCore grouped edits and deletion', () => {
    it('groups successful edits into one undo step and rolls back a failed group', () => {
        const { core } = setup();
        expect(core.group(() => {
            core.renameEquipment(EXISTING_LOAD, 'GROUPED_LOAD');
            return core.applyProperties('GROUPED_LOAD', { p0: 20 });
        })).toBe(true);
        expect(core.getModifications()).toHaveLength(2);

        core.undo();
        expect(core.getModifications()).toEqual([]);
        expect(core.getProperties(EXISTING_LOAD).p0).not.toBe(20);

        expect(core.group(() => {
            core.renameEquipment(EXISTING_LOAD, 'ROLLED_BACK_LOAD');
            return false;
        })).toBe(false);
        expect(core.getModifications()).toEqual([]);
        expect(core.getProperties(EXISTING_LOAD)).toBeDefined();
    });

    it('deletes multiple elements in one undo step and refuses unknown ids atomically', () => {
        const { core, model } = setup();
        const generatorId = 'MTPELIN2';

        expect(core.deleteElements([EXISTING_LOAD, 'UNKNOWN_EQUIPMENT'], 'element')).toBe(false);
        expect(core.getModifications()).toEqual([]);
        expect(model.getNodesForEquipment(EXISTING_LOAD)).not.toEqual([]);

        expect(core.deleteElements([EXISTING_LOAD, generatorId], 'element')).toBe(true);
        expect(model.getNodesForEquipment(EXISTING_LOAD)).toEqual([]);
        expect(model.getNodesForEquipment(generatorId)).toEqual([]);
        expect(core.getModifications()).toHaveLength(2);

        core.undo();
        expect(model.getNodesForEquipment(EXISTING_LOAD)).not.toEqual([]);
        expect(model.getNodesForEquipment(generatorId)).not.toEqual([]);
    });

    it('distinguishes deleting a bay from deleting only its element', () => {
        const { core, model } = setup();
        const candidate = model.collectTargets().find((target) =>
            target.kind === 'EQUIPMENT' &&
            core.actionsFor(target).some((action) => actionKey(action) === 'DELETE_BAY') &&
            model.collectBay(target.equipmentId).nodes.length > model.collectElementScope(target.equipmentId).nodes.length,
        );
        expect(candidate?.kind).toBe('EQUIPMENT');
        if (candidate?.kind !== 'EQUIPMENT') return;

        const ownNodeIds = model.collectElementScope(candidate.equipmentId).nodes.map((node) => node.id);
        const bayNodeIds = model.collectBay(candidate.equipmentId).nodes.map((node) => node.id);
        expect(core.deleteElements([candidate.equipmentId], 'element')).toBe(true);
        expect(ownNodeIds.every((id) => model.getNodeById(id) === undefined)).toBe(true);
        expect(bayNodeIds.some((id) => model.getNodeById(id) !== undefined)).toBe(true);

        core.undo();
        expect(core.deleteElements([candidate.equipmentId], 'bay')).toBe(true);
        expect(bayNodeIds.every((id) => model.getNodeById(id) === undefined)).toBe(true);
        core.undo();
        expect(model.getNodesForEquipment(candidate.equipmentId)).not.toEqual([]);
    });
});

describe('EditorCore lifecycle and overlays', () => {
    it('shows and removes IIDM labels and classes', () => {
        const { core, container } = setup();
        core.showIidmNodes(true);
        expect(container.querySelectorAll('text.ne-iidm-label').length).toBeGreaterThan(0);

        core.showIidmNodes(false);
        expect(container.querySelectorAll('text.ne-iidm-label')).toHaveLength(0);
        expect(container.querySelectorAll('.ne-iidm-linked, .ne-iidm-unlinked')).toHaveLength(0);
    });

    it('cleans selection DOM and stops emitting events after destroy', () => {
        const events: EditorEvent[] = [];
        const { core, model, container } = setup({ onEvent: (event) => events.push(event) });
        const nodeId = nodeIdForEquipment(model, EXISTING_LOAD);
        clickNode(container, nodeId);
        expect(container.querySelector(`[id="${CSS.escape(nodeId)}"]`)?.classList.contains('ne-selected')).toBe(true);

        core.destroy();
        const eventsAfterDestroy = events.length;
        expect(container.querySelector(`[id="${CSS.escape(nodeId)}"]`)?.classList.contains('ne-selected')).toBe(false);
        core.applyProperties(EXISTING_LOAD, { p0: 123 });
        expect(events).toHaveLength(eventsAfterDestroy);
    });
});
