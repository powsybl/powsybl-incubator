import type { Command } from './Command';
import type {
    BusbarTarget,
    ChangeSetEntry,
    ElementType,
    EquipmentProperties,
    FeederDirection,
    GapTarget,
    NodeTarget,
    OrderClaim,
} from '../types';

export class CreateCommand implements Command {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string };

    readonly orderClaim?: OrderClaim;

    constructor(
        readonly equipmentId: string,
        private readonly type: ElementType,
        private readonly target: NodeTarget | BusbarTarget | GapTarget,
        private readonly properties: EquipmentProperties,
        markerNodeId: string,
        private readonly bay?: { order: number; direction: FeederDirection },
    ) {
        this.pendingMarker = { targetId: target.id, nodeId: markerNodeId, label: equipmentId };
        if (target.kind === 'BUSBAR' && bay) {
            this.orderClaim = {
                vlId: target.vlId,
                sectionIndex: target.sectionIndex,
                order: bay.order,
            };
        }
    }

    execute(): void {}

    undo(): void {}

    toChangeSetEntry(): ChangeSetEntry {
        const equipmentId = this.equipmentId;

        switch (this.target.kind) {
            case 'NODE':
                return {
                    op: 'create',
                    equipmentId,
                    payload: {
                        equipmentType: this.type,
                        vlId: this.target.vlId,
                        node: this.target.node,
                        properties: this.properties,
                    },
                };
            case 'BUSBAR':
                return {
                    op: 'create-bay',
                    equipmentId,
                    payload: {
                        equipmentType: this.type,
                        busbarSectionId: this.target.busbarSectionId,
                        order: this.bay!.order,
                        direction: this.bay!.direction,
                        properties: this.properties,
                    },
                };
            case 'GAP':
                return {
                    op: 'create-switch',
                    equipmentId,
                    payload: {
                        equipmentType: this.type,
                        vlId: this.target.vlId,
                        node1: this.target.node1,
                        node2: this.target.node2,
                        properties: this.properties,
                    },
                };
        }
    }
}
