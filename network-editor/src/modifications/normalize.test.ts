import { describe, expect, it } from 'vitest';
import type {
    AttributeModification,
    EquipmentAttributeModification,
    EquipmentDeletion,
    EquipmentModification,
    EquipmentRename,
    GeneratorCreation,
    LoadCreation,
    MoveVoltageLevelFeederBays,
    NetworkModification,
    NodeInjectionCreation,
    SwitchCreation,
    SwitchedInjectionCreation,
} from './modifications.ts';
import { normalizeModifications } from './normalize.ts';

// Helper factories for building test modifications
function load(id: string, overrides: Partial<LoadCreation> = {}): LoadCreation {
    return {
        type: 'LOAD_CREATION',
        equipmentId: id,
        equipmentName: id,
        voltageLevelId: 'VL1',
        busOrBusbarSectionId: 'BUS1',
        connectionName: id,
        connectionDirection: 'TOP',
        connectionPosition: 10,
        terminalConnected: true,
        loadType: 'UNDEFINED',
        p0: 100,
        q0: 50,
        ...overrides,
    };
}

function generator(id: string, overrides: Partial<GeneratorCreation> = {}): GeneratorCreation {
    return {
        type: 'GENERATOR_CREATION',
        equipmentId: id,
        equipmentName: id,
        voltageLevelId: 'VL1',
        busOrBusbarSectionId: 'BUS1',
        connectionName: id,
        connectionDirection: 'BOTTOM',
        connectionPosition: 20,
        terminalConnected: true,
        energySource: 'HYDRO',
        targetP: 200,
        voltageRegulationOn: true,
        minP: 0,
        maxP: 500,
        ...overrides,
    };
}

type NodeLoad = Omit<LoadCreation, 'busOrBusbarSectionId' | 'connectionPosition' | 'connectionDirection'>;

function nodeLoad(id: string): NodeLoad {
    return {
        type: 'LOAD_CREATION',
        equipmentId: id,
        equipmentName: id,
        voltageLevelId: 'VL1',
        connectionName: id,
        terminalConnected: true,
        loadType: 'UNDEFINED',
        p0: 100,
        q0: 50,
    };
}

function nodeInjection(id: string): NodeInjectionCreation {
    return {
        type: 'NODE_INJECTION_CREATION',
        voltageLevelId: 'VL1',
        node: 1,
        creation: nodeLoad(id),
    };
}

function switchedInjection(id: string, switchId = `SW_${id}`): SwitchedInjectionCreation {
    return {
        type: 'SWITCHED_INJECTION_CREATION',
        voltageLevelId: 'VL1',
        node: 1,
        switchId,
        switchKind: 'BREAKER',
        creation: nodeLoad(id),
    };
}

function switchCreation(id: string, overrides: Partial<SwitchCreation> = {}): SwitchCreation {
    return {
        type: 'SWITCH_CREATION',
        equipmentId: id,
        voltageLevelId: 'VL1',
        switchKind: 'BREAKER',
        node1: 1,
        node2: 2,
        open: false,
        ...overrides,
    };
}

function rename(equipmentId: string, newEquipmentId: string): EquipmentRename {
    return {
        type: 'EQUIPMENT_RENAME',
        equipmentId,
        newEquipmentId,
        newConnectionName: newEquipmentId,
    };
}

function modify(
    equipmentId: string,
    fields: Record<string, AttributeModification<number | string | boolean>>,
    type: EquipmentModification['type'] = 'LOAD_MODIFICATION',
): EquipmentModification {
    return {
        type,
        equipmentId,
        ...fields,
    };
}

function switchState(equipmentId: string, open: boolean): EquipmentAttributeModification {
    return {
        type: 'EQUIPMENT_ATTRIBUTE_MODIFICATION',
        equipmentId,
        equipmentType: 'SWITCH',
        equipmentAttributeName: 'open',
        equipmentAttributeValue: open,
    };
}

