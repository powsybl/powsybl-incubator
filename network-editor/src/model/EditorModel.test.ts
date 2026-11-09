import { describe, expect, it } from 'vitest';
import { EditorModel } from './EditorModel.ts';
import { DEFAULT_LAYOUT_PARAMS } from '../types.ts';
import type {
    EquipmentProperties,
    FeederInfoMetadata,
    NodeMetadata,
    SLDMetadata,
    WireMetadata,
} from '../types.ts';

interface ModelFixture {
    metadata: SLDMetadata;
    nodes: NodeMetadata[];
    wires: WireMetadata[];
    feederInfos: FeederInfoMetadata[];
}

function makeFixture(): ModelFixture {
    const nodes: NodeMetadata[] = [
        { id: 'bbs1', equipmentId: 'BBS1', componentType: 'BUSBAR_SECTION', vid: 'VL1', iidmNode: 0, busbarIndex: 0, sectionIndex: 1 },
        { id: 'bbs2', equipmentId: 'BBS2', componentType: 'BUSBAR_SECTION', vid: 'VL1', iidmNode: 1, busbarIndex: 0, sectionIndex: 2 },
        { id: 'd1', equipmentId: 'D1', componentType: 'DISCONNECTOR', vid: 'VL1', iidmNode1: 0, iidmNode2: 7, open: false },
        { id: 'b1', equipmentId: 'BR1', componentType: 'BREAKER', vid: 'VL1', iidmNode1: 7, iidmNode2: 4, open: false },
        { id: 'lbs0', equipmentId: 'LBS0', componentType: 'LOAD_BREAK_SWITCH', vid: 'VL1', open: false },
        { id: 'load1', equipmentId: 'LOAD1', componentType: 'LOAD', vid: 'VL1', iidmNode: 4, order: 10, direction: 'BOTTOM' },
        { id: 'd2', equipmentId: 'D2', componentType: 'DISCONNECTOR', vid: 'VL1', iidmNode1: 1, iidmNode2: 8, open: true },
        { id: 'lbs1', equipmentId: 'LBS1', componentType: 'LOAD_BREAK_SWITCH', vid: 'VL1', open: false },
        { id: 'gen1', equipmentId: 'GEN1', componentType: 'GENERATOR', vid: 'VL1', iidmNode: 8, order: 20, direction: 'TOP' },
        { id: 'n1', componentType: 'NODE', vid: 'VL1', iidmNode: 7 },
        { id: 'n2', componentType: 'NODE', vid: 'VL1', iidmNode: 4 },
        { id: 'n3', componentType: 'NODE', vid: 'VL1', iidmNode: 7 },
        { id: 'n4', componentType: 'NODE', vid: 'VL1' },
        { id: 'bbs3', equipmentId: 'BBS3', componentType: 'BUSBAR_SECTION', vid: 'VL2', iidmNode: 10, busbarIndex: 0, sectionIndex: 1 },
        { id: 'bbsFake', equipmentId: 'BBS_FAKE', componentType: 'BUSBAR_SECTION', vid: 'VL2', busbarIndex: 0, sectionIndex: 2 },
        { id: 'dA', equipmentId: 'DA', componentType: 'DISCONNECTOR', vid: 'VL2', iidmNode1: 10, iidmNode2: 11, open: false },
        { id: 'sn', componentType: 'NODE', vid: 'VL2', iidmNode: 11 },
        { id: 'loadA', equipmentId: 'LOADA', componentType: 'LOAD', vid: 'VL2', iidmNode: 3, order: 10, direction: 'BOTTOM' },
        { id: 'loadB', equipmentId: 'LOADB', componentType: 'LOAD', vid: 'VL2', iidmNode: 12, order: 20, direction: 'BOTTOM' },
        { id: 'n5', componentType: 'NODE', vid: 'VL2', iidmNode: 3 },
        { id: 'fakeLoad', equipmentId: 'FAKE_LOAD', componentType: 'LOAD', vid: 'VL2', iidmNode: 13, order: 15, direction: 'TOP' },
    ];
    const wires: WireMetadata[] = [
        { id: 'w-bbs1-d1', nodeId1: 'bbs1', nodeId2: 'd1' },
        { id: 'w-d1-b1', nodeId1: 'd1', nodeId2: 'b1' },
        { id: 'w-b1-lbs0', nodeId1: 'b1', nodeId2: 'lbs0' },
        { id: 'w-lbs0-load1', nodeId1: 'lbs0', nodeId2: 'load1' },
        { id: 'w-bbs2-d2', nodeId1: 'bbs2', nodeId2: 'd2' },
        { id: 'w-d2-lbs1', nodeId1: 'd2', nodeId2: 'lbs1' },
        { id: 'w-lbs1-gen1', nodeId1: 'lbs1', nodeId2: 'gen1' },
        { id: 'w-bbs3-dA', nodeId1: 'bbs3', nodeId2: 'dA' },
        { id: 'w-dA-sn', nodeId1: 'dA', nodeId2: 'sn' },
        { id: 'w-sn-loadA', nodeId1: 'sn', nodeId2: 'loadA' },
        { id: 'w-sn-loadB', nodeId1: 'sn', nodeId2: 'loadB' },
        { id: 'w-bbsFake-fakeLoad', nodeId1: 'bbsFake', nodeId2: 'fakeLoad' },
    ];
    const feederInfos: FeederInfoMetadata[] = [
        { id: 'load1_95_ARROW_95_ACTIVE', equipmentId: 'LOAD1', componentType: 'LOAD', side: 'ONE' },
        { id: 'legacy-arrow', equipmentId: 'LOADA', componentType: 'LOAD', side: 'TWO' },
    ];
    const metadata = {
        nodes,
        wires,
        feederInfos,
        components: [{ type: 'LOAD', size: { width: 70, height: 30 } }],
        layoutParams: { cellWidth: 70 },
    };
    return { metadata: metadata as unknown as SLDMetadata, nodes, wires, feederInfos };
}

