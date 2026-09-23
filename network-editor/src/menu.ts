import type { ActionSubject, EditorAction } from './core/actions';
import { OPERATIONS } from './core/operations';
import type { BayInsertion, EditOperation, EditTarget } from './core/types';

export interface ActionMenuItem {
    action: EditorAction;
    operation: EditOperation;
    subject?: ActionSubject;
    label: string;
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
    const base = OPERATIONS[action.operation].label;

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
            danger: OPERATIONS[action.operation].danger ?? false,
            needsForm: action.form.length > 0,
        }));
}
