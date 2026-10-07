import type { PendingCreateCommand } from './Command';
import type { EditorModel } from '../EditorModel';
import { switchKind, type NetworkModification } from '../modifications';
import {
    NODE_COMPONENT_TYPE,
    createdNodeId,
    type CreateSpec,
    type ElementType,
    type EquipmentProperties,
    type NodeMetadata,
    type SwitchEnds,
} from '../types';

export class CreateSwitchCommand implements PendingCreateCommand {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string; elementId: string };

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
        this.pendingMarker = {
            targetId: ends.first,
            nodeId: ends.first,
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
        return {
            type: this.type,
            properties: { ...this.properties },
            provisionalId: this.equipmentId,
        };
    }

    withSpec(spec: CreateSpec): CreateSwitchCommand {
        return new CreateSwitchCommand(
            spec.provisionalId,
            this.type,
            this.vlId,
            this.node1,
            this.node2,
            spec.properties,
            this.ends,
            this.model,
        );
    }
}