function createModel(
    fixture = makeFixture(),
    initialProperties: Record<string, EquipmentProperties> = { LOAD1: { p0: 10, q0: 2 } },
): { model: EditorModel; fixture: ModelFixture } {
    return { model: new EditorModel(fixture.metadata, initialProperties), fixture };
}

function node(model: EditorModel, id: string): NodeMetadata {
    const result = model.getNodeById(id);
    if (!result) throw new Error(`Missing node ${id}`);
    return result;
}

describe('EditorModel construction and indexes', () => {
    it('clones source metadata, indexes initial properties and switch state', () => {
        const fixture = makeFixture();
        const model = new EditorModel(fixture.metadata, { LOAD1: { p0: 10 } });
        const load1 = node(model, 'load1');

        expect(load1).not.toBe(fixture.nodes.find((entry) => entry.id === 'load1'));
        model.setFeederOrder('load1', 99);
        expect(load1.order).toBe(99);
        expect(fixture.nodes.find((entry) => entry.id === 'load1')?.order).toBe(10);
        expect(model.getProperties('LOAD1')).toEqual({ p0: 10 });
        expect(model.getProperties('D1')).toEqual({ open: false });
        expect(model.getProperties('D2')).toEqual({ open: true });
        expect(model.componentSize('LOAD')).toEqual({ width: 70, height: 30 });
        expect(model.componentSize('UNKNOWN')).toBeUndefined();
        expect(model.cellWidth()).toBe(70);
        expect(model.voltageLevelIds()).toEqual(['VL1', 'VL2']);
    });

    it('uses the default cell width when layout parameters are missing', () => {
        const fixture = makeFixture();
        const metadata = { ...fixture.metadata, layoutParams: undefined } as unknown as SLDMetadata;
        expect(new EditorModel(metadata).cellWidth()).toBe(50);
    });

    it('completes partial layout parameters with powsybl-diagram defaults', () => {
        const { model } = createModel();
        expect(model.layoutParams()).toEqual({ ...DEFAULT_LAYOUT_PARAMS, cellWidth: 70 });
    });
});

