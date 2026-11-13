import {Fragment, useEffect, useState} from "react";
import {
    type ActionMenuGroup,
    type ActionMenuItem,
    describeTargets,
    type EditOperation,
    groupMenuItems,
} from "@powsybl/network-editor";
import type {EditorMenu} from "../useNetworkEditor.ts";

interface MenuProps {
    menu: EditorMenu
    onPick: (item: ActionMenuItem) => void
    onClose: () => void
}

export function Menu({menu, onPick, onClose}: MenuProps) {
    const [open, setOpen] = useState<EditOperation | null>(null)
    const groups = groupMenuItems(menu.items)
    const firstDanger = groups.findIndex((group) => group.danger)

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
        window.addEventListener('mousedown', onClose);
        window.addEventListener('keydown', onKey);
        return () => {
            window.removeEventListener('mousedown', onClose);
            window.removeEventListener('keydown', onKey);
        };
    }, [onClose]);

    const row = (group: ActionMenuGroup) => {
        const className = group.danger ? 'danger' : undefined

        if (group.entries.length === 1) {
            const [{item, label}] = group.entries
            return (
                <button type="button" className={className} onMouseEnter={() => setOpen(null)} onClick={() => onPick(item)}>
                    {label}
                </button>
            )
        }

        const isOpen = open === group.operation
        return (
            <div className="menu-group">
                <button
                    type="button"
                    className={className}
                    onMouseEnter={() => setOpen(group.operation)}
                    onClick={() => setOpen(isOpen ? null : group.operation)}
                >
                    {group.label}
                    <span className="chevron">▸</span>
                </button>
                {isOpen && (
                    <div className="submenu">
                        {group.entries.map(({item, label}) => (
                            <button key={item.action.id} type="button" className={className} onClick={() => onPick(item)}>
                                {label}
                            </button>
                        ))}
                    </div>
                )}
            </div>
        )
    }

    return (
        <div className="menu" style={{left: menu.x, top: menu.y}} onMouseDown={(e) => e.stopPropagation()}>
            <header>{describeTargets(menu.targets)}</header>
            {groups.map((group, index) => (
                <Fragment key={group.operation}>
                    {index > 0 && index === firstDanger && <hr className="separator"/>}
                    {row(group)}
                </Fragment>
            ))}
        </div>
    )
}
