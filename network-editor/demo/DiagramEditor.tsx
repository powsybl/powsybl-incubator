import {useEffect, useRef, useState} from 'react';
import {
    NetworkEditor,
    type ChangeSet,
    type ConnectionTarget,
    type EditorEvents,
    type EquipmentInfo,
    type EquipmentProperties,
    type SLDMetadata,
} from '../src';
import {ContextMenu} from './ContextMenu';
import {PropertyPanel} from './PropertyPanel';
import {PROPERTY_SCHEMAS, type PropertyDescriptor} from './properties/properties.ts';

interface MenuState {
    info: EquipmentInfo;
    x: number;
    y: number;
}

/**
 * The picked target, spelled as the pypowsybl call it feeds. `position_order`
 * is left to the backend: it reads the neighbours' orders with
 * `get_connectables_order_positions(network, voltage_level_id)`.
 */
function toPypowsyblCall(target: ConnectionTarget): string {
    const between = [target.previousEquipmentId, target.nextEquipmentId]
        .map((id) => id ?? 'end of busbar')
        .join(' / ');
    return `pp.network.create_line_bays(
    network, id='NEW_LINE', r=0.1, x=10, b1=0, g1=0, b2=0, g2=0,
    bus_or_busbar_section_id_1='${target.busbarSectionId}',
    direction_1='${target.direction}',
    position_order_1=...,  # between ${between}
    bus_or_busbar_section_id_2='<other voltage level>',
    direction_2='TOP', position_order_2=...,
)`;
}

const pypowsyblStyle = {
    background: '#f6f8fa',
    border: '1px solid #ddd',
    borderRadius: 4,
    fontSize: 11,
    padding: 8,
    overflowX: 'auto',
} as const;

interface Selection {
    id: string;
    schema: PropertyDescriptor[];
    values: EquipmentProperties;
}

interface DiagramEditorProps {
    title: string;
    svgUrl: string;
    metadata: SLDMetadata;
    initialProperties?: Record<string, EquipmentProperties>;
}

export function DiagramEditor({title, svgUrl, metadata, initialProperties}: DiagramEditorProps) {

    const containerRef = useRef<HTMLDivElement | null>(null);
    const editorRef = useRef<NetworkEditor | null>(null);
    const [svgContent, setSvgContent] = useState<string | null>(null);
    const [history, setHistory] = useState({canUndo: false, canRedo: false});
    const [pendingChanges, setPendingChanges] = useState<ChangeSet>([]);
    const [menu, setMenu] = useState<MenuState | null>(null);
    const [selection, setSelection] = useState<Selection | null>(null);
    const [connectionPoint, setConnectionPoint] =
        useState<EditorEvents['connection-point:picked'] | null>(null);

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
            onEvent: (name, payload) => {
                if (name === 'history:changed') {
                    setHistory(payload as EditorEvents['history:changed']);
                }
                if (name === 'model:changed') {
                    setPendingChanges((payload as EditorEvents['model:changed']).changeSet);
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
                if (name === 'connection-point:picked') {
                    setConnectionPoint(payload as EditorEvents['connection-point:picked']);
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
    const handleDelete = () => {
        if (menu) editorRef.current?.deleteElement(menu.info.equipmentId);
        closeMenu();
    };
    const handleDeleteBay = () => {
        if (menu) editorRef.current?.deleteFeederBay(menu.info.equipmentId);
        closeMenu();
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
                    <h3>Connection point</h3>
                    {connectionPoint === null ? (
                        <p style={{color: '#888'}}>Click on or near a busbar.</p>
                    ) : (
                        <>
                            <ul style={{paddingLeft: 20, fontSize: 13}}>
                                <li>
                                    <strong>busbarSectionId</strong>:{' '}
                                    {connectionPoint.busbarSectionId}
                                </li>
                                <li>
                                    <strong>voltageLevelId</strong>:{' '}
                                    {connectionPoint.voltageLevelId}
                                </li>
                                <li><strong>direction</strong>: {connectionPoint.direction}</li>
                                <li>
                                    <strong>between</strong>:{' '}
                                    {connectionPoint.previousEquipmentId ?? '—'} /{' '}
                                    {connectionPoint.nextEquipmentId ?? '—'}
                                </li>
                            </ul>
                            <p style={{fontSize: 12, color: '#666', marginBottom: 4}}>
                                What a backend would call:
                            </p>
                            <pre style={pypowsyblStyle}>{toPypowsyblCall(connectionPoint)}</pre>
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
                    info={menu.info}
                    x={menu.x}
                    y={menu.y}
                    onDelete={handleDelete}
                    onDeleteBay={handleDeleteBay}
                    onClose={closeMenu}
                />
            )}
        </div>
    );
}
