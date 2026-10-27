import type { PendingCreateCommand } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import { injectionFields, switchKind, type NetworkModification } from '../modifications/modifications.ts';
import {
    NODE_COMPONENT_TYPE,
    createdNodeId,
    type CreateSpec,
    type ElementType,
    type EquipmentProperties,
    type NodeMetadata,
} from '../types.ts';

export class CreateSwitchedInjectionCommand implements PendingCreateCommand {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string; elementId: string };

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
        targetId: string,
        private readonly model: EditorModel,
    ) {
        this.node = {
            id: createdNodeId(equipmentId),
            equipmentId,
            componentType: NODE_COMPONENT_TYPE[type],
            vid: vlId,
        };
        this.pendingMarker = {
            targetId,
            nodeId: targetId,
            label: equipmentId,
            elementId: this.node.id,
        };
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
        return {
            type: this.type,
            properties: { ...this.properties },
            provisionalId: this.equipmentId,
            switchType: this.switchType,
        };
    }
}
