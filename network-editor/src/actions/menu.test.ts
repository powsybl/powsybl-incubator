import { describe, expect, it } from 'vitest';
import type { ActionSubject, EditorAction } from './actions.ts';
import { actionLabel, groupMenuItems, type ActionMenuItem } from './menu.ts';
import type { EditOperation } from '../types.ts';

function item(operation: EditOperation, subject?: ActionSubject, danger = false): ActionMenuItem {
    const action: EditorAction = {
        id: `${operation}:${subject?.kind ?? ''}`,
        operation,
        subject,
        form: [],
        initial: {},
        equipment: { id: 'LOAD1', type: 'LOAD', vlId: 'VL1' },
        run: () => true,
    };
    return { action, label: actionLabel(action), danger, needsForm: false };
}

describe('groupMenuItems', () => {
    it('groups the items of an operation under its label, labelled by their subject', () => {
        const groups = groupMenuItems([
            item('CREATE_INJECTION', { kind: 'TYPE', type: 'LOAD' }),
            item('CREATE_SWITCH', { kind: 'TYPE', type: 'BREAKER' }),
            item('CREATE_INJECTION', { kind: 'TYPE', type: 'GENERATOR' }),
        ]);

        expect(groups.map((group) => group.label)).toEqual(['Add injection', 'Add a switch']);
        expect(groups[0].entries.map((entry) => entry.label)).toEqual(['LOAD', 'GENERATOR']);
    });

    it('keeps the full label of an item alone in its group', () => {
        const [group] = groupMenuItems([item('CREATE_SWITCH', { kind: 'TYPE', type: 'BREAKER' })]);

        expect(group.entries.map((entry) => entry.label)).toEqual(['Add a switch: BREAKER']);
    });

    it('labels bay moves by busbar and equipment edits by equipment', () => {
        const groups = groupMenuItems([
            item('MOVE_BAY', { kind: 'BUSBAR', busbarSectionId: 'BBS1' }),
            item('MOVE_BAY', { kind: 'BUSBAR', busbarSectionId: 'BBS2' }),
            item('UPDATE_PROPERTIES'),
            item('UPDATE_PROPERTIES'),
        ]);

        expect(groups[0].entries.map((entry) => entry.label)).toEqual(['BBS1', 'BBS2']);
        expect(groups[1].entries.map((entry) => entry.label)).toEqual(['LOAD1', 'LOAD1']);
    });

    it('puts the dangerous groups last', () => {
        const groups = groupMenuItems([
            item('DELETE', undefined, true),
            item('RENAME'),
            item('DELETE_BAY', undefined, true),
            item('UPDATE_PROPERTIES'),
        ]);

        expect(groups.map((group) => group.operation)).toEqual(['RENAME', 'UPDATE_PROPERTIES', 'DELETE', 'DELETE_BAY']);
    });
});
