import type { Command } from './Command.ts';
import { moveFeederBay, type NetworkModification } from '../modifications/modifications.ts';
import type { BayPosition, BusbarTarget, EquipmentTarget, OrderClaim } from '../types.ts';

export class MoveBayCommand implements Command {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string };

    readonly orderClaims: readonly OrderClaim[];

    constructor(
        private readonly feeder: EquipmentTarget,
        private readonly destination: BusbarTarget,
        markerNodeId: string,
        private readonly position: BayPosition,
        feederNodeId: string,
        private readonly connectionSide: string | null,
    ) {
        this.orderClaims = [{
            vlId: destination.vlId,
            sectionIndex: destination.sectionIndex,
            order: position.order,
            direction: position.direction,
            vacatedNodeId: feederNodeId,
        }];
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

    toModifications(): NetworkModification[] {
        return [
            moveFeederBay(
                this.destination.vlId,
                this.equipmentId,
                this.destination.busbarSectionId,
                this.position.order,
                this.position.direction,
                this.connectionSide,
            ),
        ];
    }
}
