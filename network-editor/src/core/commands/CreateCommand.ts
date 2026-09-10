import type { PendingCreateCommand } from './Command';
import type { EditorModel } from '../EditorModel';
import {
    NODE_COMPONENT_TYPE,
    createdNodeId,
    type BusbarTarget,
    type ChangeSetEntry,
    type CreateSpec,
    type ElementType,
    type EquipmentProperties,
    type FeederDirection,
    type NodeMetadata,
    type NodeTarget,
    type OrderClaim,
} from '../types';

export class CreateCommand implements PendingCreateCommand {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string; elementId: string };

    readonly orderClaim?: OrderClaim;

    private readonly node: NodeMetadata;

    constructor(
        readonly equipmentId: string,
        private readonly type: ElementType,
        private readonly target: NodeTarget | BusbarTarget,
        private readonly properties: EquipmentProperties,
        private readonly model: EditorModel,
        private readonly bay?: { order: number; direction: FeederDirection },
    ) {
        this.node = {
            id: createdNodeId(equipmentId),
            equipmentId,
            componentType: NODE_COMPONENT_TYPE[type],
            vid: target.vlId,
            iidmNode: target.kind === 'NODE' ? target.node : undefined,
            order: bay?.order,
            direction: bay?.direction,
        };
        this.pendingMarker = {
            targetId: target.id,
            nodeId: target.id,
            label: equipmentId,
            elementId: this.node.id,
        };
        if (target.kind === 'BUSBAR' && bay) {
            this.orderClaim = {
                vlId: target.vlId,
                sectionIndex: target.sectionIndex,
                order: bay.order,
            };
        }
    }

    execute(): void {
        this.model.addNode(this.node);
        this.model.replaceProperties(this.equipmentId, this.properties);
    }

    undo(): void {
        this.model.removeNode(this.node.id);
        this.model.clearProperties(this.equipmentId);
    }

    toChangeSetEntry(): ChangeSetEntry {
        const equipmentId = this.equipmentId;

        switch (this.target.kind) {
            case 'NODE':
                return {
                    op: 'create',
                    equipmentId,
                    payload: {
                        equipmentType: this.type,
                        vlId: this.target.vlId,
                        node: this.target.node,
                        properties: this.properties,
                    },
                };
            case 'BUSBAR':
                return {
                    op: 'create-bay',
                    equipmentId,
                    payload: {
                        equipmentType: this.type,
                        busbarSectionId: this.target.busbarSectionId,
                        order: this.bay!.order,
                        direction: this.bay!.direction,
                        properties: this.properties,
                    },
                };
        }
    }

    get createSpec(): CreateSpec {
        return {
            type: this.type,
            properties: { ...this.properties },
            provisionalId: this.equipmentId,
            order: this.bay?.order,
            direction: this.bay?.direction,
        };
    }

    withSpec(spec: CreateSpec): CreateCommand {
        return new CreateCommand(
            spec.provisionalId,
            this.type,
            this.target,
            spec.properties,
            this.model,
            this.bay && {
                order: spec.order ?? this.bay.order,
                direction: spec.direction ?? this.bay.direction,
            },
        );
    }
}
