import {useEffect} from "react";

export type MenuItem = {
    label: string
    enabled?: boolean
    danger?: boolean
    onClick: () => void
}

interface MenuProps {
    header: string,
    x: number,
    y: number,
    onClose: () => void,
    items: MenuItem[],
}

export function Menu({header,x,y,onClose,items}: MenuProps) {

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
        window.addEventListener('mousedown', onClose);
        window.addEventListener('keydown', onKey);
        return () => {
            window.removeEventListener('mousedown', onClose);
            window.removeEventListener('keydown', onKey);
        };
    }, [onClose]);


    return (
        <div className="menu" style={{ left: x, top: y }} onMouseDown={(e) => e.stopPropagation()}>
            <header>{header}</header>
            {items.map((item, index) => (
                <button
                    key={`${item.label}-${index}`}
                    type="button"
                    className={item.danger ? 'danger' : undefined}
                    disabled={item.enabled === false}
                    onClick={() => {
                        item.onClick?.();
                        onClose();
                    }}
                >
                    {item.label}
                </button>
            ))}
        </div>
    )
}
