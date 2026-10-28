import type { PendingCreateCommand } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import { switchKind, type NetworkModification } from '../modifications/modifications.ts';
import {
    NODE_COMPONENT_TYPE,
    createdNodeId,
    type CreateSpec,
    type ElementType,
    type EquipmentProperties,
    type NodeMetadata,
    type SwitchEnds,
} from '../types.ts';

export class CreateSwitchCommand implements PendingCreateCommand {
    private readonly node: NodeMetadata;

    constructor(
        readonly equipmentId: string,
        private readonly type: ElementType,
        private readonly vlId: string,
        private readonly node1: number,
        private readonly node2: number,
        private readonly properties: EquipmentProperties,
        readonly ends: SwitchEnds,
        private readonly model: EditorModel,
    ) {
        this.node = {
            id: createdNodeId(equipmentId),
            equipmentId,
            componentType: NODE_COMPONENT_TYPE[type],
            vid: vlId,
            iidmNode1: node1,
            iidmNode2: node2,
            open: properties.open === true,
        };
    }

    get pendingMarker(): { targetId: string; nodeId: string; label: string; elementId: string } | undefined {
        if (!this.isInModel) return undefined;
        const { ends, node } = this;
        const label = node.equipmentId ?? this.equipmentId;
        return { targetId: ends.first, nodeId: ends.first, label, elementId: node.id };
    }

    execute(): void {
        this.model.addNode(this.node);
        this.model.replaceProperties(this.equipmentId, this.properties);
    }

    undo(): void {
        this.model.removeNode(this.node.id);
        this.model.clearProperties(this.equipmentId);
    }

    toModifications(): NetworkModification[] {
        return [
            {
                type: 'SWITCH_CREATION',
                equipmentId: this.equipmentId,
                voltageLevelId: this.vlId,
                switchKind: switchKind(this.type),
                node1: this.node1,
                node2: this.node2,
                open: this.properties.open === true,
            },
        ];
    }

    get createSpec(): CreateSpec {
        const equipmentId = this.node.equipmentId ?? this.equipmentId;
        return {
            type: this.type,
            properties: this.model.getProperties(equipmentId),
            provisionalId: equipmentId,
        };
    }

    private get isInModel(): boolean {
        return this.model.getNodeById(this.node.id) !== undefined;
    }
}