describe('EditorModel lookups and reservations', () => {
    it('resolves nodes, feeder metadata and feeder sides', () => {
        const { model } = createModel();
        expect(model.getNodeById('load1')).toBe(node(model, 'load1'));
        expect(model.getNodeById('missing')).toBeUndefined();
        expect(model.getNodesForEquipment('LOAD1').map((entry) => entry.id)).toEqual(['load1']);
        expect(model.resolveNodeForSvgId('load1')).toBe(node(model, 'load1'));
        expect(model.resolveNodeForSvgId('load1_95_ARROW_95_ACTIVE')).toBe(node(model, 'load1'));
        expect(model.resolveNodeForSvgId('legacy-arrow')).toBe(node(model, 'loadA'));
        expect(model.resolveNodeForSvgId('missing-arrow')).toBeUndefined();
        expect(model.getFeederInfoById('load1_95_ARROW_95_ACTIVE')).toEqual(model.getFeederInfosForEquipment('LOAD1')[0]);
        expect(model.getFeederInfoById('missing')).toBeUndefined();
        expect(model.getFeederInfosForEquipment('LOAD1')).toHaveLength(1);
        expect(model.feederSide(node(model, 'load1'))).toBe('ONE');
        expect(model.feederSide(node(model, 'loadA'))).toBeNull();
    });

    it('reserves and releases ids while recognizing existing equipment', () => {
        const { model } = createModel();
        expect(model.isIdTaken('LOAD1')).toBe(true);
        expect(model.isIdTaken('NEW_ID')).toBe(false);
        model.reserveId('NEW_ID');
        expect(model.isIdTaken('NEW_ID')).toBe(true);
        model.releaseId('NEW_ID');
        expect(model.isIdTaken('NEW_ID')).toBe(false);
        model.releaseId('LOAD1');
        expect(model.isIdTaken('LOAD1')).toBe(true);
    });
});

describe('EditorModel mutations', () => {
    it('updates feeder fields and ignores unknown node ids', () => {
        const { model } = createModel();
        model.setFeederDirection('load1', 'TOP');
        model.setFeederOrder('load1', 25);
        model.setComponentType('load1', 'GENERATOR');
        expect(node(model, 'load1')).toMatchObject({ direction: 'TOP', order: 25, componentType: 'GENERATOR' });

        expect(() => {
            model.setFeederDirection('unknown', 'BOTTOM');
            model.setFeederOrder('unknown', 1);
            model.setComponentType('unknown', 'LOAD');
        }).not.toThrow();
        expect(model.getNodeById('unknown')).toBeUndefined();
    });

    it('removes and adds nodes, wires and feeder infos with their indexes', () => {
        const { model } = createModel();
        const removedNode = model.removeNode('load1');
        expect(removedNode).toMatchObject({ id: 'load1', equipmentId: 'LOAD1' });
        expect(model.getNodeById('load1')).toBeUndefined();
        expect(model.getNodesForEquipment('LOAD1')).toEqual([]);
        model.addNode(removedNode!);
        expect(model.getNodesForEquipment('LOAD1')).toEqual([removedNode]);
        expect(model.removeNode('missing')).toBeUndefined();

        const removedWire = model.removeWire('w-lbs0-load1');
        expect(removedWire?.id).toBe('w-lbs0-load1');
        expect(model.collectElementScope('LOAD1').wires.map((wire) => wire.id)).toEqual([]);
        model.addWire(removedWire!);
        expect(model.collectElementScope('LOAD1').wires.map((wire) => wire.id)).toEqual(['w-lbs0-load1']);
        expect(model.removeWire('missing')).toBeUndefined();

        const removedInfo = model.removeFeederInfo('load1_95_ARROW_95_ACTIVE');
        expect(removedInfo?.equipmentId).toBe('LOAD1');
        expect(model.getFeederInfosForEquipment('LOAD1')).toEqual([]);
        expect(model.getFeederInfoById('load1_95_ARROW_95_ACTIVE')).toBeUndefined();
        model.addFeederInfo(removedInfo!);
        expect(model.getFeederInfosForEquipment('LOAD1')).toEqual([removedInfo]);
        expect(model.removeFeederInfo('missing')).toBeUndefined();
    });

    it('renames equipment across node, feeder-info and properties indexes', () => {
        const { model } = createModel();
        model.renameEquipment('LOAD1', 'RENAMED_LOAD');
        expect(model.getNodesForEquipment('LOAD1')).toEqual([]);
        expect(model.getNodesForEquipment('RENAMED_LOAD').map((entry) => entry.id)).toEqual(['load1']);
        expect(node(model, 'load1').equipmentId).toBe('RENAMED_LOAD');
        expect(model.getFeederInfosForEquipment('LOAD1')).toEqual([]);
        expect(model.getFeederInfosForEquipment('RENAMED_LOAD')[0].equipmentId).toBe('RENAMED_LOAD');
        expect(model.getFeederInfoById('load1_95_ARROW_95_ACTIVE')?.equipmentId).toBe('RENAMED_LOAD');
        expect(model.getProperties('LOAD1')).toEqual({});
        expect(model.getProperties('RENAMED_LOAD')).toEqual({ p0: 10, q0: 2 });

        model.renameEquipment('UNKNOWN', 'NO_OP');
        expect(model.getNodesForEquipment('NO_OP')).toEqual([]);
    });
});

