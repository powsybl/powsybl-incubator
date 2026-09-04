import type { ActionSubject, EditorAction } from './core/actions';
import type { BayInsertion, EditOperation, EditTarget } from './core/types';

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
    operation: EditOperation;
    subject?: ActionSubject;
    label: string;
    enabled: boolean;
    danger: boolean;
    needsForm: boolean;
}

export interface ActionSource {
    getTargets(): EditTarget[];
    actionsFor(target: EditTarget, insertion?: BayInsertion): EditorAction[];
}

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

export function actionFor(
    source: ActionSource,
    equipmentId: string | null,
    operation: EditOperation,
): EditorAction | null {
    const target = source.getTargets().find((candidate) => candidate.id === equipmentId);
    if (!target) return null;
    return source.actionsFor(target).find((action) => action.operation === operation) ?? null;
}

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
