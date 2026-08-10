import type { Command } from './Command';
import type { BusbarTarget, ChangeSetEntry, EquipmentTarget } from '../types';

export class MoveBayCommand implements Command {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string };

    constructor(
        private readonly feeder: EquipmentTarget,
        private readonly destination: BusbarTarget,
        markerNodeId: string,
    ) {
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
            },
        };
    }
}
