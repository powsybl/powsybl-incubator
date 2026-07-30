import {useEffect, useRef, useState} from 'react';
import {
    CREATABLE_TYPES,
    NetworkEditor,
    type ChangeSet,
    type ConnectionPoint,
    type EditorEvents,
    type ElementType,
    type EquipmentInfo,
    type EquipmentProperties,
    type SLDMetadata,
} from '../src';
import {ContextMenu, type MenuItemSpec} from './ContextMenu';
import {PropertyPanel} from './PropertyPanel';
import {PROPERTY_SCHEMAS, type PropertyDescriptor} from './properties/properties.ts';

interface MenuState {
    info: EquipmentInfo;
    x: number;
    y: number;
}

interface PickerState {
    point: ConnectionPoint;
    x: number;
    y: number;
}

/** An equipment being filled in, not yet recorded in the change set. */
interface Draft {
    point: ConnectionPoint;
    type: ElementType;
}

interface Selection {
    id: string;
    schema: PropertyDescriptor[];
    values: EquipmentProperties;
}

interface DiagramEditorProps {
    title: string;
    svgUrl: string;
    metadata: SLDMetadata;
    /** Real values, typically fetched from the backend. */
    initialProperties?: Record<string, EquipmentProperties>;
}

export function DiagramEditor({title, svgUrl, metadata, initialProperties}: DiagramEditorProps) {

    const containerRef = useRef<HTMLDivElement | null>(null);
    const editorRef = useRef<NetworkEditor | null>(null);
    const [svgContent, setSvgContent] = useState<string | null>(null);
    const [history, setHistory] = useState({canUndo: false, canRedo: false});
    const [pendingChanges, setPendingChanges] = useState<ChangeSet>([]);
    const [menu, setMenu] = useState<MenuState | null>(null);
    const [picker, setPicker] = useState<PickerState | null>(null);
    const [showIidmNodes, setShowIidmNodes] = useState(false);
    const [draft, setDraft] = useState<Draft | null>(null);
    const [selection, setSelection] = useState<Selection | null>(null);

    useEffect(() => {
        fetch(svgUrl)
            .then((res) => res.text())
            .then(setSvgContent)
            .catch((err) => console.error('SVG loading error:', err));
    }, [svgUrl]);

    useEffect(() => {
        if (!containerRef.current || !svgContent || editorRef.current) return;

        const editor = new NetworkEditor({
            container: containerRef.current,
            svgContent,
            metadata,
            initialProperties,
            onEquipmentContextMenu: ({info, position}) =>
                setMenu({info, x: position.x, y: position.y}),
            onConnectionPointClick: ({point, position}) =>
                setPicker({point, x: position.x, y: position.y}),
            onEvent: (name, payload) => {
                if (name === 'history:changed') {
                    setHistory(payload as EditorEvents['history:changed']);
                }
                if (name === 'model:changed') {
                    setPendingChanges((payload as EditorEvents['model:changed']).changeSet);
                }
                if (name === 'connection:changed') {
                    const {points} = payload as EditorEvents['connection:changed'];
                    const alive = new Set(points.map((point) => point.id));
                    setDraft((prev) => (prev && alive.has(prev.point.id) ? prev : null));
                    setPicker((prev) => (prev && alive.has(prev.point.id) ? prev : null));
                }
                if (name === 'element:selected') {
                    // The editor knows nothing about schemas: the app picks one.
                    const {id, type} = payload as EditorEvents['element:selected'];
                    setSelection(
                        id === null || type === null
                            ? null
                            : {
                                  id,
                                  schema: PROPERTY_SCHEMAS[type] ?? [],
                                  values: editor.getProperties(id),
                              },
                    );
                }
                if (name === 'properties:changed') {
                    // Refresh the panel after an apply/undo/redo.
                    const {id} = payload as EditorEvents['properties:changed'];
                    setSelection((prev) =>
                        prev && prev.id === id
                            ? {...prev, values: editor.getProperties(id)}
                            : prev,
                    );
                }
            },
        });
        editorRef.current = editor;

        return () => {
            editor.destroy();
            editorRef.current = null;
        };
    }, [svgContent, metadata, initialProperties]);

    const closeMenu = () => setMenu(null);
    const closePicker = () => setPicker(null);

    const handleDelete = () => {
        if (menu) editorRef.current?.deleteElement(menu.info.equipmentId);
        closeMenu();
    };
    const handleDeleteBay = () => {
        if (menu) editorRef.current?.deleteFeederBay(menu.info.equipmentId);
        closeMenu();
    };

    const equipmentItems: MenuItemSpec[] = menu
        ? [
              {label: 'Delete', enabled: menu.info.deletable, tone: 'danger', onClick: handleDelete},
              {
                  label: 'Delete feeder bay',
                  enabled: menu.info.bayDeletable,
                  tone: 'danger',
                  onClick: handleDeleteBay,
              },
          ]
        : [];

    const createItems: MenuItemSpec[] = picker
        ? [...CREATABLE_TYPES].map((type) => ({
              label: `Add ${type}`,
              onClick: () => {
                  setDraft({point: picker.point, type});
                  closePicker();
              },
          }))
        : [];

    const submitDraft = (properties: EquipmentProperties) => {
        if (!draft) return;
        editorRef.current?.createEquipment(draft.point.id, {type: draft.type, properties});
        setDraft(null);
    };

    return (
        <div style={{padding: 20, fontFamily: 'sans-serif'}}>
            <h1>{title}</h1>
            <div style={{marginBottom: 10}}>
                <button
                    onClick={() => editorRef.current?.undo()}
                    disabled={!history.canUndo}
                    style={{padding: '8px 16px'}}
                >
                    ↩ Undo
                </button>
                <button
                    onClick={() => editorRef.current?.redo()}
                    disabled={!history.canRedo}
                    style={{marginLeft: 10, padding: '8px 16px'}}
                >
                    ↪ Redo
                </button>
                <button
                    onClick={() => {
                        const next = !showIidmNodes;
                        setShowIidmNodes(next);
                        editorRef.current?.showIidmNodes(next);
                    }}
                    style={{marginLeft: 10, padding: '8px 16px'}}
                >
                    {showIidmNodes ? '⬡ Hide IIDM nodes' : '⬡ Show IIDM nodes'}
                </button>
            </div>
            {!svgContent && <p>Loading diagram...</p>}
            <div style={{display: 'flex', gap: 16, alignItems: 'flex-start'}}>
                <div
                    ref={containerRef}
                    style={{
                        flex: 2,
                        height: 800,
                        border: '1px solid #ccc',
                        backgroundColor: '#f8f9fa',
                    }}
                />
                <div style={{
                    flex: 1,
                    height: 800,
                    border: '1px solid #ccc',
                    padding: '0 16px',
                    overflowY: 'auto',
                    backgroundColor: '#fff'
                }}>
                    {draft ? (
                        <>
                            <h3>New {draft.type}</h3>
                            <p style={{color: '#666', fontSize: 12}}>
                                on {draft.point.anchor.attachedTo}
                                {draft.point.anchor.node !== undefined
                                    ? ` — IIDM node ${draft.point.anchor.node}`
                                    : ''}
                            </p>
                            <PropertyPanel
                                equipmentId={draft.point.id}
                                schema={(PROPERTY_SCHEMAS[draft.type] ?? []).filter(
                                    (descriptor) => !descriptor.editOnly,
                                )}
                                values={{}}
                                mode="create"
                                onApply={submitDraft}
                            />
                            <button
                                onClick={() => setDraft(null)}
                                style={{marginTop: 8, padding: '8px 16px'}}
                            >
                                Cancel
                            </button>
                        </>
                    ) : (
                        <>
                            <h3>Properties</h3>
                            <PropertyPanel
                                equipmentId={selection?.id ?? null}
                                schema={selection?.schema ?? []}
                                values={selection?.values ?? {}}
                                onApply={(changes) =>
                                    selection &&
                                    editorRef.current?.applyProperties(selection.id, changes)
                                }
                            />
                        </>
                    )}
                    <h3>Pending changes</h3>
                    {pendingChanges.length === 0 ? (
                        <p>No change</p>
                    ) : (
                        <ul style={{paddingLeft: 20}}>
                            {pendingChanges.map((change, index) => (
                                <li key={index} style={{marginBottom: 8}}>
                                    <strong>{change.op}</strong>: {change.equipmentType} ({change.equipmentId})
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </div>
            {menu && (
                <ContextMenu
                    header={`${menu.info.type} — ${menu.info.label}`}
                    items={equipmentItems}
                    x={menu.x}
                    y={menu.y}
                    onClose={closeMenu}
                />
            )}
            {picker && (
                <ContextMenu
                    header="Free connection point"
                    items={createItems}
                    x={picker.x}
                    y={picker.y}
                    onClose={closePicker}
                />
            )}
        </div>
    );
}
