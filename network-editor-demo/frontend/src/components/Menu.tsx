import {useEffect} from "react";
import {type ActionMenuItem, describeTargets} from "@powsybl/network-editor";
import type {EditorMenu} from "../useNetworkEditor.ts";

interface MenuProps {
    menu: EditorMenu
    onPick: (item: ActionMenuItem) => void
    onClose: () => void
}

export function Menu({menu, onPick, onClose}: MenuProps) {

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
        <div className="menu" style={{left: menu.x, top: menu.y}} onMouseDown={(e) => e.stopPropagation()}>
            <header>{describeTargets(menu.targets)}</header>
            {menu.items.map((item) => (
                <button
                    key={item.action.id}
                    type="button"
                    className={item.danger ? 'danger' : undefined}
                    onClick={() => onPick(item)}
                >
                    {item.label}
                </button>
            ))}
        </div>
    )
}
