import { useEffect, useRef, useState } from 'react';
import {
    actionLabel,
    describeTargets,
    menuItemsFor,
    NetworkEditor,
    type BayInsertion,
    type ChangeSet,
    type EditorAction,
    type EditTarget,
    type EquipmentProperties,
    type Gesture,
    type SelectedElement,
    type SLDMetadata,
} from '../src';
import { ContextMenu, type MenuItemSpec } from './ContextMenu';
import { PropertyForm } from './PropertyForm';
import svgContent from './data/v1.svg?raw';
import metadataJson from './data/vl1_metadata.json';
import propertiesJson from './data/vl1_properties.json';

const metadata = metadataJson as unknown as SLDMetadata;
const initialProperties = propertiesJson as Record<string, EquipmentProperties>;

type Menu = { targets: readonly EditTarget[]; x: number; y: number; insertion?: BayInsertion };

export function App() {
    const container = useRef<HTMLDivElement>(null);
    const [editor, setEditor] = useState<NetworkEditor | null>(null);
    const [history, setHistory] = useState({ canUndo: false, canRedo: false });
    const [changes, setChanges] = useState<ChangeSet>([]);
    const [menu, setMenu] = useState<Menu | null>(null);
    const [panel, setPanel] = useState<EditorAction | null>(null);
    const [overlay, setOverlay] = useState(false);
    const [gesture, setGesture] = useState<Gesture | null>(null);
    const [selected, setSelected] = useState<readonly SelectedElement[]>([]);

    useEffect(() => {
        const instance = new NetworkEditor({
            container: container.current!,
            svgContent,
            metadata,
            initialProperties,
            onTargets: ({ targets, position, insertion }) =>
                setMenu({ targets, x: position.x, y: position.y, insertion }),

            onEvent: (event) => {
                if (event.name === 'history:changed') setHistory(event);
                if (event.name === 'model:changed') setChanges(event.changeSet);
                if (event.name === 'targets:changed') setMenu(null);
                if (event.name === 'gesture:changed') {
                    setGesture(event.gesture);
                    if (event.gesture) setMenu(null);
                }
                if (event.name === 'element:selected') {
                    setSelected(event.elements);
                    // One equipment: its properties. Several: nothing to show but the count.
                    setPanel(
                        event.elements.length === 1
                            ? propertiesAction(instance, event.elements[0].id)
                            : null,
                    );
                }
            },
        });
        setEditor(instance);
        return () => instance.destroy();
    }, []);

    const items: MenuItemSpec[] =
        editor && menu
            ? menuItemsFor(editor, menu).map((item) => ({
                  label: item.label,
                  enabled: item.enabled,
                  danger: item.danger,
                  onClick: () => (item.needsForm ? setPanel(item.action) : item.action.run()),
              }))
            : [];

    return (
        <>
            <h1>network-editor — demo</h1>
            <div className="toolbar">
                <button onClick={() => editor?.undo()} disabled={!history.canUndo}>
                    ↩ Undo
                </button>
                <button onClick={() => editor?.redo()} disabled={!history.canRedo}>
                    ↪ Redo
                </button>
                <button
                    onClick={() => {
                        setOverlay(!overlay);
                        editor?.showIidmNodes(!overlay);
                    }}
                >
                    ⬡ {overlay ? 'Hide' : 'Show'} IIDM nodes
                </button>
            </div>

            {selected.length > 1 && (
                <p className="hint">
                    {selected.length} elements selected — Shift+click to add or remove, right-click
                    to delete them all.
                </p>
            )}

            {gesture && (
                <p className="hint">
                    {gesture.kind === 'LINK'
                        ? `Pick the far end of the link — ${gesture.candidates.length} candidates.`
                        : `Pick the new position of ${gesture.equipmentId} — ${gesture.candidates.length} slots on the busbar.`}{' '}
                    Esc cancels.
                </p>
            )}

            <div className="columns">
                <div className="diagram" ref={container} />
                <div className="panel">
                    {panel ? (
                        <ActionPanel key={panel.id} action={panel} onDone={() => setPanel(null)} />
                    ) : (
                        <p className="hint">Click or right-click the diagram.</p>
                    )}

                    <h2>Change set ({changes.length})</h2>
                    <p className="hint">
                        The body a backend receives, verbatim — <code>POST /network/changes</code>
                    </p>
                    <pre className="changeset">{JSON.stringify({ changes }, null, 2)}</pre>
                </div>
            </div>

            {menu && items.length > 0 && (
                <ContextMenu
                    header={describeTargets(menu.targets)}
                    items={items}
                    x={menu.x}
                    y={menu.y}
                    onClose={() => setMenu(null)}
                />
            )}
        </>
    );
}

function ActionPanel({ action, onDone }: { action: EditorAction; onDone: () => void }) {
    const [refused, setRefused] = useState(false);

    return (
        <>
            <h2>{actionLabel(action)}</h2>
            <PropertyForm
                schema={action.form}
                initial={action.initial}
                submitLabel="Apply"
                onSubmit={(values) => {
                    const applied = action.run(values);
                    setRefused(!applied);
                    if (applied) onDone();
                }}
                rename={action.operation === 'RENAME'}
            />
            {refused && <p className="hint invalid">Refused by the component</p>}
            <button onClick={onDone}>Cancel</button>
        </>
    );
}

/** Selecting an equipment opens its properties — the action carries the values. */
function propertiesAction(editor: NetworkEditor, equipmentId: string | null): EditorAction | null {
    const target = editor.getTargets().find((candidate) => candidate.id === equipmentId);
    if (!target) return null;
    return editor.actionsFor(target).find((a) => a.operation === 'UPDATE_PROPERTIES') ?? null;
}
