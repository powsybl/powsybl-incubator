import {
    BAY_TRAVERSABLE_TYPES,
    type EditorMetadata,
    type FeederInfoMetadata,
    type NodeMetadata,
    type SLDMetadata,
    type WireMetadata,
} from './types';

export class EditorModel {
    private readonly metadata: EditorMetadata;

    private readonly nodesById = new Map<string, NodeMetadata>();

    private readonly nodesByEquipmentId = new Map<string, NodeMetadata[]>();

    private readonly wiresByNode = new Map<string, WireMetadata[]>();

    private readonly feederInfosById = new Map<string, FeederInfoMetadata>();

    constructor(metadata: SLDMetadata) {
        this.metadata = metadata as EditorMetadata;
        this.buildIndexes();
    }

    private buildIndexes(): void {
        for (const node of this.metadata.nodes) {
            this.indexNode(node);
        }

        for (const wire of this.metadata.wires) {
            this.linkWire(wire.nodeId1, wire);
            this.linkWire(wire.nodeId2, wire);
        }

        for (const info of this.metadata.feederInfos ?? []) {
            this.feederInfosById.set(info.id, info);
        }
    }

    private indexNode(node: NodeMetadata): void {
        this.nodesById.set(node.id, node);
        if (!node.equipmentId) return;
        const siblings = this.nodesByEquipmentId.get(node.equipmentId);
        if (siblings) {
            siblings.push(node);
        } else {
            this.nodesByEquipmentId.set(node.equipmentId, [node]);
        }
    }

    private linkWire(nodeId: string, wire: WireMetadata): void {
        if (!nodeId) return;
        const existing = this.wiresByNode.get(nodeId);
        if (existing) {
            existing.push(wire);
        } else {
            this.wiresByNode.set(nodeId, [wire]);
        }
    }

    getMetadata(): EditorMetadata {
        return this.metadata;
    }

    getNodeById(nodeId: string): NodeMetadata | undefined {
        return this.nodesById.get(nodeId);
    }


    resolveNodeForSvgId(svgId: string): NodeMetadata | undefined {
        const direct = this.nodesById.get(svgId);
        if (direct) return direct;
        const info = this.feederInfosById.get(svgId);
        if (!info) return undefined;
        // "<nodeId>_95_ARROW_95_XXX": the prefix targets the exact node
        // (required for 3WT legs). Fall back on the equipment index.
        const byPrefix = this.nodesById.get(svgId.replace(/_95_ARROW_95_.*$/, ''));
        return byPrefix ?? this.getNodesForEquipment(info.equipmentId)[0];
    }

    getNodesForEquipment(equipmentId: string): NodeMetadata[] {
        return this.nodesByEquipmentId.get(equipmentId) ?? [];
    }

    getWiresForNode(nodeId: string): WireMetadata[] {
        return this.wiresByNode.get(nodeId) ?? [];
    }

    getFeederInfosForEquipment(equipmentId: string): FeederInfoMetadata[] {
        const result: FeederInfoMetadata[] = [];
        for (const info of this.feederInfosById.values()) {
            if (info.equipmentId === equipmentId) result.push(info);
        }
        return result;
    }

    removeNode(nodeId: string): NodeMetadata | undefined {
        const node = this.nodesById.get(nodeId);
        if (!node) return undefined;

        this.nodesById.delete(nodeId);
        if (node.equipmentId) {
            const siblings = this.nodesByEquipmentId.get(node.equipmentId);
            if (siblings) {
                const index = siblings.indexOf(node);
                if (index !== -1) siblings.splice(index, 1);
                if (siblings.length === 0) {
                    this.nodesByEquipmentId.delete(node.equipmentId);
                }
            }
        }
        removeById(this.metadata.nodes, nodeId);
        return node;
    }

    addNode(node: NodeMetadata): void {
        this.indexNode(node);
        this.metadata.nodes.push(node);
    }

    removeWire(wireId: string): WireMetadata | undefined {
        const wire = removeById(this.metadata.wires, wireId);
        if (!wire) return undefined;

        this.unlinkWire(wire.nodeId1, wire);
        this.unlinkWire(wire.nodeId2, wire);
        return wire;
    }

