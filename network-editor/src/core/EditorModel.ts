import {
    BAY_TRAVERSABLE_TYPES,
    HIDDEN_NODE_TYPE,
    SWITCH_TYPES,
    type ConnectionPoint,
    type DeleteScope,
    type EditorMetadata,
    type FeederInfoMetadata,
    type NodeMetadata,
    type SLDMetadata,
    type WireMetadata,
} from './types';
import type { EquipmentProperties } from './types';

export class EditorModel {
    private readonly metadata: EditorMetadata;

    private readonly nodesById = new Map<string, NodeMetadata>();

    private readonly nodesByEquipmentId = new Map<string, NodeMetadata[]>();

    private readonly wiresByNode = new Map<string, WireMetadata[]>();

    private readonly feederInfosById = new Map<string, FeederInfoMetadata>();

    private readonly properties = new Map<string, EquipmentProperties>();

    private readonly initialWireCount = new Map<string, number>();

    constructor(
        metadata: SLDMetadata,
        initialProperties?: Record<string, EquipmentProperties>,
    ) {
        this.metadata = metadata as EditorMetadata;
        this.buildIndexes();
        for (const [equipmentId, values] of Object.entries(initialProperties ?? {})) {
            this.seedProperties(equipmentId, values);
        }
    }

    private buildIndexes(): void {
        for (const node of this.metadata.nodes) {
            this.indexNode(node);
            this.seedSwitchState(node);
        }

        for (const wire of this.metadata.wires) {
            this.linkWire(wire.nodeId1, wire);
            this.linkWire(wire.nodeId2, wire);
        }

        for (const info of this.metadata.feederInfos ?? []) {
            this.feederInfosById.set(info.id, info);
        }

        for (const [nodeId, wires] of this.wiresByNode) {
            this.initialWireCount.set(nodeId, wires.length);
        }

        this.setIidmIdToHiddenNodes();
    }

    private setIidmIdToHiddenNodes(): void {
        for (const node of this.metadata.nodes) {
            if (node.iidmNode === undefined || !node.equipmentId) continue;

            for (const wire of this.getWiresForNode(node.id)) {
                const otherEnd = this.otherEnd(wire, node.id);
                if (otherEnd && otherEnd.iidmNode === undefined && isHiddenNode(otherEnd)) {
                    otherEnd.iidmNode = node.iidmNode;
                }
            }
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

    private seedSwitchState(node: NodeMetadata): void {
        if (!node.equipmentId || node.open === undefined) return;
        if (!SWITCH_TYPES.has(node.componentType)) return;
        this.properties.set(node.equipmentId, { open: node.open });
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

    /** SLD node id -> IIDM node it stands on, for every node that names one. */
    collectIidmNodes(): Map<string, number> {
        const iidmNodes = new Map<string, number>();
        for (const node of this.metadata.nodes) {
            if (node.iidmNode !== undefined) iidmNodes.set(node.id, node.iidmNode);
        }
        return iidmNodes;
    }

    /** Fictitious node id -> whether it names an IIDM node. */
    collectFictitiousNodes(): Map<string, boolean> {
        const fictitious = new Map<string, boolean>();
        for (const node of this.metadata.nodes) {
            if (isHiddenNode(node)) fictitious.set(node.id, node.iidmNode !== undefined);
        }
        return fictitious;
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
    collectElementScope(equipmentId: string): DeleteScope {
        const nodes = this.getNodesForEquipment(equipmentId);
        const wires = new Map<string, WireMetadata>();

        for (const node of nodes) {
            for (const wire of this.getWiresForNode(node.id)) {
                wires.set(wire.id, wire);
            }
        }

        return {nodes: [...nodes], wires: [...wires.values()]};
    }

    collectBay(equipmentId: string): DeleteScope {
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

    collectConnectionPoints(): ConnectionPoint[] {
        const occupied = new Set(
            this.metadata.nodes
                .filter((node) => node.equipmentId && node.iidmNode !== undefined)
                .map((node) => node.iidmNode),
        );
        const points: ConnectionPoint[] = [];

        for (const node of this.metadata.nodes) {
            if (!isHiddenNode(node) || occupied.has(node.iidmNode)) continue;

            if (node.iidmNode === undefined) continue;

            const wires = this.getWiresForNode(node.id);
            if (wires.length >= (this.initialWireCount.get(node.id) ?? 0)) continue;

            const attachedTo = wires
                .map((wire) => this.otherEnd(wire, node.id)?.equipmentId)
                .find((equipmentId) => equipmentId !== undefined);
            if (!attachedTo) continue;

            points.push({
                id: node.id,
                anchor: {
                    kind: 'FREE_NODE',
                    vlId: node.vid ?? '',
                    attachedTo,
                    node: node.iidmNode,
                },
            });
        }
        return points;
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

    getProperties(equipmentId: string): EquipmentProperties {
        return { ...this.properties.get(equipmentId) };
    }

    seedProperties(equipmentId: string, values: EquipmentProperties): void {
        this.properties.set(equipmentId, { ...values });
        this.updateSwitchMetadata(equipmentId, values.open);
    }

    setProperties(equipmentId: string, values: EquipmentProperties): void {
        const merged: EquipmentProperties = {
            ...this.properties.get(equipmentId),
            ...values,
        };
        for (const key of Object.keys(merged)) {
            if (merged[key] === undefined) delete merged[key];
        }
        this.properties.set(equipmentId, merged);
        this.updateSwitchMetadata(equipmentId, merged.open);
    }


    private updateSwitchMetadata(
        equipmentId: string,
        open: EquipmentProperties[string] | undefined,
    ): void {
        if (typeof open !== 'boolean') return;
        for (const node of this.getNodesForEquipment(equipmentId)) {
            if (SWITCH_TYPES.has(node.componentType)) node.open = open;
        }
    }
}

function canTraverseNode(node: NodeMetadata): boolean {
    return BAY_TRAVERSABLE_TYPES.has(node.componentType);
}

function isHiddenNode(node: NodeMetadata): boolean {
    return node.componentType === HIDDEN_NODE_TYPE && !node.equipmentId;
}

function removeById<T extends { id: string }>(
    array: T[],
    id: string,
): T | undefined {
    const index = array.findIndex((entry) => entry.id === id);
    if (index === -1) return undefined;
    return array.splice(index, 1)[0];
}
