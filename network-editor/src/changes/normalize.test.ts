import { describe, expect, it } from 'vitest';
import type {
    Attributes,
    CreateCouplingDevice,
    CreateFeederBay,
    CreateInjection,
    CreateSwitch,
    MoveFeederBay,
    NetworkChange,
    RemoveElement,
    RemoveFeederBay,
    Rename,
    UpdateAttributes,
} from './changes.ts';
import { normalizeChanges } from './normalize.ts';

// Helper factories for building test changes
function load(id: string, overrides: Partial<CreateFeederBay> = {}): CreateFeederBay {
    return {
        kind: 'CREATE_FEEDER_BAY',
        equipmentType: 'LOAD',
        id,
        voltageLevelId: 'VL1',
        busOrBusbarSectionId: 'BUS1',
        positionOrder: 10,
        direction: 'TOP',
        attributes: { loadType: 'UNDEFINED', p0: 100, q0: 50 },
        ...overrides,
    };
}

function generator(id: string, attributes: Attributes = {}): CreateFeederBay {
    return {
        kind: 'CREATE_FEEDER_BAY',
        equipmentType: 'GENERATOR',
        id,
        voltageLevelId: 'VL1',
        busOrBusbarSectionId: 'BUS1',
        positionOrder: 20,
        direction: 'BOTTOM',
        attributes: { energySource: 'HYDRO', targetP: 200, voltageRegulatorOn: true, minP: 0, maxP: 500, ...attributes },
    };
}

function nodeLoad(id: string, overrides: Partial<CreateInjection> = {}): CreateInjection {
    return {
        kind: 'CREATE_INJECTION',
        equipmentType: 'LOAD',
        id,
        voltageLevelId: 'VL1',
        node: 1,
        attributes: { loadType: 'UNDEFINED', p0: 100, q0: 50 },
        ...overrides,
    };
}

function switchedLoad(id: string, switchId = `SW_${id}`): CreateInjection {
    return nodeLoad(id, { switch: { id: switchId, kind: 'BREAKER' } });
}

function switchCreation(id: string, overrides: Partial<CreateSwitch> = {}): CreateSwitch {
    return {
        kind: 'CREATE_SWITCH',
        id,
        voltageLevelId: 'VL1',
        switchKind: 'BREAKER',
        node1: 1,
        node2: 2,
        open: false,
        ...overrides,
    };
}

function rename(id: string, newId: string): Rename {
    return { kind: 'RENAME', id, newId };
}

function update(id: string, attributes: Attributes, equipmentType: UpdateAttributes['equipmentType'] = 'LOAD'): UpdateAttributes {
    return { kind: 'UPDATE_ATTRIBUTES', equipmentType, id, attributes };
}

function switchState(id: string, open: boolean): UpdateAttributes {
    return update(id, { open }, 'SWITCH');
}

function move(
    connectableId: string,
    positionOrder: number,
    targetBusOrBusbarSectionId: string | null = 'BUS1',
    direction: 'TOP' | 'BOTTOM' = 'TOP',
): MoveFeederBay {
    return {
        kind: 'MOVE_FEEDER_BAY',
        connectableId,
        targetVoltageLevelId: 'VL1',
        ...(targetBusOrBusbarSectionId === null ? {} : { targetBusOrBusbarSectionId }),
        positionOrder,
        direction,
    };
}

function removeBay(connectableId: string): RemoveFeederBay {
    return { kind: 'REMOVE_FEEDER_BAY', connectableId, equipmentType: 'LOAD' };
}

function removeElement(id: string): RemoveElement {
    return { kind: 'REMOVE_ELEMENT', id, equipmentType: 'SWITCH' };
}

