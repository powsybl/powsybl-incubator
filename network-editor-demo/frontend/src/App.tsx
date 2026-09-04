import {Banner, Button, Card, Header, Loader, Select} from "@design-system-rte/react";
import { useEffect, useRef, useState} from "react";
import type {NetworkInfo, Sld} from "./types.ts";
import {deleteBay, deleteElement, getSld, getVoltageLevelIds, loadNetwork, update} from "./api.ts";
import {describeTargets, menuItemsFor, NetworkEditor} from "@powsybl/network-editor";
import type {BayInsertion, EditTarget} from "@powsybl/network-editor";
import type {MenuItem} from "./components/Menu.tsx";
import {Menu} from "./components/Menu.tsx";
import {actionFor, actionLabel, type EditorAction} from "@powsybl/network-editor";
import {PropertyForm} from "@powsybl/network-editor/react";
import type {ChangeOp, ChangeSetEntry} from "@powsybl/network-editor";

const CHANGE_LABELS: Record<ChangeOp, string> = {
    'create': 'Création',
    'create-bay': 'Création de travée',
    'create-switch': 'Création d\'organe de coupure',
    'create-switched-injection': 'Création derrière un organe de coupure',
    'move-bay': 'Déplacement de travée',
    'update-position': 'Changement de position',
    'update': 'Modification',
    'delete': 'Suppression',
    'delete-bay': 'Suppression de travée',
    'rename': 'Renommage',
}

