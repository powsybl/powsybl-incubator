import { pushTo, removeById, removeFrom } from './utils.ts';
import { isSwitchNode } from './operations';
import {
    BAY_TRAVERSABLE_TYPES,
    BUSBAR_SECTION_TYPE,
    HIDDEN_NODE_TYPE,
    NO_PENDING_ORDERS,
    ORDER_STEP,
    toDirection,
    toElementType,
    type BaySlot,
    type DeleteScope,
    type EditorMetadata,
    type EditTarget,
    type FeederInfoMetadata,
    type GapTarget,
    type NodeDiagnostic,
    type NodeMetadata,
    type PendingOrders,
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

    private readonly feederInfosByEquipmentId = new Map<string, FeederInfoMetadata[]>();

    private readonly properties = new Map<string, EquipmentProperties>();

    private readonly initialWireCount = new Map<string, number>();

    private readonly nodesByIidmNode = new Map<number, NodeMetadata[]>();

    private readonly gaps = new Map<string, GapTarget>();

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
            this.indexFeederInfo(info);
        }

        for (const [nodeId, wires] of this.wiresByNode) {
            this.initialWireCount.set(nodeId, wires.length);
        }

        this.resolveHiddenNodes();

        for (const node of this.metadata.nodes) {
            this.indexIidmNode(node);
        }
    }

    private resolveHiddenNodes(): void {
        let resolvedOne = true;
        while (resolvedOne) {
            resolvedOne = false;
            for (const node of this.metadata.nodes) {
                if (node.iidmNode !== undefined || !isHiddenNode(node)) continue;

                const resolved = this.intersectNeighbours(node);
                if (resolved !== undefined) {
                    node.iidmNode = resolved;
                    resolvedOne = true;
                }
            }
        }
    }

    private intersectNeighbours(node: NodeMetadata): number | undefined {
        let candidates: Set<number> | undefined;

        for (const wire of this.getWiresForNode(node.id)) {
            const other = this.otherEnd(wire, node.id);
            const ends = other ? namedNodes(other) : [];
            if (ends.length === 0) continue;

            if (candidates === undefined) {
                candidates = new Set(ends);
            } else {
                const kept = new Set<number>();
                for (const end of ends) {
                    if (candidates.has(end)) kept.add(end);
                }
                candidates = kept;
            }
            if (candidates.size === 0) return undefined;
        }

        return candidates?.size === 1 ? [...candidates][0] : undefined;
    }

    private indexIidmNode(node: NodeMetadata): void {
        if (node.iidmNode !== undefined) pushTo(this.nodesByIidmNode, node.iidmNode, node);
    }

    private indexNode(node: NodeMetadata): void {
        this.nodesById.set(node.id, node);
        if (node.equipmentId) pushTo(this.nodesByEquipmentId, node.equipmentId, node);
    }

    private seedSwitchState(node: NodeMetadata): void {
        if (!node.equipmentId || node.open === undefined) return;
        if (!isSwitchNode(node)) return;
        this.properties.set(node.equipmentId, { open: node.open });
    }

    private linkWire(nodeId: string, wire: WireMetadata): void {
        if (nodeId) pushTo(this.wiresByNode, nodeId, wire);
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

    /** Debug overlay: the IIDM node each drawn node stands on, if it names one. */
    collectNodeStatus(): Map<string, NodeDiagnostic> {
        const diagnostics = new Map<string, NodeDiagnostic>();
        for (const node of this.metadata.nodes) {
            const hidden = isHiddenNode(node);
            if (!hidden && node.iidmNode === undefined) continue;
            diagnostics.set(node.id, { iidmNode: node.iidmNode, hidden });
        }
        return diagnostics;
    }

    getNodesForEquipment(equipmentId: string): NodeMetadata[] {
        return this.nodesByEquipmentId.get(equipmentId) ?? [];
    }

    getWiresForNode(nodeId: string): WireMetadata[] {
        return this.wiresByNode.get(nodeId) ?? [];
    }

    getFeederInfosForEquipment(equipmentId: string): FeederInfoMetadata[] {
        return this.feederInfosByEquipmentId.get(equipmentId) ?? [];
    }

    removeNode(nodeId: string): NodeMetadata | undefined {
        const node = this.nodesById.get(nodeId);
        if (!node) return undefined;

        this.nodesById.delete(nodeId);
        if (node.equipmentId) removeFrom(this.nodesByEquipmentId, node.equipmentId, node);
        if (node.iidmNode !== undefined) removeFrom(this.nodesByIidmNode, node.iidmNode, node);
        this.openGap(node);
        removeById(this.metadata.nodes, nodeId);
        return node;
    }

    addNode(node: NodeMetadata): void {
        this.indexNode(node);
        this.indexIidmNode(node);
        this.gaps.delete(node.id);
        this.metadata.nodes.push(node);
    }

    /** Removing a switch leaves its two nodes facing each other: that is a gap. */
    private openGap(node: NodeMetadata): void {
        if (!isSwitchNode(node)) return;
        if (node.iidmNode1 === undefined || node.iidmNode2 === undefined) return;

        this.gaps.set(node.id, {
            kind: 'GAP',
            id: `GAP_${node.iidmNode1}_${node.iidmNode2}`,
            vlId: node.vid ?? '',
            node1: node.iidmNode1,
            node2: node.iidmNode2,
        });
    }

    getNodesForIidmNode(iidmNode: number): NodeMetadata[] {
        return this.nodesByIidmNode.get(iidmNode) ?? [];
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
        removeFrom(this.feederInfosByEquipmentId, info.equipmentId, info);
        removeById(this.metadata.feederInfos ?? [], id);
        return info;
    }

    addFeederInfo(info: FeederInfoMetadata): void {
        this.indexFeederInfo(info);
        (this.metadata.feederInfos ??= []).push(info);
    }

    renameEquipment(equipmentId: string, newId: string): void {
        const nodes = this.nodesByEquipmentId.get(equipmentId);
        if (!nodes) return;

        this.nodesByEquipmentId.delete(equipmentId);
        this.nodesByEquipmentId.set(newId, nodes);
        for (const node of nodes) node.equipmentId = newId;

        const infos = this.feederInfosByEquipmentId.get(equipmentId);
        if (infos) {
            this.feederInfosByEquipmentId.delete(equipmentId);
            this.feederInfosByEquipmentId.set(newId, infos);
            for (const info of infos) info.equipmentId = newId;
        }

        const properties = this.properties.get(equipmentId);
        if (properties) {
            this.properties.delete(equipmentId);
            this.properties.set(newId, properties);
        }
    }

    private indexFeederInfo(info: FeederInfoMetadata): void {
        this.feederInfosById.set(info.id, info);
        pushTo(this.feederInfosByEquipmentId, info.equipmentId, info);
    }

    private unlinkWire(nodeId: string, wire: WireMetadata): void {
        removeFrom(this.wiresByNode, nodeId, wire);
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

    collectTargets(): EditTarget[] {
        const targets: EditTarget[] = [];
        const seenEquipments = new Set<string>();
        const occupied = new Set(
            this.metadata.nodes
                .filter((node) => node.equipmentId !== undefined)
                .map((node) => node.iidmNode)
                .filter((iidmNode): iidmNode is number => iidmNode !== undefined),
        );

        for (const node of this.metadata.nodes) {
            const vlId = node.vid ?? '';

            if (node.componentType === BUSBAR_SECTION_TYPE) {
                if (
                    node.equipmentId &&
                    node.iidmNode !== undefined &&
                    node.busbarIndex !== undefined &&
                    node.sectionIndex !== undefined
                ) {
                    targets.push({
                        kind: 'BUSBAR',
                        id: node.id,
                        vlId,
                        busbarSectionId: node.equipmentId,
                        busbarIndex: node.busbarIndex,
                        sectionIndex: node.sectionIndex,
                    });
                }
                continue;
            }

            if (node.equipmentId) {
                if (seenEquipments.has(node.equipmentId)) continue;
                seenEquipments.add(node.equipmentId);
                targets.push({
                    kind: 'EQUIPMENT',
                    id: node.equipmentId,
                    vlId,
                    equipmentId: node.equipmentId,
                    type: toElementType(node.componentType),
                    node: node.iidmNode,
                    order: node.order,
                    direction: toDirection(node.direction),
                });
                continue;
            }

            if (this.isFreeNode(node, occupied)) {
                targets.push({ kind: 'NODE', id: node.id, vlId, node: node.iidmNode! });
            }
        }

        targets.push(...this.collectGaps());
        return targets;
    }

    private isFreeNode(node: NodeMetadata, occupied: ReadonlySet<number>): boolean {
        if (!isHiddenNode(node) || node.iidmNode === undefined) return false;
        if (occupied.has(node.iidmNode)) return false;
        return this.getWiresForNode(node.id).length < (this.initialWireCount.get(node.id) ?? 0);
    }

    private collectGaps(): GapTarget[] {
        return [...this.gaps.values()].filter(
            (gap) =>
                this.getNodesForIidmNode(gap.node1).length > 0 &&
                this.getNodesForIidmNode(gap.node2).length > 0,
        );
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

                if (other.componentType === BUSBAR_SECTION_TYPE) continue;
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

    nextOrderForBusbar(
        slot: BaySlot,
        pending: PendingOrders = NO_PENDING_ORDERS,
    ): number | undefined {
        const candidate = this.maxOrderInSection(slot, pending) + ORDER_STEP;
        return this.isOrderAvailable(slot, candidate, pending) ? candidate : undefined;
    }

    orderBetween(
        slot: BaySlot,
        left?: number,
        right?: number,
        pending: PendingOrders = NO_PENDING_ORDERS,
    ): number | undefined {
        if (right === undefined) return this.nextOrderForBusbar(slot, pending);

        const candidate =
            left === undefined
                ? Math.max(0, right - ORDER_STEP)
                : Math.floor((left + right) / 2);

        const framed = candidate < right && (left === undefined || candidate > left);
        return framed && this.isOrderAvailable(slot, candidate, pending) ? candidate : undefined;
    }

    isOrderAvailable(
        slot: BaySlot,
        order: number,
        pending: PendingOrders = NO_PENDING_ORDERS,
    ): boolean {
        if (!Number.isInteger(order) || order < 0) return false;

        for (const [section, taken] of this.ordersBySection(slot.vlId, pending)) {
            if (taken.includes(order)) return false;
            if (section > slot.sectionIndex && taken.some((used) => used <= order)) return false;
            if (section < slot.sectionIndex && taken.some((used) => used >= order)) return false;
        }
        return true;
    }

    feedersInSection(slot: BaySlot): NodeMetadata[] {
        return this.feedersBySection(slot.vlId).get(slot.sectionIndex) ?? [];
    }

    slotOfFeeder(node: NodeMetadata): BaySlot | undefined {
        const vlId = node.vid ?? '';
        for (const [sectionIndex, feeders] of this.feedersBySection(vlId)) {
            if (feeders.some((feeder) => feeder.id === node.id)) return { vlId, sectionIndex };
        }
        return undefined;
    }

    private maxOrderInSection(slot: BaySlot, pending: PendingOrders): number {
        const orders = this.ordersBySection(slot.vlId, pending).get(slot.sectionIndex) ?? [];
        return orders.length === 0 ? 0 : Math.max(...orders);
    }

    private ordersBySection(vlId: string, pending: PendingOrders): Map<number, number[]> {
        const orders = new Map<number, number[]>();
        for (const [section, feeders] of this.feedersBySection(vlId)) {
            orders.set(
                section,
                feeders
                    .filter((feeder) => !pending.vacated.has(feeder.id))
                    .map((feeder) => feeder.order!),
            );
        }
        for (const [section, claimed] of pending.claimed) {
            for (const order of claimed) pushTo(orders, section, order);
        }
        return orders;
    }

    private feedersBySection(vlId: string): Map<number, NodeMetadata[]> {
        const busbars = new Map<number, NodeMetadata[]>();
        for (const node of this.metadata.nodes) {
            if (node.componentType !== BUSBAR_SECTION_TYPE || node.vid !== vlId) continue;
            if (node.sectionIndex !== undefined) pushTo(busbars, node.sectionIndex, node);
        }

        const feeders = new Map<number, NodeMetadata[]>();
        for (const [section, sectionBusbars] of busbars) {
            feeders.set(
                section,
                this.reachableEquipments(sectionBusbars)
                    .filter((node) => node.order !== undefined)
                    .sort((a, b) => a.order! - b.order!),
            );
        }
        return feeders;
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
            if (isSwitchNode(node)) node.open = open;
        }
    }
}

function canTraverseNode(node: NodeMetadata): boolean {
    return BAY_TRAVERSABLE_TYPES.has(node.componentType);
}

function isHiddenNode(node: NodeMetadata): boolean {
    return node.componentType === HIDDEN_NODE_TYPE && !node.equipmentId;
}

function namedNodes(node: NodeMetadata): number[] {
    if (node.iidmNode !== undefined) return [node.iidmNode];
    if (node.iidmNode1 !== undefined && node.iidmNode2 !== undefined) {
        return [node.iidmNode1, node.iidmNode2];
    }
    return [];
}

