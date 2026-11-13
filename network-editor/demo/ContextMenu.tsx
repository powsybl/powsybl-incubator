import { Fragment, useState } from 'react';
import { groupMenuItems, type ActionMenuGroup, type ActionMenuItem, type EditOperation } from '../src';

interface ContextMenuProps {
    items: ActionMenuItem[];
    x: number;
    y: number;
    onPick: (item: ActionMenuItem) => void;
}

/** One line per operation; an operation with several variants opens a submenu. */
export function ContextMenu({ items, x, y, onPick }: ContextMenuProps) {
    const [open, setOpen] = useState<EditOperation | null>(null);
    const groups = groupMenuItems(items);
    const firstDanger = groups.findIndex((group) => group.danger);

    const row = (group: ActionMenuGroup) => {
        const className = group.danger ? 'danger' : undefined;

        if (group.entries.length === 1) {
            const [{ item, label }] = group.entries;
            return (
                <button className={className} onMouseEnter={() => setOpen(null)} onClick={() => onPick(item)}>
                    {label}
                </button>
            );
        }

        const isOpen = open === group.operation;
        return (
            <div className="menu-group">
                <button
                    className={className}
                    onMouseEnter={() => setOpen(group.operation)}
                    onClick={() => setOpen(isOpen ? null : group.operation)}
                >
                    {group.label}
                    <span className="chevron">▸</span>
                </button>
                {isOpen && (
                    <div className="submenu">
                        {group.entries.map(({ item, label }) => (
                            <button key={item.action.id} className={className} onClick={() => onPick(item)}>
                                {label}
                            </button>
                        ))}
                    </div>
                )}
            </div>
        );
    };

    return (
        <div className="menu" style={{ left: x, top: y }} onMouseDown={(event) => event.stopPropagation()}>
            {groups.map((group, index) => (
                <Fragment key={group.operation}>
                    {index > 0 && index === firstDanger && <hr className="separator" />}
                    {row(group)}
                </Fragment>
            ))}
        </div>
    );
}