describe('normalizeChanges', () => {
    describe('immutability', () => {
        it('does not mutate the input list', () => {
            const changes = [load('LOAD_1'), rename('LOAD_1', 'LOAD_2')];
            const copy = structuredClone(changes);
            normalizeChanges(changes);
            expect(changes).toEqual(copy);
        });

        it('does not mutate input creations when folding edits into them', () => {
            const changes: NetworkChange[] = [
                load('LOAD_1'),
                update('LOAD_1', { p0: 999 }),
                rename('LOAD_1', 'LOAD_2'),
                move('LOAD_2', 40),
            ];
            const copy = structuredClone(changes);
            normalizeChanges(changes);
            expect(changes).toEqual(copy);
        });

        it('is idempotent', () => {
            const changes: NetworkChange[] = [
                load('LOAD_NEW'),
                rename('LOAD_NEW', 'LOAD_NEW_2'),
                update('LOAD_1', { p0: 10 }),
                rename('LOAD_1', 'LOAD_1B'),
                move('LOAD_1B', 30),
                removeBay('LOAD_OLD'),
            ];
            const once = normalizeChanges(changes);
            expect(normalizeChanges(once)).toEqual(once);
        });
    });

    describe('empty and clean lists', () => {
        it('returns an empty list when input is empty', () => {
            expect(normalizeChanges([])).toEqual([]);
        });

        it('preserves an already clean list of non-overlapping changes', () => {
            const changes: NetworkChange[] = [
                removeBay('LOAD_OLD'),
                rename('LOAD_1', 'LOAD_1_RENAMED'),
                load('LOAD_NEW'),
                update('LOAD_2', { p0: 120 }),
                switchState('SW_1', true),
                move('LOAD_3', 30),
            ];
            expect(normalizeChanges(changes)).toEqual(changes);
        });
    });

    describe('renames', () => {
        it('compacts consecutive renames A -> B -> C into A -> C', () => {
            expect(normalizeChanges([rename('LOAD_A', 'LOAD_B'), rename('LOAD_B', 'LOAD_C')])).toEqual([
                rename('LOAD_A', 'LOAD_C'),
            ]);
        });

        it('cancels out cyclic rename back to original name A -> B -> A', () => {
            expect(normalizeChanges([rename('LOAD_A', 'LOAD_B'), rename('LOAD_B', 'LOAD_A')])).toEqual([]);
        });

        it('returns raw changes if renames swap ids between different equipments', () => {
            // X and Y swap their ids through TMP: cannot be replayed as single renames.
            const swap: NetworkChange[] = [rename('X', 'TMP'), rename('Y', 'X'), rename('TMP', 'Y'), move('LOAD_1', 10)];
            expect(normalizeChanges(swap)).toEqual(swap);
        });

        it('does not treat reuse of a freed id by a new creation as a cycle', () => {
            const changes: NetworkChange[] = [rename('LOAD_A', 'LOAD_B'), load('LOAD_A')];
            expect(normalizeChanges(changes)).toEqual([rename('LOAD_A', 'LOAD_B'), load('LOAD_A')]);
        });
    });

    describe('attribute updates', () => {
        it('folds multiple updates on the same equipment into one (last attribute wins)', () => {
            const changes: NetworkChange[] = [
                update('LOAD_1', { p0: 10, q0: 20 }),
                update('LOAD_1', { p0: 15, loadType: 'AUXILIARY' }),
            ];
            expect(normalizeChanges(changes)).toEqual([update('LOAD_1', { p0: 15, q0: 20, loadType: 'AUXILIARY' })]);
        });

        it('tracks updates across renames and targets the latest id', () => {
            const changes: NetworkChange[] = [
                update('LOAD_1', { p0: 10 }),
                rename('LOAD_1', 'LOAD_2'),
                update('LOAD_2', { q0: 20 }),
            ];
            expect(normalizeChanges(changes)).toEqual([rename('LOAD_1', 'LOAD_2'), update('LOAD_2', { p0: 10, q0: 20 })]);
        });

        it('merges the host extra data of successive updates', () => {
            const changes: NetworkChange[] = [
                { ...update('LOAD_1', { p0: 10 }), extra: { name: 'A', properties: [1] } },
                { ...update('LOAD_1', {}), extra: { name: 'B' } },
            ];
            expect(normalizeChanges(changes)).toEqual([
                { ...update('LOAD_1', { p0: 10 }), extra: { name: 'B', properties: [1] } },
            ]);
        });
    });

    describe('switch state', () => {
        it('keeps only the latest state of an existing switch', () => {
            const changes = [switchState('SW_1', true), switchState('SW_1', false), switchState('SW_1', true)];
            expect(normalizeChanges(changes)).toEqual([switchState('SW_1', true)]);
        });

        it('targets the renamed id when the switch state changes after a rename', () => {
            const changes: NetworkChange[] = [rename('SW_1', 'SW_NEW'), switchState('SW_NEW', true)];
            expect(normalizeChanges(changes)).toEqual([rename('SW_1', 'SW_NEW'), switchState('SW_NEW', true)]);
        });
    });

    describe('feeder bay moves', () => {
        it('keeps only the latest move for a given equipment', () => {
            expect(normalizeChanges([move('LOAD_1', 10), move('LOAD_1', 20)])).toEqual([move('LOAD_1', 20)]);
        });

        it('keeps one move per equipment', () => {
            expect(normalizeChanges([move('LOAD_1', 10), move('LOAD_2', 20), move('LOAD_1', 15)])).toEqual([
                move('LOAD_1', 15),
                move('LOAD_2', 20),
            ]);
        });
    });

    describe('removals of existing equipments', () => {
        it('reduces update + move + removal of an existing equipment to the removal of its original id', () => {
            const changes: NetworkChange[] = [update('LOAD_1', { p0: 200 }), move('LOAD_1', 30), removeBay('LOAD_1')];
            expect(normalizeChanges(changes)).toEqual([removeBay('LOAD_1')]);
        });

        it('reduces rename A -> B followed by removal of B to the removal of A', () => {
            expect(normalizeChanges([rename('LOAD_A', 'LOAD_B'), removeBay('LOAD_B')])).toEqual([removeBay('LOAD_A')]);
        });

        it('supports the removal of a single element', () => {
            expect(normalizeChanges([rename('ELEM_A', 'ELEM_B'), removeElement('ELEM_B')])).toEqual([
                removeElement('ELEM_A'),
            ]);
        });
    });

    describe('creations - feeder bay', () => {
        it('folds rename, attribute updates and move into the creation itself', () => {
            const changes: NetworkChange[] = [
                load('LOAD_1'),
                update('LOAD_1', { p0: 150, q0: 20 }),
                rename('LOAD_1', 'LOAD_RENAMED'),
                move('LOAD_RENAMED', 25, 'BUS2', 'BOTTOM'),
            ];
            expect(normalizeChanges(changes)).toEqual([
                load('LOAD_RENAMED', {
                    busOrBusbarSectionId: 'BUS2',
                    direction: 'BOTTOM',
                    positionOrder: 25,
                    attributes: { loadType: 'UNDEFINED', p0: 150, q0: 20 },
                }),
            ]);
        });

        it('carries host extra data as is into the creation', () => {
            const changes: NetworkChange[] = [load('LOAD_1'), { ...update('LOAD_1', {}), extra: { name: 'Pretty name' } }];
            expect(normalizeChanges(changes)).toEqual([{ ...load('LOAD_1'), extra: { name: 'Pretty name' } }]);
        });

        it('folds updates into a generator creation', () => {
            const changes: NetworkChange[] = [generator('GEN_1'), update('GEN_1', { targetP: 300 }, 'GENERATOR')];
            expect(normalizeChanges(changes)).toEqual([generator('GEN_1', { targetP: 300 })]);
        });

        it('keeps the creation busbar section when the move does not change it', () => {
            const changes: NetworkChange[] = [load('LOAD_1', { busOrBusbarSectionId: 'BUS_ORIGINAL' }), move('LOAD_1', 40, null)];
            expect(normalizeChanges(changes)).toEqual([load('LOAD_1', { busOrBusbarSectionId: 'BUS_ORIGINAL', positionOrder: 40 })]);
        });
    });

    describe('creations - injection on a node, behind a switch or not', () => {
        it('folds rename and updates into the creation and emits a separate move', () => {
            const changes: NetworkChange[] = [
                nodeLoad('NODE_LOAD_1'),
                update('NODE_LOAD_1', { p0: 180 }),
                rename('NODE_LOAD_1', 'NODE_LOAD_2'),
                move('NODE_LOAD_2', 35),
            ];
            expect(normalizeChanges(changes)).toEqual([
                nodeLoad('NODE_LOAD_2', { attributes: { loadType: 'UNDEFINED', p0: 180, q0: 50 } }),
                move('NODE_LOAD_2', 35),
            ]);
        });

        it('keeps the switch id unchanged when the injection behind it is renamed', () => {
            const changes: NetworkChange[] = [
                switchedLoad('SW_LOAD_1', 'BREAKER_1'),
                update('SW_LOAD_1', { p0: 250 }),
                rename('SW_LOAD_1', 'SW_LOAD_2'),
                move('SW_LOAD_2', 45),
            ];
            expect(normalizeChanges(changes)).toEqual([
                nodeLoad('SW_LOAD_2', {
                    switch: { id: 'BREAKER_1', kind: 'BREAKER' },
                    attributes: { loadType: 'UNDEFINED', p0: 250, q0: 50 },
                }),
                move('SW_LOAD_2', 45),
            ]);
        });
    });

    describe('creations - switch and busbar section', () => {
        it('folds rename and switch state into the switch creation and emits a separate move', () => {
            const changes: NetworkChange[] = [
                switchCreation('SW_NEW', { open: false }),
                rename('SW_NEW', 'SW_FINAL'),
                switchState('SW_FINAL', true),
                move('SW_FINAL', 50),
            ];
            expect(normalizeChanges(changes)).toEqual([switchCreation('SW_FINAL', { open: true }), move('SW_FINAL', 50)]);
        });

        it('folds a rename into the busbar section creation', () => {
            const busbar = (id: string): NetworkChange => ({
                kind: 'CREATE_BUSBAR_SECTION',
                id,
                voltageLevelId: 'VL1',
                busbarIndex: 3,
                sectionIndex: 1,
            });
            expect(normalizeChanges([busbar('BBS3'), rename('BBS3', 'BBS_NEW')])).toEqual([busbar('BBS_NEW')]);
        });
    });

    describe('creations followed by removals', () => {
        it('cancels out creation and removal of the same equipment', () => {
            const changes: NetworkChange[] = [
                load('TEMP_LOAD'),
                update('TEMP_LOAD', { p0: 300 }),
                move('TEMP_LOAD', 15),
                removeBay('TEMP_LOAD'),
            ];
            expect(normalizeChanges(changes)).toEqual([]);
        });

        it('cancels out creation + rename + removal', () => {
            const changes: NetworkChange[] = [load('TEMP_LOAD'), rename('TEMP_LOAD', 'TEMP_LOAD_2'), removeBay('TEMP_LOAD_2')];
            expect(normalizeChanges(changes)).toEqual([]);
        });

        it('cancels out switch creation followed by removal', () => {
            expect(normalizeChanges([switchCreation('TEMP_SW'), removeElement('TEMP_SW')])).toEqual([]);
        });

        it('cancels out injection behind a switch + rename + removal', () => {
            const changes: NetworkChange[] = [switchedLoad('SW_LOAD'), rename('SW_LOAD', 'SW_LOAD_2'), removeBay('SW_LOAD_2')];
            expect(normalizeChanges(changes)).toEqual([]);
        });
    });

    describe('removal followed by re-creation with the same id', () => {
        it('emits the removal of the old equipment followed by the creation of the new one', () => {
            const changes: NetworkChange[] = [removeBay('REUSED_ID'), load('REUSED_ID')];
            expect(normalizeChanges(changes)).toEqual([removeBay('REUSED_ID'), load('REUSED_ID')]);
        });

        it('applies later edits to the new equipment, not to the removed one', () => {
            const changes: NetworkChange[] = [removeBay('REUSED_ID'), load('REUSED_ID'), update('REUSED_ID', { p0: 42 })];
            expect(normalizeChanges(changes)).toEqual([
                removeBay('REUSED_ID'),
                load('REUSED_ID', { attributes: { loadType: 'UNDEFINED', p0: 42, q0: 50 } }),
            ]);
        });

        it('drops the state change of an existing switch that is then removed', () => {
            expect(normalizeChanges([switchState('SW_1', true), removeElement('SW_1')])).toEqual([removeElement('SW_1')]);
        });
    });

    it('orders the output by removal, rename, creation, coupling, attributes, move', () => {
        const changes: NetworkChange[] = [
            move('LOAD_MOVE', 10),
            update('LOAD_MOD', { p0: 100 }),
            { kind: 'CREATE_COUPLING_DEVICE', voltageLevelId: 'VL1', busOrBusbarSectionId1: 'B1', busOrBusbarSectionId2: 'B2' },
            load('LOAD_NEW'),
            rename('LOAD_REN', 'LOAD_REN_2'),
            removeBay('LOAD_DEL'),
        ];
        expect(normalizeChanges(changes).map((change) => change.kind)).toEqual([
            'REMOVE_FEEDER_BAY',
            'RENAME',
            'CREATE_FEEDER_BAY',
            'CREATE_COUPLING_DEVICE',
            'UPDATE_ATTRIBUTES',
            'MOVE_FEEDER_BAY',
        ]);
    });

    describe('coupling', () => {
        const coupling = (id1: string, id2: string): CreateCouplingDevice => ({
            kind: 'CREATE_COUPLING_DEVICE',
            voltageLevelId: 'VL1',
            busOrBusbarSectionId1: id1,
            busOrBusbarSectionId2: id2,
        });

        it('follows a busbar renamed after the coupling', () => {
            expect(normalizeChanges([coupling('BBS1', 'BBS2'), rename('BBS2', 'X')])).toEqual([
                rename('BBS2', 'X'),
                coupling('BBS1', 'X'),
            ]);
        });

        it('keeps a busbar renamed before the coupling', () => {
            expect(normalizeChanges([rename('BBS2', 'X'), coupling('BBS1', 'X')])).toEqual([
                rename('BBS2', 'X'),
                coupling('BBS1', 'X'),
            ]);
        });
    });
});
