import type { Command } from './Command';
import type { ChangeSetEntry, ElementType, EquipmentProperties } from '../types';


export class CreateSwitchedInjectionCommand implements Command {
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
        private readonly node: number,
        private readonly switchType: ElementType,
        readonly switchId: string,
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
            op: 'create-switched-injection',
            equipmentId: this.equipmentId,
            payload: {
                equipmentType: this.type,
                vlId: this.vlId,
                node: this.node,
                switchType: this.switchType,
                switchId: this.switchId,
                properties: this.properties,
            },
        };
    }
}