export default function App() {

    const container = useRef<HTMLDivElement>(null);

    const [error, setError] = useState<string>("")
    const [loading, setLoading] = useState<boolean>(false)
    const [info, setInfo] = useState<NetworkInfo | null>(null)
    const [vlIds, setVlIds] = useState<string[] | null>(null)
    const [selectedVlId, setSelectedVlId] = useState<string | null>(null)
    const [sld, setSld] = useState<Sld | null>(null)

    const [history, setHistory] = useState<{ canUndo: boolean; canRedo: boolean }>({canUndo: false, canRedo: false})
    const [changes, setChanges] = useState<ChangeSetEntry[]>([])

    const [editor, setEditor] = useState<NetworkEditor | null>(null);
    const [menu, setMenu] = useState<{
        targets: readonly EditTarget[],
        x: number,
        y: number,
        insertion?: BayInsertion
    } | null>(null)
    const [panel, setPanel] = useState<EditorAction | null>(null)


    const load = () => {
        setLoading(true)
        loadNetwork("/home/leclercclm/Projects/powsybl/powsybl-incubator/network-editor-demo/data/reseau.xiidm").then(
            async (loaded) => {
                setInfo(loaded)
                console.log("Network loaded")
                setLoading(false)
            }
        ).catch((err) => {
            setError(String(err));
            setLoading(false)
        })

    }

    const loadVlIds = () => {
        getVoltageLevelIds().then(
            async (ids) => {
                setVlIds(ids)
            }).catch((err) => {
            setError(String(err))
        })
    }

    useEffect(() => {
        if (!info) return
        loadVlIds()
    }, [info])

    useEffect(() => {
        if (!selectedVlId) return
        getSld(selectedVlId).then(
            async (sld) => {
                setSld(sld)
            }
        ).catch((err) => {
            setError(String(err))
        })
    }, [selectedVlId])


    useEffect(() => {
        if (!sld || !container.current) return

        const instance = new NetworkEditor({
            container: container.current,
            metadata: sld.metadata,
            svgContent: sld.svg,
            initialProperties: sld.properties,

            onTargets: ({targets, position, insertion}) =>
                setMenu({targets, x: position.x, y: position.y, insertion}),

            onEvent: (event) => {
                if (event.name === 'targets:changed') setMenu(null);
                if (event.name === 'history:changed') setHistory({canRedo: event.canRedo, canUndo: event.canUndo});
                if (event.name === 'element:selected') {
                    setPanel(
                        event.elements.length === 1
                            ? actionFor(instance, event.elements[0].id, 'UPDATE_PROPERTIES')
                            : null,
                    )
                }
                if (event.name === 'model:changed') {
                    setChanges(event.changeSet);
                    console.log(event.changeSet);
                }

            },
        });
        setEditor(instance);
        return () => instance.destroy();
    }, [sld])


    const menuItems: MenuItem[] =
        editor && menu
            ? menuItemsFor(editor, menu).map((item) => ({
                label: item.label,
                enabled: item.enabled,
                danger: item.danger,
                onClick: () => (item.needsForm ? setPanel(item.action) : item.action.run()),
            }))
            : [];


    const applyChanges = async () => {

        if (!selectedVlId || !editor) return;
        setError("")

        try {
            for (const change of changes) {
                switch (change.op) {
                    case 'delete' :
                        await deleteElement(change.equipmentId)
                        break;
                    case 'delete-bay' :
                        await deleteBay(change.equipmentId)
                        break;
                    case 'update' :
                        await update(change)
                        break;
                }
            }

            editor.clearPendingChanges();
            setSld(await getSld(selectedVlId));
        } catch (error) {
            setError(String(error));
        }
    }

    return (
        <>
            <Header
                appearance="brand"
                applicationName="Network Editor - Démo"
                compactSpacing
                logoSrc="https://opensource.rte-france.com/design-system-rte/react/assets/rte-BsegNGhf.png"
                navigationItems={[
                    {
                        href: '/',
                        label: 'Accueil'
                    },
                ]}
            />

            {error &&
                <Banner
                    type="error"
                    position="push"
                    message={error}
                    isOpen
                    isCompact
                    closable
                    onClose={() => setError("")}
                />
            }

            <main className="app">
                <aside className="sidebar">
                    <Card cardType="outlined" width="95%">
                        <div className="card-body">
                            <h2 className="block-title">Réseau</h2>

                            <Button
                                label="Charger le réseau"
                                onClick={() => load()}
                                variant="primary"
                                disabled={loading}
                            />

                            {loading &&
                                <Loader
                                    appearance="brand"
                                    label="Chargement..."
                                    labelPosition="right"
                                    showLabel
                                    size="medium"
                                />
                            }

                            {info &&
                                <dl className="infos">
                                    <dt>Identifiant</dt>
                                    <dd>{info.id}</dd>
                                    <dt>Postes</dt>
                                    <dd>{info.substation_count}</dd>
                                    <dt>Niveaux de tension</dt>
                                    <dd>{info.vl_count}</dd>
                                </dl>
                            }
                        </div>
                    </Card>

                    {vlIds &&
                        <Card cardType="outlined" width="95%">
                            <div className="card-body">
                                <Select
                                    id="selectVl"
                                    label="Niveau de tension"
                                    width={240}
                                    onChange={(value) => setSelectedVlId(value)}
                                    optionToDisplay="first-selected"
                                    options={vlIds.map(id => ({
                                        label: id,
                                        value: id
                                    }))}
                                    placeholder="Choisir un niveau de tension"
                                    showLabel
                                    value={selectedVlId ?? ""}
                                />
                            </div>
                        </Card>
                    }

                    {sld &&
                        <Card cardType="outlined" width="100%">
                            <div className="card-body">
                                <h2 className="block-title">Modifications en attente</h2>

                                {changes.length === 0 ?
                                    <p className="changes-empty">Aucune modification.</p>
                                    :
                                    <ul className="changes">
                                        {changes.map((change, index) =>
                                            <li key={`${change.op}-${change.equipmentId}-${index}`}>
                                                <span className="change-op">{CHANGE_LABELS[change.op]}</span>
                                                <span className="change-id">{change.equipmentId}</span>
                                            </li>
                                        )}
                                    </ul>
                                }
                            </div>
                        </Card>
                    }
                </aside>

                <section className="workspace">
                    {sld ?
                        <>
                            <div className="toolbar">
                                <div className="toolbar-group">
                                    <Button
                                        label="Annuler"
                                        variant="secondary"
                                        disabled={!history.canUndo}
                                        onClick={() => editor?.undo()}
                                    />
                                    <Button
                                        label="Rétablir"
                                        variant="secondary"
                                        disabled={!history.canRedo}
                                        onClick={() => editor?.redo()}
                                    />
                                </div>

                                <div className="toolbar-group">
                                    {changes.length > 0 &&
                                        <span className="pending">
                                            {changes.length} modification{changes.length > 1 ? 's' : ''} en attente
                                        </span>
                                    }
                                    <Button
                                        label="Valider les modifications"
                                        variant="primary"
                                        disabled={!changes.length}
                                        onClick={() => applyChanges()}
                                    />
                                </div>
                            </div>

                            <div className="diagram" ref={container}/>
                        </>
                        :
                        <p className="empty">
                            Charge le réseau, puis choisis un niveau de tension pour afficher son schéma.
                        </p>
                    }
                </section>

                {panel &&
                    <aside className="properties">
                        <h2 className="block-title">{actionLabel(panel)}</h2>
                        <PropertyForm
                            action={panel}
                            submitLabel="Valider"
                            refusedLabel="Refusé par le composant"
                            onDone={() => setPanel(null)}
                        >
                            <div className="properties-actions">
                                <Button label="Valider" variant="primary" type="submit"/>
                                <Button
                                    label="Fermer"
                                    variant="secondary"
                                    type="button"
                                    onClick={() => setPanel(null)}
                                />
                            </div>
                        </PropertyForm>
                    </aside>
                }

                {menu &&
                    <Menu
                        header={describeTargets(menu.targets)}
                        x={menu.x}
                        y={menu.y}
                        onClose={() => setMenu(null)}
                        items={menuItems}
                    />
                }
            </main>
        </>
    );
}
