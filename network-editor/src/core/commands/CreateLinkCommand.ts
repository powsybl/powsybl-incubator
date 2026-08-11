import type { Command } from './Command';
import type { ChangeSetEntry, ElementType, EquipmentProperties } from '../types';

export class CreateLinkCommand implements Command {
    readonly pendingMarker: {
        targetId: string;
        nodeId: string;
        label: string;
        consumes: boolean;
    };

    constructor(
        readonly equipmentId: string,
        private readonly type: ElementType,
        private readonly vlId: string,
        private readonly node1: number,
        private readonly node2: number,
        private readonly properties: EquipmentProperties,
        markerNodeId: string,
        targetId: string,
    ) {
        this.pendingMarker = {
            targetId,
            nodeId: markerNodeId,
            label: equipmentId,
            consumes: false,
        };
    }

    execute(): void {}

    undo(): void {}

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: 'create-switch',
            equipmentId: this.equipmentId,
            payload: {
                equipmentType: this.type,
                vlId: this.vlId,
                node1: this.node1,
                node2: this.node2,
                properties: this.properties,
            },
        };
    }
}
