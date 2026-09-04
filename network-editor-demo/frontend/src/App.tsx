import {Button, Card, Header, Loader, Select} from "@design-system-rte/react";
import { useEffect, useRef, useState} from "react";
import type {NetworkInfo, Sld} from "./types.ts";
import {getSld, getVoltageLevelIds, loadNetwork} from "./api.ts";
import {describeTargets, menuItemsFor, NetworkEditor} from "@powsybl/network-editor";
import type {BayInsertion, EditTarget} from "@powsybl/network-editor";
import type {MenuItem} from "./components/Menu.tsx";
import {Menu} from "./components/Menu.tsx";
import {actionFor, actionLabel, type EditorAction} from "@powsybl/network-editor";
import {PropertyForm} from "@powsybl/network-editor/react";

export default function App() {

    const container = useRef<HTMLDivElement>(null);

    const [error, setError] = useState<string>("")
    const [loading, setLoading] = useState<boolean>(false)
    const [info, setInfo] = useState<NetworkInfo | null>(null)
    const [vlIds, setVlIds] = useState<string[] | null>(null)
    const [selectedVlId, setSelectedVlId] = useState<string | null>(null)
    const [sld, setSld] = useState<Sld | null>(null)

    const [history, setHistory] = useState<{ canUndo: boolean; canRedo: boolean }>({canUndo: false, canRedo: false})

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

            <div>
                <Button
                    label="Charger le réseau"
                    onClick={() => load()}
                    variant="primary"
                />
            </div>

            <div>
                {error && <p>{error}</p>}
            </div>

            <div>
                {loading && <Loader
                    appearance="brand"
                    label="Loading..."
                    labelPosition="right"
                    showLabel
                    size="medium"
                />
                }

                {info &&
                    <Card
                        cardType="default"
                        onClick={function Hs() {
                        }}
                        width="480px"
                    >
                        <div
                            style={{
                                padding: '16px'
                            }}
                        >
                            <h2
                                style={{
                                    fontSize: '20px',
                                    fontWeight: '600',
                                    margin: '0 0 12px 0'
                                }}
                            >
                                Informations sur le reseau
                            </h2>
                            <p
                                style={{
                                    color: '#666',
                                    lineHeight: '1.5',
                                    margin: '0 0 16px 0'
                                }}
                            >
                                {info.id}
                            </p>
                            <ul>
                                <li>Substations : {info.substation_count}</li>
                                <li>Voltages level : {info.vl_count}</li>
                            </ul>
                        </div>
                    </Card>
                }

                {
                    vlIds &&
                    <div
                        style={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '16px'
                        }}
                    >
                        <Select
                            id="selectVl"
                            label="Choisir un voltageLevel"
                            onChange={(value) => setSelectedVlId(value)}
                            optionToDisplay="first-selected"
                            options={vlIds.map(id => ({
                                label: id,
                                value: id
                            }))}
                            placeholder="Select an option"
                            showLabel
                            value={selectedVlId ?? ""}
                        />
                    </div>
                }
            </div>

            <div style={{display: 'flex', gap: '16px', alignItems: 'flex-start'}}>
                {sld &&
                    <div>
                        <div>
                            <Button
                                disabled={!history.canRedo}
                                label="redo"
                                variant="primary"
                                onClick={() => editor?.redo()}
                            />
                            <Button
                                disabled={!history.canUndo}
                                label="undo"
                                variant="primary"
                                onClick={() => editor?.undo()}

                            />
                        </div>

                        <div ref={container}/>


                    </div>
                }

                <div>
                    {panel &&
                        <div className="panel">
                            <h2>{actionLabel(panel)}</h2>
                            <PropertyForm
                                action={panel}
                                submitLabel="Valider"
                                refusedLabel="Refusé par le composant"
                                onDone={() => setPanel(null)}
                                children={<Button label="Valider" variant="secondary" type="submit" />}
                            />
                            <Button label="Annuler" variant="secondary" onClick={() => setPanel(null)}/>
                        </div>
                    }
                </div>

                <div>
                    {menu  &&
                        <Menu
                            header={describeTargets(menu.targets)}
                            x={menu.x}
                            y={menu.y}
                            onClose={() => setMenu(null)}
                            items={menuItems}
                        />
                    }
                </div>
            </div>
        </>

    );



}


