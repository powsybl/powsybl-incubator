import { useEffect, useRef, useState } from 'react';
import {
    actionLabel,
    NetworkEditor,
    type ActionMenuItem,
    type EditorAction,
    type EquipmentProperties,
    type NetworkChange,
    type SLDMetadata,
} from '../src';
import { ContextMenu } from './ContextMenu';
import { PropertyForm } from './PropertyForm';
import svgContent from './data/reseau.svg?raw';
import metadata from './data/reseau_metadata.json';
import properties from './data/reseau_properties.json';

type Menu = { items: ActionMenuItem[]; x: number; y: number };

export function App() {
    const container = useRef<HTMLDivElement>(null);
    const [editor, setEditor] = useState<NetworkEditor | null>(null);
    const [menu, setMenu] = useState<Menu | null>(null);
    const [form, setForm] = useState<EditorAction | null>(null);
    const [changes, setChanges] = useState<NetworkChange[]>([]);
    const [showIidmNodes, setShowIidmNodes] = useState(false);
    const [history, setHistory] = useState<{ canUndo: boolean; canRedo: boolean }>({ canUndo: false, canRedo: false });

    useEffect(() => {
        const instance = new NetworkEditor({
            container: container.current!,
            svgContent,
            metadata: metadata as unknown as SLDMetadata,
            initialProperties: properties as Record<string, EquipmentProperties>,
            onEvent: (event) => {
                switch (event.name) {
                    case 'menu:requested':
                        setMenu({ items: event.items, ...event.position });
                        break;
                    case 'gesture:changed':
                        setForm(event.action);
                        break;
                    case 'changes:changed':
                        setChanges(event.changes);
                        break;
                    case 'history:changed' :
                        setHistory({ canUndo: event.canUndo, canRedo: event.canRedo });
                        break;
                }
            },
        });
        setEditor(instance);
        return () => instance.destroy();
    }, []);

    const itemClicked = (item: ActionMenuItem) => {
        setMenu(null);
        if (item.needsForm) setForm(item.action);
        else item.action.run();
    };

    const cancel = () => {
        setForm(null);
        editor?.cancelGesture();
    };

    const clearChanges = () => editor?.clearChanges();

    const toggleIidmNodes = () => {
        setShowIidmNodes(!showIidmNodes);
        editor?.showIidmNodes(!showIidmNodes);
    };

    return (
        <>
            <h1>network-editor — demo</h1>
            <p className="hint">Click or right-click the diagram to edit it. Esc cancels a gesture.</p>

            <div className="columns">
                <div className="diagram" ref={container} />

                <aside className="panel">
                    <div className="toolbar">
                        <button disabled={!history.canUndo} onClick={() => editor?.undo()}>↩ Undo</button>
                        <button disabled={!history.canRedo} onClick={() => editor?.redo()}>↪ Redo</button>
                        <button onClick={() => setChanges(editor?.getChanges() ?? [])}>send to backend</button>
                        <button onClick={() => clearChanges()}>clear</button>
                        <button onClick={toggleIidmNodes}>
                            {showIidmNodes ? 'hide IIDM nodes' : 'show IIDM nodes'}
                        </button>
                    </div>

                    {form && (
                        <>
                            <h2>{actionLabel(form)}</h2>
                            <PropertyForm action={form} onDone={() => setForm(null)} />
                            <button onClick={cancel}>Cancel</button>
                        </>
                    )}

                    <h2>Changes</h2>
                    <pre className="modifications">{JSON.stringify(changes, null, 2)}</pre>
                </aside>
            </div>

            {menu && (
                <div className="backdrop" onMouseDown={() => setMenu(null)}>
                    <ContextMenu items={menu.items} x={menu.x} y={menu.y} onPick={itemClicked} />
                </div>
            )}
        </>
    );
}
