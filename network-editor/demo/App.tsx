import { useEffect, useRef, useState } from 'react';
import {
    NetworkEditor,
    type BayInsertion,
    type ChangeSet,
    type EditOperation,
    type EditorAction,
    type EditTarget,
    type EquipmentProperties,
    type SLDMetadata,
} from '../src';
import { ContextMenu, type MenuItemSpec } from './ContextMenu';
import { PropertyForm } from './PropertyForm';
import svgContent from './data/v1.svg?raw';
import metadataJson from './data/vl1_metadata.json';
import propertiesJson from './data/vl1_properties.json';

const metadata = metadataJson as unknown as SLDMetadata;
const initialProperties = propertiesJson as Record<string, EquipmentProperties>;

const LABELS: Record<EditOperation, string> = {
    CREATE_INJECTION: 'Add injection',
    CREATE_SWITCH: 'Put a switch back',
    CREATE_FEEDER_BAY: 'Create a feeder bay',
    CREATE_COUPLING: 'Create a coupling',
    DELETE: 'Delete',
    DELETE_BAY: 'Delete feeder bay',
    UPDATE_PROPERTIES: 'Properties',
    UPDATE_BAY_POSITION: 'Change bay position',
    MOVE_BAY: 'Move feeder bay',
    RENAME: 'Rename',
};

const DANGEROUS: ReadonlySet<EditOperation> = new Set<EditOperation>(['DELETE', 'DELETE_BAY']);

/** The component hands over data; putting it into words is the host's job. */
function label(action: EditorAction): string {
    const base = LABELS[action.operation];
    if (!action.enabled) return `${base} — not implemented`;

    switch (action.subject?.kind) {
        case 'TYPE':
            return `${base}: ${action.subject.type}`;
        case 'BUSBAR':
            return `${base}: ${action.subject.busbarSectionId}`;
        default:
            return base;
    }
}

function describe(target: EditTarget): string {
    switch (target.kind) {
        case 'NODE':
            return `Free node ${target.node}`;
        case 'GAP':
            return `Gap ${target.node1}–${target.node2}`;
        case 'BUSBAR':
            return `Busbar ${target.busbarSectionId}`;
        case 'EQUIPMENT':
            return `${target.type} ${target.equipmentId}`;
    }
}

type Menu = { targets: readonly EditTarget[]; x: number; y: number; insertion?: BayInsertion };

export function App() {
    const container = useRef<HTMLDivElement>(null);
    const [editor, setEditor] = useState<NetworkEditor | null>(null);
    const [history, setHistory] = useState({ canUndo: false, canRedo: false });
    const [changes, setChanges] = useState<ChangeSet>([]);
    const [menu, setMenu] = useState<Menu | null>(null);
    const [panel, setPanel] = useState<EditorAction | null>(null);
    const [overlay, setOverlay] = useState(false);

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
                if (event.name === 'element:selected') {
                    setPanel(propertiesAction(instance, event.id));
                }
            },
        });
        setEditor(instance);
        return () => instance.destroy();
    }, []);

    // The whole menu: the component says what is possible and what it needs.
    const items: MenuItemSpec[] =
        menu?.targets
            .flatMap((target) => editor?.actionsFor(target, menu.insertion) ?? [])
            .map((action) => ({
                label: label(action),
                enabled: action.enabled,
                danger: DANGEROUS.has(action.operation),
                onClick: () => (action.form.length > 0 ? setPanel(action) : action.run()),
            })) ?? [];

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
                    header={menu.targets.map(describe).join(' · ')}
                    items={items}
                    x={menu.x}
                    y={menu.y}
                    onClose={() => setMenu(null)}
                />
            )}
        </>
    );
}

/** One panel for every operation: the action says what to ask and what to run. */
function ActionPanel({ action, onDone }: { action: EditorAction; onDone: () => void }) {
    const [refused, setRefused] = useState(false);

    return (
        <>
            <h2>{label(action)}</h2>
            <PropertyForm
                schema={action.form}
                initial={action.initial}
                submitLabel="Apply"
                onSubmit={(values) => {
                    const applied = action.run(values);
                    setRefused(!applied);
                    if (applied) onDone();
                }}
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
