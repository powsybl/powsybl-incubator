import {type RefObject, useCallback, useEffect, useState} from "react";
import {
    type ActionMenuItem,
    type EditorAction,
    type EditTarget,
    NetworkEditor,
    type NetworkModification,
} from "@powsybl/network-editor";
import type {Sld} from "./types.ts";

export interface EditorMenu {
    items: ActionMenuItem[]
    targets: readonly EditTarget[]
    x: number
    y: number
}

export function useNetworkEditor(container: RefObject<HTMLDivElement | null>, sld: Sld | null) {
    const [editor, setEditor] = useState<NetworkEditor | null>(null)
    const [menu, setMenu] = useState<EditorMenu | null>(null)
    const [form, setForm] = useState<EditorAction | null>(null)
    const [modifications, setModifications] = useState<NetworkModification[]>([])
    const [history, setHistory] = useState({canUndo: false, canRedo: false})

    useEffect(() => {
        setEditor(null)
        setMenu(null)
        setForm(null)
        setModifications([])
        setHistory({canUndo: false, canRedo: false})
        if (!sld || !container.current) return

        const instance = new NetworkEditor({
            container: container.current,
            svgContent: sld.svg,
            metadata: sld.metadata,
            initialProperties: sld.properties,
            onEvent: (event) => {
                switch (event.name) {
                    case "menu:requested":
                        setMenu({items: event.items, targets: event.targets, ...event.position})
                        break
                    case "gesture:changed":
                        setMenu(null)
                        setForm(event.action)
                        break
                    case "modifications:changed":
                        setModifications(event.modifications)
                        break
                    case "history:changed":
                        setHistory({canUndo: event.canUndo, canRedo: event.canRedo})
                        break
                }
            },
        })
        setEditor(instance)
        return () => instance.destroy()
    }, [container, sld])

    const closeMenu = useCallback(() => setMenu(null), [])

    const pick = (item: ActionMenuItem) => {
        setMenu(null)
        if (item.needsForm) setForm(item.action)
        else item.action.run()
    }

    const closeForm = () => {
        setForm(null)
        editor?.cancelGesture()
    }

    return {editor, menu, closeMenu, pick, form, closeForm, modifications, history}
}
