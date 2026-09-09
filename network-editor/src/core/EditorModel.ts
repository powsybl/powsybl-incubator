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

    private readonly componentSizes = new Map<string, { width: number; height: number }>();

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

        for (const component of this.metadata.components ?? []) {
            this.componentSizes.set(component.type, component.size);
        }
    }

    componentSize(componentType: string): { width: number; height: number } {
        return this.componentSizes.get(componentType) ?? { width: 0, height: 0 };
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
        const seenIidmNodes = new Set<string>();

        const occupied = new Set(
            this.metadata.nodes
                .filter((node) => isEquipmentNode(node) && node.iidmNode !== undefined)
                .map((node) => iidmKey(node.vid ?? '', node.iidmNode!)),
        );

        for (const node of this.metadata.nodes) {
            const vlId = node.vid ?? '';

            if (isBusBarNode(node)) {
                targets.push({
                    kind: 'BUSBAR',
                    id: node.id,
                    vlId,
                    busbarSectionId: node.equipmentId,
                    busbarIndex: node.busbarIndex,
                    sectionIndex: node.sectionIndex,
                    node: node.iidmNode,
                });
                continue;
            }

            if (isHiddenNode(node)) {
                if (node.iidmNode === undefined) continue;
                const seen = iidmKey(vlId, node.iidmNode);
                if (seenIidmNodes.has(seen)) continue;
                seenIidmNodes.add(seen);

                targets.push({
                    kind: 'NODE',
                    id: node.id,
                    vlId,
                    node: node.iidmNode,
                    occupied: occupied.has(seen),
                });
                continue;
            }

            if (isEquipmentNode(node)) {
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

        }

        return targets;
    }

    hasSwitchBetween(vlId: string, a: number, b: number): boolean {
        return this.metadata.nodes.some(
            (node) =>
                (node.vid ?? '') === vlId &&
                node.iidmNode1 !== undefined &&
                node.iidmNode2 !== undefined &&
                ((node.iidmNode1 === a && node.iidmNode2 === b) ||
                    (node.iidmNode1 === b && node.iidmNode2 === a)),
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

    clearProperties(equipmentId: string): void {
        this.properties.delete(equipmentId);
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

function iidmKey(vlId: string, iidmNode: number): string {
    return `${vlId}#${iidmNode}`;
}

function isHiddenNode(node: NodeMetadata): boolean {
    return node.componentType === HIDDEN_NODE_TYPE;
}

function isEquipmentNode(node: NodeMetadata): node is NodeMetadata & {equipmentId: string} {
    return node.equipmentId !== undefined && !isHiddenNode(node);
}

function isBusBarNode(node: NodeMetadata): node is NodeMetadata & {
    equipmentId: string;
    iidmNode: number;
    busbarIndex: number;
    sectionIndex: number;
} {
    return (
        node.componentType === BUSBAR_SECTION_TYPE &&
        Boolean(node.equipmentId) &&
        node.iidmNode !== undefined &&
        node.busbarIndex !== undefined &&
        node.sectionIndex !== undefined
    );
}

