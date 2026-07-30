import { useEffect } from 'react';

export interface MenuItemSpec {
    label: string;
    enabled?: boolean;
    tone?: 'default' | 'danger';
    onClick: () => void;
}

interface ContextMenuProps {
    header: string;
    items: MenuItemSpec[];
    x: number;
    y: number;
    onClose: () => void;
}

function MenuItem({ label, enabled = true, tone = 'default', onClick }: MenuItemSpec) {
    const color = enabled ? (tone === 'danger' ? '#dc3545' : '#212529') : '#aaa';
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
                color,
            }}
        >
            {label}
        </button>
    );
}

/** Positioned menu, shared by the equipment actions and the creation picker. */
export function ContextMenu({ header, items, x, y, onClose }: ContextMenuProps) {
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
                {header}
            </div>
            {items.map((item) => (
                <MenuItem key={item.label} {...item} />
            ))}
        </div>
    );
}
