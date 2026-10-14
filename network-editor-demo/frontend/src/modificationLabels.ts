import type {NetworkModification} from "@powsybl/network-editor";

const LABELS: Partial<Record<NetworkModification['type'], string>> = {
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

export const modificationLabel = (modification: NetworkModification) =>
    LABELS[modification.type] ?? (modification.type.endsWith('_MODIFICATION') ? 'Modification' : modification.type)

export const modificationTarget = (modification: NetworkModification) => {
    if ('equipmentId' in modification) return modification.equipmentId
    if (modification.type === 'NODE_INJECTION_CREATION' || modification.type === 'SWITCHED_INJECTION_CREATION') {
        return modification.creation.equipmentId
    }
    return modification.voltageLevelId
}
