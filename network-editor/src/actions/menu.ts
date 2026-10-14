import type { EditorAction } from './actions.ts';
import { OPERATIONS } from './operations.ts';
import type { EditTarget } from '../types.ts';

export interface ActionMenuItem {
    action: EditorAction;
    label: string;
    danger: boolean;
    needsForm: boolean;
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

export function menuItemsFor(
    actionsFor: (target: EditTarget, insertion?: number) => EditorAction[],
    targets: readonly EditTarget[],
    insertion?: number,
): ActionMenuItem[] {
    return targets
        .flatMap((target) => actionsFor(target, insertion))
        .map((action) => ({
            action,
            label: actionLabel(action),
            danger: OPERATIONS[action.operation].danger ?? false,
            needsForm: action.form.length > 0,
        }));
}
