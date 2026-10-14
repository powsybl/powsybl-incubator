import { useEffect, useRef, useState } from 'react';
import {
    actionLabel,
    NetworkEditor,
    type ActionMenuItem,
    type EditorAction,
    type EquipmentProperties,
    type NetworkModification,
    type SLDMetadata,
} from '../src';
import { PropertyForm } from '../src/react';
import svgContent from './data/reseau.svg?raw';
import metadata from './data/reseau_metadata.json';
import properties from './data/reseau_properties.json';

type Menu = { items: ActionMenuItem[]; x: number; y: number };

export function App() {
    const container = useRef<HTMLDivElement>(null);
    const [editor, setEditor] = useState<NetworkEditor | null>(null);
    const [menu, setMenu] = useState<Menu | null>(null);
    const [form, setForm] = useState<EditorAction | null>(null);
    const [modifications, setModifications] = useState<NetworkModification[]>([]);

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
                    case 'modifications:changed':
                        setModifications(event.modifications);
                        break;
                }
            },
        });
        setEditor(instance);
        return () => instance.destroy();
    }, []);

    const pick = (item: ActionMenuItem) => {
        setMenu(null);
        if (item.needsForm) setForm(item.action);
        else item.action.run();
    };

    const cancel = () => {
        setForm(null);
        editor?.cancelGesture();
    };

    return (
        <>
            <h1>network-editor — demo</h1>
            <p className="hint">Click or right-click the diagram to edit it. Esc cancels a gesture.</p>

            <div className="columns">
                <div className="diagram" ref={container} />

                <aside className="panel">
                    <div className="toolbar">
                        <button onClick={() => editor?.undo()}>↩ Undo</button>
                        <button onClick={() => editor?.redo()}>↪ Redo</button>
                    </div>

                    {form && (
                        <>
                            <h2>{actionLabel(form)}</h2>
                            <PropertyForm action={form} onDone={() => setForm(null)} />
                            <button onClick={cancel}>Cancel</button>
                        </>
                    )}

                    <h2>Modifications ({modifications.length})</h2>
                    <pre className="modifications">{JSON.stringify(modifications, null, 2)}</pre>
                </aside>
            </div>

            {menu && (
                <div className="backdrop" onMouseDown={() => setMenu(null)}>
                    <div
                        className="menu"
                        style={{ left: menu.x, top: menu.y }}
                        onMouseDown={(event) => event.stopPropagation()}
                    >
                        {menu.items.map((item) => (
                            <button
                                key={item.action.id}
                                className={item.danger ? 'danger' : undefined}
                                onClick={() => pick(item)}
                            >
                                {item.label}
                            </button>
                        ))}
                    </div>
                </div>
            )}
        </>
    );
}