    addWire(wire: WireMetadata): void {
        this.metadata.wires.push(wire);
        this.linkWire(wire.nodeId1, wire);
        this.linkWire(wire.nodeId2, wire);
    }

    removeFeederInfo(id: string): FeederInfoMetadata | undefined {
        const info = this.feederInfosById.get(id);
        if (!info) return undefined;

        this.feederInfosById.delete(id);
        removeById(this.metadata.feederInfos ?? [], id);
        return info;
    }

    addFeederInfo(info: FeederInfoMetadata): void {
        this.feederInfosById.set(info.id, info);
        (this.metadata.feederInfos ??= []).push(info);
    }

    private unlinkWire(nodeId: string, wire: WireMetadata): void {
        const list = this.wiresByNode.get(nodeId);
        if (!list) return;
        const index = list.indexOf(wire);
        if (index !== -1) list.splice(index, 1);
    }

    /** Scope of a plain equipment delete: its nodes and their direct wires. */
    collectElementScope(equipmentId: string): { nodes: NodeMetadata[]; wires: WireMetadata[] } {
        const nodes = this.getNodesForEquipment(equipmentId);
        const wires = new Map<string, WireMetadata>();

        for (const node of nodes) {
            for (const wire of this.getWiresForNode(node.id)) {
                wires.set(wire.id, wire);
            }
        }

        return {nodes: [...nodes], wires: [...wires.values()]};
    }

    collectBay(equipmentId: string): { nodes: NodeMetadata[]; wires: WireMetadata[] } {
        const nodes = new Map(
            this.getNodesForEquipment(equipmentId).map((node) => [node.id, node]),
        );
        const wires = new Map<string, WireMetadata>();
        const queue = [...nodes.values()];

        while (queue.length > 0) {
            const node = queue.pop()!;
            for (const wire of this.getWiresForNode(node.id)) {
                wires.set(wire.id, wire);

                const other = this.otherEnd(wire, node.id);
                if (!other || nodes.has(other.id)) continue;
                if (!canTraverseNode(other)) continue;
                if (this.isSharedFork(other, nodes)) continue;

                nodes.set(other.id, other);
                queue.push(other);
            }
        }
        return {nodes: [...nodes.values()], wires: [...wires.values()]};
    }

    private isSharedFork(
        node: NodeMetadata,
        bayNodes: ReadonlyMap<string, NodeMetadata>,
    ): boolean {
        if (this.getWiresForNode(node.id).length <= 2) return false;
        return this.reachableEquipments([node], bayNodes.keys()).length > 0;
    }

    private reachableEquipments(
        seeds: NodeMetadata[],
        blocked: Iterable<string> = [],
    ): NodeMetadata[] {
        const visited = new Set<string>(blocked);
        for (const seed of seeds) visited.add(seed.id);
        const equipments: NodeMetadata[] = [];
        const queue = [...seeds];

        while (queue.length > 0) {
            const node = queue.pop()!;
            for (const wire of this.getWiresForNode(node.id)) {
                const other = this.otherEnd(wire, node.id);
                if (!other || visited.has(other.id)) continue;
                visited.add(other.id);

                if (other.componentType === 'BUSBAR_SECTION') continue;
                if (canTraverseNode(other)) queue.push(other);
                else equipments.push(other);
            }
        }
        return equipments;
    }

    private otherEnd(wire: WireMetadata, nodeId: string): NodeMetadata | undefined {
        return this.nodesById.get(
            wire.nodeId1 === nodeId ? wire.nodeId2 : wire.nodeId1,
        );
    }

}

function canTraverseNode(node: NodeMetadata): boolean {
    return BAY_TRAVERSABLE_TYPES.has(node.componentType ?? '');
}

function removeById<T extends { id: string }>(
    array: T[],
    id: string,
): T | undefined {
    const index = array.findIndex((entry) => entry.id === id);
    if (index === -1) return undefined;
    return array.splice(index, 1)[0];
}
