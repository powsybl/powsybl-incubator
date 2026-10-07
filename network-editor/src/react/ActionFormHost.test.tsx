import { act } from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { EditorAction } from '../core/actions';
import { ActionFormHost, actionFormKey, registryOf, type ActionFormComponent } from './ActionFormHost';

(globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true;

function loadAction(run: EditorAction['run']): EditorAction {
    return {
        id: 'VL1_BBS1:CREATE_FEEDER_BAY:LOAD',
        operation: 'CREATE_FEEDER_BAY',
        subject: { kind: 'TYPE', type: 'LOAD' },
        form: [{ key: 'equipmentId', type: 'string', required: true }],
        initial: { equipmentId: 'NEW_LOAD' },
        run,
    };
}

const Injected: ActionFormComponent = ({ action, onDone }) => (
    <button data-testid="injected" onClick={onDone}>
        {action.id}
    </button>
);

let container: HTMLDivElement;
let root: Root;

beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
    root = createRoot(container);
});

afterEach(() => {
    act(() => root.unmount());
    container.remove();
});

describe('ActionFormHost', () => {
    it('keys a creation by operation and type', () => {
        expect(actionFormKey(loadAction(() => true))).toBe('CREATE_FEEDER_BAY:LOAD');
    });

    it('falls back to PropertyForm without a registry', () => {
        act(() =>
            root.render(
                <ActionFormHost action={loadAction(() => true)} onDone={() => {}} onCancel={() => {}} />,
            ),
        );

        expect(container.querySelector('form')).not.toBeNull();
        expect(container.querySelector('[data-testid="injected"]')).toBeNull();
    });

    it('renders the registered form and never runs the action', () => {
        const run = vi.fn(() => true);
        const onDone = vi.fn();
        const registry = registryOf({ 'CREATE_FEEDER_BAY:LOAD': Injected });

        act(() =>
            root.render(
                <ActionFormHost
                    action={loadAction(run)}
                    registry={registry}
                    onDone={onDone}
                    onCancel={() => {}}
                />,
            ),
        );
        const button = container.querySelector<HTMLButtonElement>('[data-testid="injected"]');
        act(() => button?.click());

        expect(container.querySelector('form')).toBeNull();
        expect(onDone).toHaveBeenCalledOnce();
        expect(run).not.toHaveBeenCalled();
    });
});
