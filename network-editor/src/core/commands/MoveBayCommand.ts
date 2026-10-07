import type { Command } from './Command';
import type { BayPosition, BusbarTarget, ChangeSetEntry, EquipmentTarget, OrderClaim } from '../types';

export class MoveBayCommand implements Command {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string };

    readonly orderClaims: readonly OrderClaim[];

    constructor(
        private readonly feeder: EquipmentTarget,
        private readonly destination: BusbarTarget,
        markerNodeId: string,
        private readonly position?: BayPosition,
        feederNodeId?: string,
    ) {
        this.orderClaims =
            position && feederNodeId
                ? [{
                      vlId: destination.vlId,
                      sectionIndex: destination.sectionIndex,
                      order: position.order,
                      direction: position.direction,
                      vacatedNodeId: feederNodeId,
                  }]
                : [];
        this.pendingMarker = {
            targetId: feeder.id,
            nodeId: markerNodeId,
            label: `→ ${destination.busbarSectionId}`,
        };
    }

    get equipmentId(): string {
        return this.feeder.equipmentId;
    }

    execute(): void {}

    undo(): void {}

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: 'move-bay',
            equipmentId: this.equipmentId,
            payload: {
                node: this.feeder.node!,
                targetBusbarSectionId: this.destination.busbarSectionId,
                ...(this.position && { order: this.position.order, direction: this.position.direction }),
            },
        };
    }
}
