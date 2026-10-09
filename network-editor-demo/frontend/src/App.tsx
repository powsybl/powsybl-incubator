import {Banner, Button, Card, Header, Loader, Select} from "@design-system-rte/react";
import { useEffect, useRef, useState} from "react";
import type {NetworkInfo, Sld} from "./types.ts";
import {applyModifications, getSld, getVoltageLevelIds, uploadNetwork} from "./api.ts";
import {isAxiosError} from "axios";
import {describeTargets, type Gesture, menuItemsFor, NetworkEditor, type NetworkModification} from "@powsybl/network-editor";
import type {EditTarget} from "@powsybl/network-editor";
import type {MenuItem} from "./components/Menu.tsx";
import {Menu} from "./components/Menu.tsx";
import { actionLabel, type EditorAction} from "@powsybl/network-editor";
import {PropertyForm} from "@powsybl/network-editor/react";

const CHANGE_LABELS: Partial<Record<NetworkModification['type'], string>> = {
    LOAD_CREATION: 'Création de charge',
    GENERATOR_CREATION: 'Création de générateur',
    BATTERY_CREATION: 'Création de batterie',
    SHUNT_COMPENSATOR_CREATION: 'Création de compensateur shunt',
    STATIC_VAR_COMPENSATOR_CREATION: 'Création de CSPR',
    CONVERTER_STATION_CREATION: 'Création de station VSC',
    LCC_CONVERTER_STATION_CREATION: 'Création de station LCC',
    BOUNDARY_LINE_CREATION: 'Création de ligne frontière',
    NODE_INJECTION_CREATION: 'Création sur un nœud',
    SWITCHED_INJECTION_CREATION: 'Création derrière un organe de coupure',
    SWITCH_CREATION: 'Création d\'organe de coupure',
    MOVE_VOLTAGE_LEVEL_FEEDER_BAYS: 'Déplacement de travée',
    EQUIPMENT_ATTRIBUTE_MODIFICATION: 'Modification d\'attribut',
    EQUIPMENT_DELETION: 'Suppression',
    ELEMENT_DELETION: 'Suppression d\'élément',
    EQUIPMENT_RENAME: 'Renommage',
}

const changeLabel = (change: NetworkModification) =>
    CHANGE_LABELS[change.type] ?? (change.type.endsWith('_MODIFICATION') ? 'Modification' : change.type)

const changeTarget = (change: NetworkModification) => {
    if ('equipmentId' in change) return change.equipmentId
    if (change.type === 'NODE_INJECTION_CREATION' || change.type === 'SWITCHED_INJECTION_CREATION') {
        return change.creation.equipmentId
    }
    return change.voltageLevelId
}

const errorMessage = (error: unknown) =>
    isAxiosError(error) && error.response?.data?.message ? error.response.data.message : String(error)

export default function App() {

    const container = useRef<HTMLDivElement>(null);
    const fileInput = useRef<HTMLInputElement>(null);

    const [error, setError] = useState<string>("")
    const [loading, setLoading] = useState<boolean>(false)
    const [info, setInfo] = useState<NetworkInfo | null>(null)
    const [fileName, setFileName] = useState<string>("")
    const [vlIds, setVlIds] = useState<string[] | null>(null)
    const [selectedVlId, setSelectedVlId] = useState<string | null>(null)
    const [sld, setSld] = useState<Sld | null>(null)
    const [showIidmNodes, setShowIidmNodes] = useState<boolean>(false)

    const [history, setHistory] = useState<{ canUndo: boolean; canRedo: boolean }>({canUndo: false, canRedo: false})
    const [changes, setChanges] = useState<NetworkModification[]>([])
    const [gesture, setGesture] = useState<Gesture | null>(null)

    const [editor, setEditor] = useState<NetworkEditor | null>(null);
    const [menu, setMenu] = useState<{
        targets: readonly EditTarget[],
        x: number,
        y: number,
        insertion?: number
    } | null>(null)
    const [panel, setPanel] = useState<EditorAction | null>(null)


    const load = (file: File) => {
        setLoading(true)
        setError("")
        setVlIds(null)
        setSelectedVlId(null)
        setSld(null)
        setChanges([])
        setPanel(null)
        setMenu(null)

        uploadNetwork(file).then(
            (loaded) => {
                setInfo(loaded)
                setFileName(file.name)
                setLoading(false)
            }
        ).catch((err) => {
            setInfo(null)
            setFileName("")
            setError(String(err));
            setLoading(false)
        })

    }

    useEffect(() => {

        loadVlIds();
        setSelectedVlId("MTPELP6")

        getSld("MTPELP6").then(
            async (sld) => {
                setSld(sld)
            }).catch((err) => {
            setError(String(err))
        })
    }, [])

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
                setSld(sld);
                setShowIidmNodes(false);
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

                if (event.name === 'model:changed') {
                    setChanges(event.changeSet);
                    console.log(event.changeSet);
                }
                if (event.name === 'gesture:changed') {
                    setGesture(event.gesture);
                    if (event.gesture) setMenu(null);
                    setPanel(instance.switchAction());
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
                danger: item.danger,
                onClick: () => (item.needsForm ? setPanel(item.action) : item.action.run()),
            }))
            : [];


    const applyChanges = async () => {

        if (!selectedVlId || !editor) return;
        setError("")

        try {
            await applyModifications(changes);

            editor.clearPendingChanges();
            setSld(await getSld(selectedVlId));
        } catch (error) {
            setError(errorMessage(error));
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

                            <input
                                ref={fileInput}
                                type="file"
                                accept=".xiidm,.iidm"
                                hidden
                                onChange={(event) => {
                                    const file = event.target.files?.[0]
                                    if (file) load(file)
                                    event.target.value = ""
                                }}
                            />

                            <Button
                                label="Charger le réseau"
                                onClick={() => fileInput.current?.click()}
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
                                    <dt>Fichier</dt>
                                    <dd>{fileName}</dd>
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
                                    })).filter((a) => a.label.charAt(0)==='M')}
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
                                            <li key={`${change.type}-${index}`}>
                                                <span className="change-op">{changeLabel(change)}</span>
                                                <span className="change-id">{changeTarget(change)}</span>
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
                                    <Button
                                        label="Afficher Iidm"
                                        variant="secondary"
                                        onClick={() => {
                                            editor?.showIidmNodes(!showIidmNodes)
                                            setShowIidmNodes(!showIidmNodes)
                                        }}
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
                                    onClick={() => {
                                        setPanel(null)
                                        editor?.cancelGesture();
                                    }}
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
