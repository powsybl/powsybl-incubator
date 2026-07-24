import {useEffect, useRef, useState} from 'react';
import {
    NetworkEditor,
    type ChangeSet,
    type EditorEvents,
    type EquipmentInfo,
    type SLDMetadata,
} from '../src';
import {ContextMenu} from './ContextMenu';

interface MenuState {
    info: EquipmentInfo;
    x: number;
    y: number;
}


interface DiagramEditorProps {
    title: string;
    svgUrl: string;
    metadata: SLDMetadata;
}

export function DiagramEditor({title, svgUrl, metadata}: DiagramEditorProps) {

    const containerRef = useRef<HTMLDivElement | null>(null);
    const editorRef = useRef<NetworkEditor | null>(null);
    const [svgContent, setSvgContent] = useState<string | null>(null);
    const [history, setHistory] = useState({canUndo: false, canRedo: false});
    const [pendingChanges, setPendingChanges] = useState<ChangeSet>([]);
    const [menu, setMenu] = useState<MenuState | null>(null);

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
            onEquipmentContextMenu: ({info, position}) =>
                setMenu({info, x: position.x, y: position.y}),
            onEvent: (name, payload) => {
                if (name === 'history:changed') {
                    setHistory(payload as EditorEvents['history:changed']);
                }
                if (name === 'model:changed') {
                    setPendingChanges((payload as EditorEvents['model:changed']).changeSet);
                }
            },
        });
        editorRef.current = editor;

        return () => {
            editor.destroy();
            editorRef.current = null;
        };
    }, [svgContent, metadata]);

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
                    <h3>Modifications en attente</h3>
                    {pendingChanges.length === 0 ? (
                        <p>Aucune modification</p>
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