function move(
    voltageLevelId: string,
    equipmentId: string,
    connectionPosition: number,
    busbarSectionId: string | null = 'BUS1',
    connectionDirection: 'TOP' | 'BOTTOM' = 'TOP',
): MoveVoltageLevelFeederBays {
    return {
        type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
        voltageLevelId,
        feederBays: [
            {
                equipmentId,
                busbarSectionId,
                connectionSide: null,
                connectionPosition,
                connectionName: equipmentId,
                connectionDirection,
            },
        ],
    };
}

function del(equipmentId: string, type: 'EQUIPMENT_DELETION' | 'ELEMENT_DELETION' = 'EQUIPMENT_DELETION'): EquipmentDeletion {
    return {
        type,
        equipmentId,
        equipmentType: 'LOAD',
    };
}

describe('normalizeModifications', () => {
    describe('immutability', () => {
        it('does not mutate the input list', () => {
            const mods = [load('LOAD_1'), rename('LOAD_1', 'LOAD_2')];
            const copy = structuredClone(mods);
            normalizeModifications(mods);
            expect(mods).toEqual(copy);
        });

        it('does not mutate input creations when folding edits into them', () => {
            const mods: NetworkModification[] = [
                load('LOAD_1'),
                modify('LOAD_1', { p0: { op: 'SET', value: 999 } }),
                rename('LOAD_1', 'LOAD_2'),
                move('VL1', 'LOAD_2', 40),
            ];
            const copy = structuredClone(mods);
            normalizeModifications(mods);
            expect(mods).toEqual(copy);
        });

        it('is idempotent', () => {
            const mods: NetworkModification[] = [
                load('LOAD_NEW'),
                rename('LOAD_NEW', 'LOAD_NEW_2'),
                modify('LOAD_1', { p0: { op: 'SET', value: 10 } }),
                rename('LOAD_1', 'LOAD_1B'),
                move('VL1', 'LOAD_1B', 30),
                del('LOAD_OLD'),
            ];
            const once = normalizeModifications(mods);
            expect(normalizeModifications(once)).toEqual(once);
        });
    });

    describe('empty and clean lists', () => {
        it('returns an empty list when input is empty', () => {
            expect(normalizeModifications([])).toEqual([]);
        });

        it('preserves an already clean list of non-overlapping modifications', () => {
            const mods: NetworkModification[] = [
                del('LOAD_OLD'),
                rename('LOAD_1', 'LOAD_1_RENAMED'),
                load('LOAD_NEW'),
                modify('LOAD_2', { p0: { op: 'SET', value: 120 } }),
                switchState('SW_1', true),
                move('VL1', 'LOAD_3', 30),
            ];
            expect(normalizeModifications(mods)).toEqual(mods);
        });
    });

    describe('renames', () => {
        it('compacts consecutive renames A -> B -> C into A -> C', () => {
            const mods: NetworkModification[] = [
                rename('LOAD_A', 'LOAD_B'),
                rename('LOAD_B', 'LOAD_C'),
            ];
            expect(normalizeModifications(mods)).toEqual([rename('LOAD_A', 'LOAD_C')]);
        });

        it('cancels out cyclic rename back to original name A -> B -> A', () => {
            const mods: NetworkModification[] = [
                rename('LOAD_A', 'LOAD_B'),
                rename('LOAD_B', 'LOAD_A'),
            ];
            expect(normalizeModifications(mods)).toEqual([]);
        });

        it('returns raw modifications if renames swap ids between different equipments', () => {
            // X and Y swap their ids through TMP: cannot be replayed as single renames.
            const swapMods: NetworkModification[] = [
                rename('X', 'TMP'),
                rename('Y', 'X'),
                rename('TMP', 'Y'),
            ];
            expect(normalizeModifications(swapMods)).toEqual(swapMods);
        });

        it('still merges feeder bay moves of the same voltage level when keeping raw modifications', () => {
            const mods: NetworkModification[] = [
                rename('X', 'TMP'),
                rename('Y', 'X'),
                rename('TMP', 'Y'),
                move('VL1', 'LOAD_1', 10),
                move('VL1', 'LOAD_2', 20),
            ];
            const result = normalizeModifications(mods);
            expect(result.slice(0, 3)).toEqual(mods.slice(0, 3));
            expect(result).toHaveLength(4);
            expect((result[3] as MoveVoltageLevelFeederBays).feederBays.map((bay) => bay.equipmentId)).toEqual([
                'LOAD_1',
                'LOAD_2',
            ]);
        });

        it('does not treat reuse of a freed id by a new creation as a cycle', () => {
            const mods: NetworkModification[] = [
                rename('LOAD_A', 'LOAD_B'),
                load('LOAD_A'),
            ];
            expect(normalizeModifications(mods)).toEqual([rename('LOAD_A', 'LOAD_B'), load('LOAD_A')]);
        });
    });

    describe('properties modifications', () => {
        it('folds multiple property modifications on the same equipment into one (last field wins)', () => {
            const mods: NetworkModification[] = [
                modify('LOAD_1', { p0: { op: 'SET', value: 10 }, q0: { op: 'SET', value: 20 } }),
                modify('LOAD_1', { p0: { op: 'SET', value: 15 }, loadType: { op: 'SET', value: 'AUXILIARY' } }),
            ];
            expect(normalizeModifications(mods)).toEqual([
                modify('LOAD_1', {
                    p0: { op: 'SET', value: 15 },
                    q0: { op: 'SET', value: 20 },
                    loadType: { op: 'SET', value: 'AUXILIARY' },
                }),
            ]);
        });

        it('keeps the latest UNSET operation over an earlier SET on an existing equipment', () => {
            const mods: NetworkModification[] = [
                modify('GEN_1', { targetQ: { op: 'SET', value: 10 } }, 'GENERATOR_MODIFICATION'),
                modify('GEN_1', { targetQ: { op: 'UNSET' } }, 'GENERATOR_MODIFICATION'),
            ];
            expect(normalizeModifications(mods)).toEqual([
                modify('GEN_1', { targetQ: { op: 'UNSET' } }, 'GENERATOR_MODIFICATION'),
            ]);
        });

        it('tracks property modifications across renames and targets the latest id', () => {
            const mods: NetworkModification[] = [
                modify('LOAD_1', { p0: { op: 'SET', value: 10 } }),
                rename('LOAD_1', 'LOAD_2'),
                modify('LOAD_2', { q0: { op: 'SET', value: 20 } }),
            ];
            expect(normalizeModifications(mods)).toEqual([
                rename('LOAD_1', 'LOAD_2'),
                modify('LOAD_2', {
                    p0: { op: 'SET', value: 10 },
                    q0: { op: 'SET', value: 20 },
                }),
            ]);
        });
    });

    describe('switch state modifications', () => {
        it('keeps only the latest switch state modification for an existing switch', () => {
            const mods: NetworkModification[] = [
                switchState('SW_1', true),
                switchState('SW_1', false),
                switchState('SW_1', true),
            ];
            expect(normalizeModifications(mods)).toEqual([switchState('SW_1', true)]);
        });

        it('targets the renamed id when switch state is modified after rename', () => {
            const mods: NetworkModification[] = [
                rename('SW_1', 'SW_NEW'),
                switchState('SW_NEW', true),
            ];
            expect(normalizeModifications(mods)).toEqual([
                rename('SW_1', 'SW_NEW'),
                switchState('SW_NEW', true),
            ]);
        });
    });

    describe('feeder bay moves', () => {
        it('keeps only the latest move for a given equipment', () => {
            const mods: NetworkModification[] = [
                move('VL1', 'LOAD_1', 10),
                move('VL1', 'LOAD_1', 20),
            ];
            expect(normalizeModifications(mods)).toEqual([move('VL1', 'LOAD_1', 20)]);
        });

        it('merges moves across different equipments belonging to the same voltage level', () => {
            const mods: NetworkModification[] = [
                move('VL1', 'LOAD_1', 10),
                move('VL1', 'LOAD_2', 20),
            ];
            expect(normalizeModifications(mods)).toEqual([
                {
                    type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
                    voltageLevelId: 'VL1',
                    feederBays: [
                        {
                            equipmentId: 'LOAD_1',
                            busbarSectionId: 'BUS1',
                            connectionSide: null,
                            connectionPosition: 10,
                            connectionName: 'LOAD_1',
                            connectionDirection: 'TOP',
                        },
                        {
                            equipmentId: 'LOAD_2',
                            busbarSectionId: 'BUS1',
                            connectionSide: null,
                            connectionPosition: 20,
                            connectionName: 'LOAD_2',
                            connectionDirection: 'TOP',
                        },
                    ],
                },
            ]);
        });

        it('splits input MoveVoltageLevelFeederBays with multiple bays and folds each per equipment', () => {
            const compoundMove: MoveVoltageLevelFeederBays = {
                type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
                voltageLevelId: 'VL1',
                feederBays: [
                    {
                        equipmentId: 'LOAD_1',
                        busbarSectionId: 'BUS1',
                        connectionSide: null,
                        connectionPosition: 10,
                        connectionName: 'LOAD_1',
                        connectionDirection: 'TOP',
                    },
                    {
                        equipmentId: 'LOAD_2',
                        busbarSectionId: 'BUS1',
                        connectionSide: null,
                        connectionPosition: 20,
                        connectionName: 'LOAD_2',
                        connectionDirection: 'TOP',
                    },
                ],
            };
            const updateLoad1 = move('VL1', 'LOAD_1', 15);
            expect(normalizeModifications([compoundMove, updateLoad1])).toEqual([
                {
                    type: 'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
                    voltageLevelId: 'VL1',
                    feederBays: [
                        {
                            equipmentId: 'LOAD_1',
                            busbarSectionId: 'BUS1',
                            connectionSide: null,
                            connectionPosition: 15,
                            connectionName: 'LOAD_1',
                            connectionDirection: 'TOP',
                        },
                        {
                            equipmentId: 'LOAD_2',
                            busbarSectionId: 'BUS1',
                            connectionSide: null,
                            connectionPosition: 20,
                            connectionName: 'LOAD_2',
                            connectionDirection: 'TOP',
                        },
                    ],
                },
            ]);
        });
    });

    describe('deletions of existing equipments', () => {
        it('reduces modifications + move + delete of an existing equipment to deletion only with originalId', () => {
            const mods: NetworkModification[] = [
                modify('LOAD_1', { p0: { op: 'SET', value: 200 } }),
                move('VL1', 'LOAD_1', 30),
                del('LOAD_1'),
            ];
            expect(normalizeModifications(mods)).toEqual([del('LOAD_1')]);
        });

        it('reduces rename A -> B followed by delete B to deletion of originalId A', () => {
            const mods: NetworkModification[] = [
                rename('LOAD_A', 'LOAD_B'),
                del('LOAD_B'),
            ];
            expect(normalizeModifications(mods)).toEqual([del('LOAD_A')]);
        });

        it('supports ELEMENT_DELETION type', () => {
            const mods: NetworkModification[] = [
                rename('ELEM_A', 'ELEM_B'),
                del('ELEM_B', 'ELEMENT_DELETION'),
            ];
            expect(normalizeModifications(mods)).toEqual([del('ELEM_A', 'ELEMENT_DELETION')]);
        });
    });

    describe('creations - injection on busbar', () => {
        it('folds rename, property edits, and move into the creation itself', () => {
            const mods: NetworkModification[] = [
                load('LOAD_1', { p0: 100, q0: 50 }),
                modify('LOAD_1', { p0: { op: 'SET', value: 150 }, q0: { op: 'SET', value: 20 } }),
                rename('LOAD_1', 'LOAD_RENAMED'),
                move('VL1', 'LOAD_RENAMED', 25, 'BUS2', 'BOTTOM'),
            ];
            expect(normalizeModifications(mods)).toEqual([
                load('LOAD_RENAMED', {
                    equipmentName: 'LOAD_1',
                    busOrBusbarSectionId: 'BUS2',
                    connectionDirection: 'BOTTOM',
                    connectionPosition: 25,
                    p0: 150,
                    q0: 20,
                }),
            ]);
        });

        it('handles UNSET operation when folding property edits into creation', () => {
            const mods: NetworkModification[] = [
                generator('GEN_1', { targetQ: 50 }),
                modify('GEN_1', { targetQ: { op: 'UNSET' } }),
            ];
            const result = normalizeModifications(mods);
            expect(result).toHaveLength(1);
            expect((result[0] as GeneratorCreation).targetQ).toBeUndefined();
            expect(result[0]).not.toHaveProperty('targetQ');
        });

        it('carries host fields that are not attribute modifications as is into the creation', () => {
            const mods = [
                load('LOAD_1'),
                { ...modify('LOAD_1', {}), equipmentName: 'Pretty name' },
            ] as NetworkModification[];
            expect(normalizeModifications(mods)).toEqual([load('LOAD_1', { equipmentName: 'Pretty name' })]);
        });

        it('folds edits into a generator creation', () => {
            const mods: NetworkModification[] = [
                generator('GEN_1'),
                modify('GEN_1', { targetP: { op: 'SET', value: 300 } }, 'GENERATOR_MODIFICATION'),
            ];
            expect(normalizeModifications(mods)).toEqual([generator('GEN_1', { targetP: 300 })]);
        });

        it('keeps creation busbarSectionId if move has null busbarSectionId', () => {
            const mods: NetworkModification[] = [
                load('LOAD_1', { busOrBusbarSectionId: 'BUS_ORIGINAL' }),
                move('VL1', 'LOAD_1', 40, null, 'TOP'),
            ];
            expect(normalizeModifications(mods)).toEqual([
                load('LOAD_1', {
                    busOrBusbarSectionId: 'BUS_ORIGINAL',
                    connectionPosition: 40,
                }),
            ]);
        });
    });

    describe('creations - node injection and switched injection', () => {
        it('folds rename and property edits into NodeInjectionCreation and emits a separate move if moved', () => {
            const mods: NetworkModification[] = [
                nodeInjection('NODE_LOAD_1'),
                modify('NODE_LOAD_1', { p0: { op: 'SET', value: 180 } }),
                rename('NODE_LOAD_1', 'NODE_LOAD_2'),
                move('VL1', 'NODE_LOAD_2', 35),
            ];
            expect(normalizeModifications(mods)).toEqual([
                {
                    type: 'NODE_INJECTION_CREATION',
                    voltageLevelId: 'VL1',
                    node: 1,
                    creation: {
                        ...nodeLoad('NODE_LOAD_2'),
                        equipmentName: 'NODE_LOAD_1',
                        p0: 180,
                    },
                },
                move('VL1', 'NODE_LOAD_2', 35),
            ]);
        });

        it('folds rename and property edits into SwitchedInjectionCreation while keeping switchId unchanged', () => {
            const mods: NetworkModification[] = [
                switchedInjection('SW_LOAD_1', 'BREAKER_1'),
                modify('SW_LOAD_1', { p0: { op: 'SET', value: 250 } }),
                rename('SW_LOAD_1', 'SW_LOAD_2'),
                move('VL1', 'SW_LOAD_2', 45),
            ];
            expect(normalizeModifications(mods)).toEqual([
                {
                    type: 'SWITCHED_INJECTION_CREATION',
                    voltageLevelId: 'VL1',
                    node: 1,
                    switchId: 'BREAKER_1',
                    switchKind: 'BREAKER',
                    creation: {
                        ...nodeLoad('SW_LOAD_2'),
                        equipmentName: 'SW_LOAD_1',
                        p0: 250,
                    },
                },
                move('VL1', 'SW_LOAD_2', 45),
            ]);
        });
    });

    describe('creations - switch creation', () => {
        it('folds rename and switch state into SwitchCreation and emits a separate move if moved', () => {
            const mods: NetworkModification[] = [
                switchCreation('SW_NEW', { open: false }),
                rename('SW_NEW', 'SW_FINAL'),
                switchState('SW_FINAL', true),
                move('VL1', 'SW_FINAL', 50),
            ];
            expect(normalizeModifications(mods)).toEqual([
                switchCreation('SW_FINAL', { open: true }),
                move('VL1', 'SW_FINAL', 50),
            ]);
        });
    });

    describe('creations followed by deletions', () => {
        it('cancels out creation and deletion of the same equipment (born and died in changeSet)', () => {
            const mods: NetworkModification[] = [
                load('TEMP_LOAD'),
                modify('TEMP_LOAD', { p0: { op: 'SET', value: 300 } }),
                move('VL1', 'TEMP_LOAD', 15),
                del('TEMP_LOAD'),
            ];
            expect(normalizeModifications(mods)).toEqual([]);
        });

        it('cancels out creation + rename + delete', () => {
            const mods: NetworkModification[] = [
                load('TEMP_LOAD'),
                rename('TEMP_LOAD', 'TEMP_LOAD_2'),
                del('TEMP_LOAD_2'),
            ];
            expect(normalizeModifications(mods)).toEqual([]);
        });

        it('cancels out switch creation followed by deletion', () => {
            const mods: NetworkModification[] = [
                switchCreation('TEMP_SW'),
                del('TEMP_SW'),
            ];
            expect(normalizeModifications(mods)).toEqual([]);
        });

        it('cancels out switched injection creation + rename + delete', () => {
            const mods: NetworkModification[] = [
                switchedInjection('SW_LOAD'),
                rename('SW_LOAD', 'SW_LOAD_2'),
                del('SW_LOAD_2'),
            ];
            expect(normalizeModifications(mods)).toEqual([]);
        });
    });

    describe('deletion followed by re-creation with same ID', () => {
        it('emits deletion of old equipment followed by creation of new equipment', () => {
            const mods: NetworkModification[] = [
                del('REUSED_ID'),
                load('REUSED_ID', { p0: 500 }),
            ];
            expect(normalizeModifications(mods)).toEqual([
                del('REUSED_ID'),
                load('REUSED_ID', { p0: 500 }),
            ]);
        });

        it('applies later edits to the new equipment, not to the deleted one', () => {
            const mods: NetworkModification[] = [
                del('REUSED_ID'),
                load('REUSED_ID'),
                modify('REUSED_ID', { p0: { op: 'SET', value: 42 } }),
            ];
            expect(normalizeModifications(mods)).toEqual([del('REUSED_ID'), load('REUSED_ID', { p0: 42 })]);
        });

        it('drops switch state edits of an existing switch that is then deleted', () => {
            const mods: NetworkModification[] = [
                switchState('SW_1', true),
                del('SW_1'),
            ];
            expect(normalizeModifications(mods)).toEqual([del('SW_1')]);
        });
    });

    describe('canonical output ordering', () => {
        it('orders output strictly by DELETION, RENAME, CREATION, PROPERTIES, SWITCH_STATE, MOVE', () => {
            const mods: NetworkModification[] = [
                move('VL1', 'LOAD_MOVE', 10),
                switchState('SW_1', true),
                modify('LOAD_MOD', { p0: { op: 'SET', value: 100 } }),
                load('LOAD_NEW'),
                rename('LOAD_REN', 'LOAD_REN_2'),
                del('LOAD_DEL'),
            ];

            const result = normalizeModifications(mods);
            expect(result.map((m) => m.type)).toEqual([
                'EQUIPMENT_DELETION',
                'EQUIPMENT_RENAME',
                'LOAD_CREATION',
                'LOAD_MODIFICATION',
                'EQUIPMENT_ATTRIBUTE_MODIFICATION',
                'MOVE_VOLTAGE_LEVEL_FEEDER_BAYS',
            ]);
        });
    });
});
