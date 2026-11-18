import type {NetworkChange} from "@powsybl/network-editor";

const LABELS: Record<NetworkChange['kind'], string> = {
    CREATE_FEEDER_BAY: 'Création de travée',
    CREATE_INJECTION: 'Création sur un nœud',
    CREATE_SWITCH: 'Création d\'organe de coupure',
    CREATE_BUSBAR_SECTION: 'Création de jeu de barres',
    CREATE_COUPLING_DEVICE: 'Création de couplage',
    REMOVE_FEEDER_BAY: 'Suppression de travée',
    REMOVE_ELEMENT: 'Suppression d\'élément',
    MOVE_FEEDER_BAY: 'Déplacement de travée',
    UPDATE_ATTRIBUTES: 'Modification',
    RENAME: 'Renommage',
}

export const changeLabel = (change: NetworkChange) => {
    if (change.kind === 'CREATE_INJECTION' && change.switch) return 'Création derrière un organe de coupure'
    return LABELS[change.kind]
}

export const changeTarget = (change: NetworkChange) => {
    switch (change.kind) {
        case 'REMOVE_FEEDER_BAY':
        case 'MOVE_FEEDER_BAY':
            return change.connectableId
        case 'CREATE_COUPLING_DEVICE':
            return `${change.busOrBusbarSectionId1} ↔ ${change.busOrBusbarSectionId2}`
        default:
            return change.id
    }
}
