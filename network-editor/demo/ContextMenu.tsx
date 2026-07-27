import { useEffect } from 'react';
import type { EquipmentInfo } from '../src';

interface ContextMenuProps {
    info: EquipmentInfo;
    x: number;
    y: number;
    onDelete: () => void;
    onDeleteBay: () => void;
    onClose: () => void;
}

function MenuItem({
    label,
    enabled,
    onClick,
}: {
    label: string;
    enabled: boolean;
    onClick: () => void;
}) {
    return (
        <button
            type="button"
            disabled={!enabled}
            onClick={onClick}
            style={{
                display: 'block',
                width: '100%',
                textAlign: 'left',
                padding: '8px 12px',
                border: 'none',
                background: 'none',
                cursor: enabled ? 'pointer' : 'not-allowed',
                color: enabled ? '#dc3545' : '#aaa',
            }}
        >
            {label}
        </button>
    );
}

export function ContextMenu({info, x, y, onDelete, onDeleteBay, onClose}: ContextMenuProps) {
    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('mousedown', onClose);
        window.addEventListener('keydown', onKey);
        return () => {
            window.removeEventListener('mousedown', onClose);
            window.removeEventListener('keydown', onKey);
        };
    }, [onClose]);

    return (
        <div
            onMouseDown={(e) => e.stopPropagation()}
            style={{
                position: 'fixed',
                left: x,
                top: y,
                zIndex: 1000,
                minWidth: 180,
                background: '#fff',
                border: '1px solid #ccc',
                borderRadius: 4,
                boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                fontFamily: 'sans-serif',
                fontSize: 14,
                overflow: 'hidden',
            }}
        >
            <div
                style={{
                    padding: '6px 12px',
                    borderBottom: '1px solid #eee',
                    color: '#666',
                    fontSize: 12,
                }}
            >
                {info.componentType} — {info.label}
            </div>
            <MenuItem label="Delete" enabled={info.deletable} onClick={onDelete} />
            <MenuItem
                label="Delete feeder bay"
                enabled={info.bayDeletable}
                onClick={onDeleteBay}
            />
        </div>
    );
}