describe('EditorModel deletion scopes and topology', () => {
    it('collects equipment and bay scopes, stopping at busbars and shared nodes', () => {
        const { model } = createModel();
        const element = model.collectElementScope('LOAD1');
        const bay = model.collectBay('LOAD1');
        expect(element.nodes.map((entry) => entry.id)).toEqual(['load1']);
        expect(element.wires.map((wire) => wire.id)).toEqual(['w-lbs0-load1']);
        expect(bay.nodes.map((entry) => entry.id)).toEqual(expect.arrayContaining(['load1', 'lbs0', 'b1', 'd1']));
        expect(bay.nodes.some((entry) => entry.id === 'bbs1')).toBe(false);
        expect(model.bayNodes(node(model, 'load1')).map((entry) => entry.id)).toEqual(expect.arrayContaining(['load1', 'lbs0', 'b1', 'd1']));
        expect(model.bayBreaker(node(model, 'load1'))?.id).toBe('b1');
        expect(model.bayBreaker(node(model, 'gen1'))?.id).toBe('lbs1');

        const sharedBay = model.collectBay('LOADA');
        expect(sharedBay.nodes.map((entry) => entry.id)).toEqual(['loadA']);
        expect(sharedBay.wires.map((wire) => wire.id)).toContain('w-sn-loadA');
        expect(sharedBay.nodes.some((entry) => entry.id === 'sn' || entry.id === 'loadB')).toBe(false);
    });

    it('finds connected busbars and switch endpoint pairs', () => {
        const { model } = createModel();
        expect(model.busbarSectionOfBay(node(model, 'load1'))).toBe('BBS1');
        expect(model.connectedBusbarSection(node(model, 'load1'))).toBe('BBS1');
        expect(model.connectedBusbarSection(node(model, 'gen1'))).toBe('BBS2');
        expect(model.busbarNodes('VL1').map((entry) => entry.id)).toEqual(['bbs1', 'bbs2']);
        expect(model.busbarNodes('VL2').map((entry) => entry.id)).toEqual(['bbs3', 'bbsFake']);
        expect(model.hasSwitchBetween('VL1', 0, 7)).toBe(true);
        expect(model.hasSwitchBetween('VL1', 7, 0)).toBe(true);
        expect(model.hasSwitchBetween('VL1', 0, 11)).toBe(false);
        expect(model.hasSwitchBetween('VL2', 0, 7)).toBe(false);
    });
});

describe('EditorModel targets and feeder lookup', () => {
    it('collects busbar, node and equipment targets with IIDM occupancy and deduplication', () => {
        const { model } = createModel();
        const targets = model.collectTargets();
        const busbars = targets.filter((target) => target.kind === 'BUSBAR');
        const nodes = targets.filter((target) => target.kind === 'NODE');
        const equipment = targets.filter((target) => target.kind === 'EQUIPMENT');

        expect(busbars.map((target) => target.id)).toEqual(['bbs1', 'bbs2', 'bbs3']);
        expect(nodes).toContainEqual({ kind: 'NODE', id: 'n1', vlId: 'VL1', node: 7, occupied: false });
        expect(nodes).toContainEqual({ kind: 'NODE', id: 'n2', vlId: 'VL1', node: 4, occupied: true });
        expect(nodes).toContainEqual({ kind: 'NODE', id: 'n5', vlId: 'VL2', node: 3, occupied: true });
        expect(nodes.some((target) => target.id === 'n3' || target.id === 'n4')).toBe(false);
        expect(equipment.map((target) => target.equipmentId)).toEqual(
            expect.arrayContaining(['LOAD1', 'GEN1', 'LOADA', 'LOADB']),
        );
        expect(model.feederNodeOf(equipment.find((target) => target.kind === 'EQUIPMENT' && target.equipmentId === 'LOAD1')!)).toBe(node(model, 'load1'));
        expect(model.collectNodeStatus().get('n4')).toEqual({ iidmNode: undefined, hidden: true });
        expect(model.collectNodeStatus().get('load1')).toEqual({ iidmNode: 4, hidden: false });
    });

    it('orders feeder nodes and resolves slots and node-less equipment targets', () => {
        const { model } = createModel();
        expect(model.feedersInSection({ vlId: 'VL1', sectionIndex: 1 }).map((entry) => entry.id)).toEqual(['load1']);
        expect(model.feedersInSection({ vlId: 'VL1', sectionIndex: 2 }).map((entry) => entry.id)).toEqual(['gen1']);
        expect(model.slotOfFeeder(node(model, 'load1'))).toEqual({ vlId: 'VL1', sectionIndex: 1 });
        expect(model.slotOfFeeder(node(model, 'd1'))).toBeUndefined();
        expect(model.feederNodes('VL1', 'TOP').map((entry) => entry.id)).toEqual(['gen1']);
        expect(model.feederNodes('VL2', 'BOTTOM').map((entry) => entry.id)).toEqual(['loadA', 'loadB']);

        const equipmentTarget = model.collectTargets().find(
            (target) => target.kind === 'EQUIPMENT' && target.equipmentId === 'LOAD1',
        );
        expect(equipmentTarget?.kind).toBe('EQUIPMENT');
        if (equipmentTarget?.kind === 'EQUIPMENT') {
            expect(model.feederNodeOf(equipmentTarget)).toBe(node(model, 'load1'));
            expect(model.feederNodeOf({ ...equipmentTarget, order: 999 })).toBeUndefined();
        }
    });
});

