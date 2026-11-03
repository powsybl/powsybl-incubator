import { beforeAll, describe, expect, it } from 'vitest';
import { EditorCore } from './EditorCore.ts';
import { EditorModel } from '../model/EditorModel.ts';
import { SvgDomService } from '../dom/SvgDomService.ts';
import { actionKey, type EditorAction } from '../actions/actions.ts';
import type { BayPosition, CreateSpec, EquipmentProperties, SLDMetadata } from '../types.ts';
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

function setup(): { core: EditorCore; model: EditorModel } {
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
    return { core: new EditorCore(model, new SvgDomService(container), symbols), model };
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
