import {Banner, Button, Card, Header, Loader, Select} from "@design-system-rte/react";
import {useEffect, useRef, useState} from "react";
import {isAxiosError} from "axios";
import {actionLabel} from "@powsybl/network-editor";
import {PropertyForm} from "@powsybl/network-editor/react";
import type {NetworkInfo, Sld} from "./types.ts";
import {applyModifications, getSld, getVoltageLevelIds, uploadNetwork} from "./api.ts";
import {Menu} from "./components/Menu.tsx";
import {modificationLabel, modificationTarget} from "./modificationLabels.ts";
import {useNetworkEditor} from "./useNetworkEditor.ts";

const errorMessage = (error: unknown) =>
    isAxiosError(error) && error.response?.data?.message ? error.response.data.message : String(error)

export default function App() {

    const container = useRef<HTMLDivElement>(null);
    const fileInput = useRef<HTMLInputElement>(null);

    const [error, setError] = useState<string>("")
    const [networkLoading, setNetworkLoading] = useState<boolean>(false)
    const [applyLoading, setApplyLoading] = useState<boolean>(false)
    const [info, setInfo] = useState<NetworkInfo | null>(null)
    const [fileName, setFileName] = useState<string>("")
    const [vlIds, setVlIds] = useState<string[] | null>(null)
    const [selectedVlId, setSelectedVlId] = useState<string | null>(null)
    const [sld, setSld] = useState<Sld | null>(null)
    const [showIidmNodes, setShowIidmNodes] = useState<boolean>(false)

    const {editor, menu, closeMenu, pick, form, closeForm, modifications, history} = useNetworkEditor(container, sld)

    const loadVlIds = () => {
        getVoltageLevelIds().then(setVlIds).catch((err) => setError(String(err)))
    }

    const loadSld = (vlId: string) => {
        getSld(vlId).then((loaded) => {
            setSld(loaded)
            setShowIidmNodes(false)
        }).catch((err) => setError(String(err)))
    }

    useEffect(() => {
        loadVlIds()
        setSelectedVlId("MTPELP6")
    }, [])

    useEffect(() => {
        if (selectedVlId) loadSld(selectedVlId)
    }, [selectedVlId])

    const load = (file: File) => {
        setNetworkLoading(true)
        setError("")
        setVlIds(null)
        setSelectedVlId(null)
        setSld(null)

        uploadNetwork(file).then((loaded) => {
            setInfo(loaded)
            setFileName(file.name)
            loadVlIds()
        }).catch((err) => {
            setInfo(null)
            setFileName("")
            setError(String(err))
        }).finally(() => setNetworkLoading(false))
    }

    const applyChanges = async () => {
        if (!selectedVlId || !editor) return
        setError("")
        setApplyLoading(true)
        try {
            await applyModifications(editor.getModifications())
            editor.clearModifications()
            loadSld(selectedVlId)
        } catch (error) {
            setError(errorMessage(error))
        }
        setApplyLoading(false)
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
                                disabled={networkLoading}
                            />

                            {networkLoading &&
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

                                {modifications.length === 0 ?
                                    <p className="changes-empty">Aucune modification.</p>
                                    :
                                    <ul className="changes">
                                        {modifications.map((modification, index) =>
                                            <li key={`${modification.type}-${index}`}>
                                                <span className="change-op">{modificationLabel(modification)}</span>
                                                <span className="change-id">{modificationTarget(modification)}</span>
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
                                    {modifications.length > 0 &&
                                        <span className="pending">
                                            {modifications.length} modification{modifications.length > 1 ? 's' : ''} en attente
                                        </span>
                                    }
                                    <Button
                                        label="Valider les modifications"
                                        variant="primary"
                                        disabled={!modifications.length || applyLoading}
                                        onClick={() => applyChanges()}
                                    />
                                    { applyLoading &&  <Loader
                                        appearance="brand"
                                        label=""
                                        labelPosition="right"
                                        showLabel
                                        size="medium"
                                    />
                                    }
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

                {form &&
                    <aside className="properties">
                        <h2 className="block-title">{actionLabel(form)}</h2>
                        <PropertyForm
                            action={form}
                            refusedLabel="Refusé par le composant"
                            onDone={closeForm}
                        >
                            <div className="properties-actions">
                                <Button label="Valider" variant="primary" type="submit"/>
                                <Button
                                    label="Fermer"
                                    variant="secondary"
                                    type="button"
                                    onClick={closeForm}
                                />
                            </div>
                        </PropertyForm>
                    </aside>
                }

                {menu && <Menu menu={menu} onPick={pick} onClose={closeMenu}/>}
            </main>
        </>
    );
}
