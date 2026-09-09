import type {SLDMetadata} from "@powsybl/network-viewer-core";
import type {EquipmentProperties} from "@powsybl/network-editor";

export interface NetworkInfo {
    id: string
    substation_count: number
    vl_count: number
}

export interface Sld {
    svg: string
    metadata: SLDMetadata
    properties: Record<string, EquipmentProperties>
}
