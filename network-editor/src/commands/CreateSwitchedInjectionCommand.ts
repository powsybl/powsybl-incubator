import type { PendingCreateCommand } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import { injectionFields, switchKind, type NetworkModification } from '../modifications/modifications.ts';
import {
    NODE_COMPONENT_TYPE,
    newCreatedNodeId,
    type CreateSpec,
    type ElementType,
    type EquipmentProperties,
    type NodeMetadata,
} from '../types.ts';

export class CreateSwitchedInjectionCommand implements PendingCreateCommand {
    /**
     * Only the injection enters the model: the switch it hangs behind gets its IIDM nodes from
     * the backend, and a node with no SVG element of its own would be unreachable.
     */
    private readonly node: NodeMetadata;

    constructor(
        readonly equipmentId: string,
        private readonly type: ElementType,
        private readonly vlId: string,
        private readonly iidmNode: number,
        private readonly switchType: ElementType,
        readonly switchId: string,
        private readonly properties: EquipmentProperties,
        private readonly targetId: string,
        private readonly model: EditorModel,
    ) {
        this.node = {
            id: newCreatedNodeId(equipmentId),
            equipmentId,
            componentType: NODE_COMPONENT_TYPE[type],
            vid: vlId,
        };
    }

    get pendingMarker(): { targetId: string; nodeId: string; label: string; elementId: string } | undefined {
        if (!this.isInModel) return undefined;
        const { targetId, node } = this;
        const label = node.equipmentId ?? this.equipmentId;
        return { targetId, nodeId: targetId, label, elementId: node.id };
    }

    execute(): void {
        this.model.addNode(this.node);
        this.model.replaceProperties(this.equipmentId, this.properties);
        this.model.reserveId(this.switchId);
    }

    undo(): void {
        this.model.releaseId(this.switchId);
        this.model.removeNode(this.node.id);
        this.model.clearProperties(this.equipmentId);
    }

    toModifications(): NetworkModification[] {
        return [
            {
                type: 'SWITCHED_INJECTION_CREATION',
                voltageLevelId: this.vlId,
                node: this.iidmNode,
                switchId: this.switchId,
                switchKind: switchKind(this.switchType),
                creation: injectionFields(this.type, this.equipmentId, this.vlId, this.properties),
            },
        ];
    }

    get createSpec(): CreateSpec {
        const equipmentId = this.node.equipmentId ?? this.equipmentId;
        return {
            type: this.type,
            properties: this.model.getProperties(equipmentId),
            provisionalId: equipmentId,
            switchType: this.switchType,
        };
    }

    private get isInModel(): boolean {
        return this.model.getNodeById(this.node.id) !== undefined;
    }
}
