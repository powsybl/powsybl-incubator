import type { Command } from './Command';
import type {
    BayPosition,
    BaySlot,
    ChangeSetEntry,
    EquipmentTarget,
    NodeMetadata,
    OrderClaim,
} from '../types';

export class UpdateBayPositionCommand implements Command {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string };

    readonly orderClaim: OrderClaim;

    constructor(
        private readonly feeder: EquipmentTarget,
        node: NodeMetadata,
        slot: BaySlot,
        private readonly position: BayPosition,
    ) {
        this.pendingMarker = {
            targetId: feeder.id,
            nodeId: node.id,
            label: `↕ ${position.order} ${position.direction}`,
        };
        this.orderClaim = { ...slot, order: position.order, vacatedNodeId: node.id };
    }

    get equipmentId(): string {
        return this.feeder.equipmentId;
    }

    execute(): void {}

    undo(): void {}

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: 'update-position',
            equipmentId: this.equipmentId,
            payload: {
                node: this.feeder.node!,
                order: this.position.order,
                direction: this.position.direction,
            },
        };
    }
}