describe('EditorModel feeder order allocation', () => {
    it('enforces occupied, cross-section and integer order constraints', () => {
        const { model } = createModel();
        const section1 = { vlId: 'VL1', sectionIndex: 1 };
        const section2 = { vlId: 'VL1', sectionIndex: 2 };
        expect(model.isOrderAvailable(section1, 10)).toBe(false);
        expect(model.isOrderAvailable(section1, 20)).toBe(false);
        expect(model.isOrderAvailable(section1, 15)).toBe(true);
        expect(model.isOrderAvailable(section2, 10)).toBe(false);
        expect(model.isOrderAvailable(section2, 15)).toBe(true);
        expect(model.isOrderAvailable(section2, -1)).toBe(false);
        expect(model.isOrderAvailable(section2, 1.5)).toBe(false);

        expect(model.isOrderAvailable({ vlId: 'VL2', sectionIndex: 1 }, 16)).toBe(true);
        expect(model.isOrderAvailable({ vlId: 'VL2', sectionIndex: 2 }, 11)).toBe(true);
        expect(model.nextOrderForBusbar(section1)).toBe(19);
        expect(model.orderBetween(section1, 10, 20)).toBe(15);
        expect(model.orderBetween(section1, undefined, 15)).toBe(5);
        expect(model.orderBetween(section1, 10, 11)).toBeUndefined();
        expect(model.orderBetween(section1)).toBe(19);
    });

    it('applies pending order claims and vacated equipment ids', () => {
        const { model } = createModel();
        const slot = { vlId: 'VL1', sectionIndex: 2 };
        const pending = {
            claims: [{ vlId: 'VL1', sectionIndex: 2, order: 30 }],
            vacated: new Set(['gen1']),
        };
        expect(model.isOrderAvailable(slot, 20, pending)).toBe(true);
        expect(model.isOrderAvailable(slot, 30, pending)).toBe(false);
        expect(model.nextOrderForBusbar(slot, pending)).toBe(40);
    });
});

describe('EditorModel properties', () => {
    it('returns defensive copies, replaces/merges values and clears undefined keys', () => {
        const { model } = createModel();
        const properties = model.getProperties('LOAD1');
        properties.p0 = 999;
        expect(model.getProperties('LOAD1')).toEqual({ p0: 10, q0: 2 });

        model.replaceProperties('LOAD1', { p0: 20, enabled: true });
        expect(model.getProperties('LOAD1')).toEqual({ p0: 20, enabled: true });
        model.mergeProperties('LOAD1', { q0: 3 });
        expect(model.getProperties('LOAD1')).toEqual({ p0: 20, enabled: true, q0: 3 });
        model.mergeProperties('LOAD1', { enabled: undefined } as unknown as EquipmentProperties);
        expect(model.getProperties('LOAD1')).toEqual({ p0: 20, q0: 3 });
        model.clearProperties('LOAD1');
        expect(model.getProperties('LOAD1')).toEqual({});
    });

    it('synchronizes switch open state when properties are replaced or merged', () => {
        const { model } = createModel();
        const disconnect = node(model, 'd1');
        model.replaceProperties('D1', { open: true, tag: 'changed' });
        expect(disconnect.open).toBe(true);
        model.mergeProperties('D1', { open: false });
        expect(model.getProperties('D1')).toEqual({ open: false, tag: 'changed' });
        expect(disconnect.open).toBe(false);
        model.clearProperties('D1');
        expect(model.getProperties('D1')).toEqual({});
        expect(disconnect.open).toBe(false);
    });
});