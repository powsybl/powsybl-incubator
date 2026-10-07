import type { ComponentType } from 'react';
import type { EditorAction } from '../core/actions';
import { PropertyForm } from './PropertyForm';

/** Props a host form receives in place of the built-in PropertyForm. */
export interface ActionFormProps {
    action: EditorAction;
    onDone: () => void;
    onCancel: () => void;
}

export type ActionFormComponent = ComponentType<ActionFormProps>;

/** Picks the form of an action; undefined keeps the built-in PropertyForm. */
export type ActionFormRegistry = (action: EditorAction) => ActionFormComponent | undefined;

/** `OPERATION:TYPE` for a creation (e.g. `CREATE_FEEDER_BAY:LOAD`), the operation otherwise. */
export function actionFormKey(action: EditorAction): string {
    return action.subject?.kind === 'TYPE'
        ? `${action.operation}:${action.subject.type}`
        : action.operation;
}

export function registryOf(forms: Record<string, ActionFormComponent>): ActionFormRegistry {
    return (action) => forms[actionFormKey(action)];
}

export interface ActionFormHostProps extends ActionFormProps {
    registry?: ActionFormRegistry;
}

/**
 * Renders the host form registered for the action, or PropertyForm.
 * An injected form does its own submission and never calls action.run.
 */
export function ActionFormHost({ action, registry, onDone, onCancel }: ActionFormHostProps) {
    const Injected = registry?.(action);
    if (Injected) return <Injected action={action} onDone={onDone} onCancel={onCancel} />;
    return <PropertyForm action={action} onDone={onDone} />;
}
