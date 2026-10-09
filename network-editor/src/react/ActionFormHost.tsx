import type { ComponentType } from 'react';
import { actionKey, type EditorAction } from '../core/actions';
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

/** Same key as actionKey, kept for existing registries. */
export const actionFormKey = actionKey;

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
