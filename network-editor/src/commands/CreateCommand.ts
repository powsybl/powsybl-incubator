import type { PendingCreateCommand } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import { injectionCreation, nodeInjectionCreation, type NetworkModification } from '../modifications/modifications.ts';
import {
    NODE_COMPONENT_TYPE,
    newCreatedNodeId,
    toDirection,
    type BayPosition,
    type BusbarTarget,
    type CreateSpec,
    type ElementType,
    type EquipmentProperties,
    type NodeMetadata,
    type NodeTarget,
    type OrderClaim,
} from '../types.ts';

export class CreateCommand implements PendingCreateCommand {
    private readonly node: NodeMetadata;

    constructor(
        readonly equipmentId: string,
        private readonly type: ElementType,
        private readonly target: NodeTarget | BusbarTarget,
        private readonly properties: EquipmentProperties,
        private readonly model: EditorModel,
        private readonly bay?: BayPosition,
        private readonly fields: Readonly<Record<string, unknown>> = {},
    ) {
        this.node = {
            id: newCreatedNodeId(equipmentId),
            equipmentId,
            componentType: NODE_COMPONENT_TYPE[type],
            vid: target.vlId,
            iidmNode: target.kind === 'NODE' ? target.node : undefined,
            order: bay?.order,
            direction: bay?.direction,
        };
    }

    get pendingMarker(): { targetId: string; nodeId: string; label: string; elementId: string } | undefined {
        if (!this.isInModel) return undefined;
        const { target, node } = this;
        const label = node.equipmentId ?? this.equipmentId;
        return { targetId: target.id, nodeId: target.id, label, elementId: node.id };
    }

    get orderClaims(): OrderClaim[] | undefined {
        const { target, node } = this;
        if (!this.isInModel || target.kind !== 'BUSBAR' || node.order === undefined) return undefined;
        return [{ vlId: target.vlId, sectionIndex: target.sectionIndex, order: node.order }];
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
        if (this.target.kind === 'NODE') {
            return [nodeInjectionCreation(this.type, this.equipmentId, this.target.vlId, this.target.node, this.properties, this.fields)];
        }
        const placement = {
            voltageLevelId: this.target.vlId,
            busbarSectionId: this.target.busbarSectionId,
            order: this.bay?.order ?? null,
            direction: this.bay?.direction,
        };
        return [injectionCreation(this.type, this.equipmentId, placement, this.properties, this.fields)];
    }

    get createSpec(): CreateSpec {
        const equipmentId = this.node.equipmentId ?? this.equipmentId;
        return {
            type: this.type,
            properties: this.model.getProperties(equipmentId),
            provisionalId: equipmentId,
            order: this.node.order,
            direction: toDirection(this.node.direction),
        };
    }

    private get isInModel(): boolean {
        return this.model.getNodeById(this.node.id) !== undefined;
    }
}
