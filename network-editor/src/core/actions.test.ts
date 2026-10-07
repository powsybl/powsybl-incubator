import { describe, expect, it } from 'vitest';
import { buildActions, type ActionHost } from './actions';
import type { BusbarTarget } from './types';

const busbar: BusbarTarget = {
    kind: 'BUSBAR',
    id: 'VL1_BBS1',
    vlId: 'VL1',
    sectionIndex: 0,
    busbarSectionId: 'VL1_BBS1',
    busbarIndex: 0,
    node: 0,
};

// Only the methods buildActions reads for a busbar target.
const host = {
    selectedTargets: () => [],
    proposedOrder: () => 25,
} as unknown as ActionHost;

describe('buildActions placement', () => {
    it('exposes the busbar placement of a feeder bay creation', () => {
        const load = buildActions(host, busbar).find(
            (action) =>
                action.operation === 'CREATE_FEEDER_BAY' &&
                action.subject?.kind === 'TYPE' &&
                action.subject.type === 'LOAD',
        );

        expect(load?.placement).toEqual({
            elementType: 'LOAD',
            vlId: 'VL1',
            busbarSectionId: 'VL1_BBS1',
            order: 25,
            direction: 'BOTTOM',
        });
    });
});
