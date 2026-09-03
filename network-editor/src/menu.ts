import type { ActionSubject, EditorAction } from './core/actions';
import type { BayInsertion, EditOperation, EditTarget } from './core/types';

/** English wording for every operation. Hosts doing i18n key off `operation` instead. */
export const ACTION_LABELS: Record<EditOperation, string> = {
    CREATE_INJECTION: 'Add injection',
    CREATE_SWITCH: 'Add a switch',
    CREATE_FEEDER_BAY: 'Create a feeder bay',
    CREATE_COUPLING: 'Create a coupling',
    DELETE: 'Delete',
    DELETE_BAY: 'Delete feeder bay',
    UPDATE_PROPERTIES: 'Properties',
    UPDATE_BAY_POSITION: 'Change bay position',
    FLIP_BAY_DIRECTION: 'Flip bay direction',
    MOVE_BAY: 'Move feeder bay',
    RENAME: 'Rename',
    CREATE_SWITCHED_INJECTION: 'Add behind a switch',
};

const DANGEROUS: ReadonlySet<EditOperation> = new Set<EditOperation>(['DELETE', 'DELETE_BAY']);

export interface ActionMenuItem {
    action: EditorAction;
    /** Doubles as an i18n message id. */
    operation: EditOperation;
    subject?: ActionSubject;
    label: string;
    enabled: boolean;
    danger: boolean;
    /** true when run() needs values: open a form instead of firing the action. */
    needsForm: boolean;
}

/** Anything handing out actions — NetworkEditor satisfies it. */
export interface ActionSource {
    actionsFor(target: EditTarget, insertion?: BayInsertion): EditorAction[];
}

/** What a menu is opened on: the TargetEvent itself, or whatever the host kept of it. */
export interface MenuSubjects {
    targets: readonly EditTarget[];
    insertion?: BayInsertion;
}

export function actionLabel(action: EditorAction): string {
    const base = ACTION_LABELS[action.operation];
    if (!action.enabled) return `${base} — not implemented`;

    switch (action.subject?.kind) {
        case 'TYPE':
            return `${base}: ${action.subject.type}`;
        case 'BUSBAR':
            return `${base}: ${action.subject.busbarSectionId}`;
        case 'SELECTION':
            return `${base} (${action.subject.size})`;
        default:
            return base;
    }
}

export function describeTarget(target: EditTarget): string {
    switch (target.kind) {
        case 'NODE':
            return target.occupied ? `Node ${target.node} (taken)` : `Node ${target.node}`;
        case 'BUSBAR':
            return `Busbar ${target.busbarSectionId}`;
        case 'EQUIPMENT':
            return target.created
                ? `${target.type} ${target.equipmentId} (pending)`
                : `${target.type} ${target.equipmentId}`;
    }
}

export function describeTargets(targets: readonly EditTarget[]): string {
    return targets.map(describeTarget).join(' · ');
}

/** Every entry a host can put in a menu, already worded and flagged. */
export function menuItemsFor(source: ActionSource, subjects: MenuSubjects): ActionMenuItem[] {
    return subjects.targets
        .flatMap((target) => source.actionsFor(target, subjects.insertion))
        .map((action) => ({
            action,
            operation: action.operation,
            subject: action.subject,
            label: actionLabel(action),
            enabled: action.enabled,
            danger: DANGEROUS.has(action.operation),
            needsForm: action.form.length > 0,
        }));
}
