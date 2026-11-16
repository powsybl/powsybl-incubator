import type { EditorAction } from './actions.ts';
import { OPERATIONS } from './operations.ts';
import type { EditOperation, EditTarget } from '../types.ts';

export interface ActionMenuItem {
    action: EditorAction;
    label: string;
    danger: boolean;
    needsForm: boolean;
}

export interface ActionMenuGroup {
    operation: EditOperation;
    label: string;
    danger: boolean;
    entries: ActionMenuEntry[];
}

export interface ActionMenuEntry {
    item: ActionMenuItem;
    label: string;
}

export function operationLabel(operation: EditOperation): string {
    return OPERATIONS[operation].label;
}

export function actionLabel(action: EditorAction): string {
    const base = operationLabel(action.operation);

    switch (action.subject?.kind) {
        case 'TYPE':
            return `${base}: ${action.subject.type}`;
        case 'BUSBAR':
            return `${base}: ${action.subject.busbarSectionId}`;
        case 'SELECTION':
            return `${base} (${action.subject.size})`;
        case 'STATE' :
            return action.subject.open ? 'close' : 'open';
        default:
            return base;
    }
}

function describeTarget(target: EditTarget): string {
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

export function groupMenuItems(items: readonly ActionMenuItem[]): ActionMenuGroup[] {
    const groups = new Map<EditOperation, ActionMenuItem[]>();
    for (const item of items) {
        const group = groups.get(item.action.operation);
        if (group) group.push(item);
        else groups.set(item.action.operation, [item]);
    }

    return [...groups].map(([operation, grouped]) => ({
        operation,
        label: operationLabel(operation),
        danger: grouped[0].danger,
        entries: grouped.map((item) => ({
            item,
            label: grouped.length === 1 ? item.label : entryLabel(item),
        })),
    })).sort((a, b) => Number(a.danger) - Number(b.danger));
}

function entryLabel(item: ActionMenuItem): string {
    const { subject, equipment } = item.action;
    if (subject?.kind === 'TYPE') return subject.type;
    if (subject?.kind === 'BUSBAR') return subject.busbarSectionId;
    return equipment?.id ?? item.label;
}
